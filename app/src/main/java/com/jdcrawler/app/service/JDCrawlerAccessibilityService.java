package com.jdcrawler.app.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import com.jdcrawler.app.model.ProductInfo;
import com.jdcrawler.app.manager.PageStateManager;
import com.jdcrawler.app.manager.NavigationController;
import com.jdcrawler.app.manager.DataCollector;
import com.jdcrawler.app.util.NodeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 京东商品爬虫无障碍服务核心类
 * 负责页面监听、状态管理和数据提取的核心协调
 */
public class JDCrawlerAccessibilityService extends AccessibilityService {
    
    private static final String TAG = "JDCrawlerService";
    private static final String JD_PACKAGE_NAME = "com.jingdong.app.mall";
    
    // 通知栏相关
    private static final String NOTIFICATION_CHANNEL_ID = "jd_crawler_channel";
    private static final int NOTIFICATION_ID = 1001;
    private NotificationManager notificationManager;
    
    // 核心管理器
    private PageStateManager pageStateManager;
    private NavigationController navigationController;
    private DataCollector dataCollector;
    
    // 服务状态
    private AtomicBoolean isServiceActive = new AtomicBoolean(false);
    private AtomicBoolean isCrawling = new AtomicBoolean(false);
    private Handler mainHandler;
    
    // 广播接收器
    private ServiceBroadcastReceiver broadcastReceiver;
    
    // 爬取进度
    private int totalProductsFound = 0;
    private int currentProductIndex = 0;
    private List<ProductInfo> collectedProducts = new ArrayList<>();
    
    // 延时配置
    private static final long PAGE_LOAD_DELAY = 2000; // 页面加载等待时间
    private static final long CLICK_DELAY = 1500; // 点击操作延时
    private static final long SCROLL_DELAY = 1000; // 滚动延时
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "JDCrawlerAccessibilityService onCreate");
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化通知栏
        initNotification();
        
        // 初始化核心管理器
        pageStateManager = new PageStateManager();
        navigationController = new NavigationController(this);
        dataCollector = new DataCollector();
        
        // 注册广播接收器
        registerBroadcastReceiver();
        
        isServiceActive.set(true);
    }
    
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "无障碍服务已连接");
        showToast("京东爬虫服务已启动");
        
        // 发送服务状态广播
        sendServiceStatusBroadcast(true);
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isServiceActive.get()) return;
        
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        
        // 记录所有应用的事件（调试用）
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d(TAG, "收到事件: " + AccessibilityEvent.eventTypeToString(eventType) + " 来自: " + packageName);
        }
        
        // 只处理京东APP的事件
        if (!JD_PACKAGE_NAME.equals(packageName)) {
            return;
        }
        
        Log.d(TAG, "✓ 处理京东APP事件: " + AccessibilityEvent.eventTypeToString(eventType));
        
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handlePageContentChanged();
                break;
                
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                if (isCrawling.get()) {
                    handleScrollEvent();
                }
                break;
        }
    }
    
    /**
     * 处理页面内容变化事件
     */
    private void handlePageContentChanged() {
        if (!isCrawling.get()) return;
        
        mainHandler.postDelayed(() -> {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) return;
            
            try {
                // 更新页面状态
                PageStateManager.PageType currentPageType = pageStateManager.detectPageType(rootNode);
                Log.d(TAG, "当前页面类型: " + currentPageType);
                
                // 根据页面类型执行相应的爬取逻辑
                switch (currentPageType) {
                    case SHOP_HOME:
                    case PRODUCT_LIST:
                        handleProductListPage(rootNode);
                        break;
                        
                    case PRODUCT_DETAIL:
                        handleProductDetailPage(rootNode);
                        break;
                        
                    case UNKNOWN:
                        Log.w(TAG, "未识别的页面类型，等待页面稳定");
                        break;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "处理页面变化时发生错误", e);
            } finally {
                rootNode.recycle();
            }
        }, PAGE_LOAD_DELAY);
    }
    
    /**
     * 处理商品列表页面
     */
    private void handleProductListPage(AccessibilityNodeInfo rootNode) {
        Log.d(TAG, "处理商品列表页面");
        
        // 查找所有商品节点
        List<AccessibilityNodeInfo> productNodes = NodeUtils.findProductNodes(rootNode);
        
        if (productNodes.isEmpty()) {
            Log.w(TAG, "未找到商品节点，尝试滚动加载更多");
            scrollToLoadMore();
            return;
        }
        
        Log.d(TAG, "找到 " + productNodes.size() + " 个商品");
        
        // 如果是第一次发现商品，记录总数
        if (totalProductsFound == 0) {
            totalProductsFound = productNodes.size();
            sendProgressUpdate(0, totalProductsFound);
        }
        
        // 开始逐个处理商品
        processNextProduct(productNodes);
    }
    
    /**
     * 逐个处理商品
     */
    private void processNextProduct(List<AccessibilityNodeInfo> productNodes) {
        if (currentProductIndex >= productNodes.size()) {
            // 所有商品处理完毕，尝试翻页或结束
            handlePageComplete();
            return;
        }
        
        AccessibilityNodeInfo productNode = productNodes.get(currentProductIndex);
        Log.d(TAG, "处理第 " + (currentProductIndex + 1) + " 个商品");
        
        // 点击进入商品详情页
        if (clickProductNode(productNode)) {
            // 等待详情页加载
            mainHandler.postDelayed(() -> {
                // 检查是否成功进入详情页
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    PageStateManager.PageType pageType = pageStateManager.detectPageType(rootNode);
                    if (pageType == PageStateManager.PageType.PRODUCT_DETAIL) {
                        Log.d(TAG, "成功进入商品详情页");
                        // handleProductDetailPage 会在下次事件中被调用
                    } else {
                        Log.w(TAG, "未能进入详情页，跳过当前商品");
                        currentProductIndex++;
                        processNextProduct(productNodes);
                    }
                    rootNode.recycle();
                }
            }, PAGE_LOAD_DELAY);
        } else {
            Log.w(TAG, "点击商品失败，跳过");
            currentProductIndex++;
            processNextProduct(productNodes);
        }
    }
    
    /**
     * 处理商品详情页面
     */
    private void handleProductDetailPage(AccessibilityNodeInfo rootNode) {
        Log.d(TAG, "处理商品详情页面");
        
        try {
            // 提取商品详细信息
            ProductInfo productInfo = dataCollector.extractProductDetail(rootNode);
            
            if (productInfo != null) {
                collectedProducts.add(productInfo);
                Log.d(TAG, "成功提取商品信息: " + productInfo.getTitle());
                
                // 更新进度
                sendProgressUpdate(collectedProducts.size(), totalProductsFound);
            } else {
                Log.w(TAG, "未能提取商品信息");
            }
            
            // 返回列表页继续下一个商品
            currentProductIndex++;
            navigateBack();
            
        } catch (Exception e) {
            Log.e(TAG, "提取商品详情时发生错误", e);
            currentProductIndex++;
            navigateBack();
        }
    }
    
    /**
     * 点击商品节点
     */
    private boolean clickProductNode(AccessibilityNodeInfo productNode) {
        try {
            Rect bounds = new Rect();
            productNode.getBoundsInScreen(bounds);
            
            // 计算点击位置（商品卡片中心）
            int clickX = bounds.centerX();
            int clickY = bounds.centerY();
            
            return performClick(clickX, clickY);
            
        } catch (Exception e) {
            Log.e(TAG, "点击商品节点失败", e);
            return false;
        }
    }
    
    /**
     * 执行点击操作
     */
    private boolean performClick(int x, int y) {
        try {
            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            
            GestureDescription.StrokeDescription clickStroke = 
                new GestureDescription.StrokeDescription(clickPath, 0, 100);
            
            GestureDescription clickGesture = new GestureDescription.Builder()
                .addStroke(clickStroke)
                .build();
            
            return dispatchGesture(clickGesture, null, null);
            
        } catch (Exception e) {
            Log.e(TAG, "执行点击操作失败", e);
            return false;
        }
    }
    
    /**
     * 导航返回
     */
    private void navigateBack() {
        mainHandler.postDelayed(() -> {
            performGlobalAction(GLOBAL_ACTION_BACK);
            Log.d(TAG, "执行返回操作");
        }, CLICK_DELAY);
    }
    
    /**
     * 滚动加载更多商品
     */
    private void scrollToLoadMore() {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) return;
            
            // 查找可滚动的节点
            AccessibilityNodeInfo scrollableNode = NodeUtils.findScrollableNode(rootNode);
            if (scrollableNode != null) {
                scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                Log.d(TAG, "执行滚动操作");
            }
            
            rootNode.recycle();
            if (scrollableNode != null) {
                scrollableNode.recycle();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "滚动操作失败", e);
        }
    }
    
    /**
     * 处理页面完成
     */
    private void handlePageComplete() {
        Log.d(TAG, "当前页面商品处理完毕");
        
        // 尝试翻页或结束爬取
        if (!tryLoadNextPage()) {
            // 没有更多页面，结束爬取
            finishCrawling();
        }
    }
    
    /**
     * 尝试加载下一页
     */
    private boolean tryLoadNextPage() {
        // TODO: 实现翻页逻辑
        Log.d(TAG, "尝试加载下一页（待实现）");
        return false;
    }
    
    /**
     * 完成爬取
     */
    private void finishCrawling() {
        Log.d(TAG, "爬取完成，共收集 " + collectedProducts.size() + " 个商品");
        
        isCrawling.set(false);
        
        // 导出Excel
        dataCollector.exportToExcel(collectedProducts, this);
        
        showToast("爬取完成！共收集 " + collectedProducts.size() + " 个商品");
        sendCrawlingCompleteBroadcast();
    }
    
    /**
     * 处理滚动事件
     */
    private void handleScrollEvent() {
        // 滚动时等待内容稳定
        mainHandler.removeCallbacksAndMessages("scroll_stable");
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "滚动稳定，检查新内容");
        }, SCROLL_DELAY);
    }
    
    // ========== 公共控制方法 ==========
    
    /**
     * 开始爬取
     */
    public void startCrawling() {
        if (!isServiceActive.get()) {
            showToast("服务未激活");
            return;
        }
        
        if (isCrawling.get()) {
            showToast("正在爬取中");
            return;
        }
        
        // 重置状态
        currentProductIndex = 0;
        totalProductsFound = 0;
        collectedProducts.clear();
        
        isCrawling.set(true);
        Log.d(TAG, "开始爬取");
        showToast("开始爬取商品信息");
        
        // 更新通知栏
        updateNotification("正在爬取", 0, 0);
        
        sendCrawlingStartBroadcast();
    }
    
    /**
     * 停止爬取
     */
    public void stopCrawling() {
        if (isCrawling.get()) {
            isCrawling.set(false);
            Log.d(TAG, "停止爬取");
            showToast("已停止爬取");
            
            // 更新通知栏
            updateNotification("已停止", collectedProducts.size(), totalProductsFound);
            
            sendCrawlingStopBroadcast();
        }
    }
    
    // ========== 广播和通知方法 ==========
    
    private void sendServiceStatusBroadcast(boolean connected) {
        Intent intent = new Intent("com.jdcrawler.SERVICE_STATUS");
        intent.putExtra("connected", connected);
        sendBroadcast(intent);
    }
    
    private void sendProgressUpdate(int current, int total) {
        Intent intent = new Intent("com.jdcrawler.PROGRESS_UPDATE");
        intent.putExtra("current", current);
        intent.putExtra("total", total);
        sendBroadcast(intent);
        
        // 同时更新通知栏
        updateNotification("正在爬取", current, total);
    }
    
    private void sendCrawlingStartBroadcast() {
        Intent intent = new Intent("com.jdcrawler.CRAWLING_START");
        sendBroadcast(intent);
    }
    
    private void sendCrawlingStopBroadcast() {
        Intent intent = new Intent("com.jdcrawler.CRAWLING_STOP");
        sendBroadcast(intent);
    }
    
    private void sendCrawlingCompleteBroadcast() {
        Intent intent = new Intent("com.jdcrawler.CRAWLING_COMPLETE");
        intent.putExtra("productCount", collectedProducts.size());
        sendBroadcast(intent);
    }
    
    private void showToast(String message) {
        mainHandler.post(() -> 
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }
    
    // ========== 通知栏管理方法 ==========
    
    /**
     * 初始化通知栏
     */
    private void initNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        // Android 8.0+ 需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "京东爬虫服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("显示爬虫运行状态和控制按钮");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 更新通知栏状态
     */
    private void updateNotification(String status, int current, int total) {
        try {
            // 创建停止爬取的Intent
            Intent stopIntent = new Intent("com.jdcrawler.STOP_CRAWLING");
            stopIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            stopIntent.setPackage(getPackageName());
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                this, 0, stopIntent, 
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
            );
            
            // 创建打开主界面的Intent
            Intent openIntent = new Intent(this, com.jdcrawler.app.MainActivity.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openPendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
            );
            
            // 构建通知内容
            String contentText = isCrawling.get() ? 
                String.format("正在爬取: %d/%d", current, total) : 
                "爬虫服务运行中，点击查看详情";
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("京东商品爬虫")
                .setContentText(contentText)
                .setContentIntent(openPendingIntent)
                .setOngoing(isCrawling.get())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false);
            
            // 添加停止按钮（仅在爬取时显示）
            if (isCrawling.get()) {
                builder.addAction(
                    android.R.drawable.ic_media_pause,
                    "停止爬取",
                    stopPendingIntent
                );
                
                // 添加进度条
                if (total > 0) {
                    builder.setProgress(total, current, false);
                }
            }
            
            Notification notification = builder.build();
            notificationManager.notify(NOTIFICATION_ID, notification);
            
        } catch (Exception e) {
            Log.e(TAG, "更新通知栏失败", e);
        }
    }
    
    /**
     * 清除通知栏
     */
    private void clearNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "服务被中断");
        isServiceActive.set(false);
        isCrawling.set(false);
        clearNotification();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "服务销毁");
        isServiceActive.set(false);
        isCrawling.set(false);
        
        // 取消注册广播接收器
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        
        clearNotification();
        sendServiceStatusBroadcast(false);
    }
    
    // ========== 广播接收器管理 ==========
    
    /**
     * 注册广播接收器
     */
    private void registerBroadcastReceiver() {
        try {
            broadcastReceiver = new ServiceBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.jdcrawler.START_CRAWLING");
            filter.addAction("com.jdcrawler.STOP_CRAWLING");
            filter.addAction("com.jdcrawler.EXPORT_DATA");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(broadcastReceiver, filter);
            }
            
            Log.d(TAG, "✓ 广播接收器注册成功");
        } catch (Exception e) {
            Log.e(TAG, "× 广播接收器注册失败", e);
        }
    }
    
    /**
     * 服务广播接收器
     */
    private class ServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            
            Log.d(TAG, "✓ 收到广播: " + action);
            
            switch (action) {
                case "com.jdcrawler.START_CRAWLING":
                    Log.d(TAG, "收到开始爬取指令");
                    startCrawling();
                    break;
                    
                case "com.jdcrawler.STOP_CRAWLING":
                    Log.d(TAG, "收到停止爬取指令");
                    stopCrawling();
                    break;
                    
                case "com.jdcrawler.EXPORT_DATA":
                    Log.d(TAG, "收到导出数据指令");
                    if (!collectedProducts.isEmpty()) {
                        dataCollector.exportToExcel(collectedProducts, context);
                    }
                    break;
                    
                default:
                    Log.w(TAG, "未知的广播动作: " + action);
                    break;
            }
        }
    }
}
