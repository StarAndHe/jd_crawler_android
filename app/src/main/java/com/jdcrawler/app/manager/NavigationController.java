package com.jdcrawler.app.manager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 导航控制器
 * 负责页面导航、安全返回和手势操作的管理
 */
public class NavigationController {
    
    private static final String TAG = "NavigationController";
    
    private final AccessibilityService accessibilityService;
    private final Handler mainHandler;
    
    // 导航状态
    private AtomicBoolean isNavigating = new AtomicBoolean(false);
    private List<NavigationStep> navigationHistory;
    private int maxHistorySize = 20;
    
    // 手势配置
    private static final long CLICK_DURATION = 100;
    private static final long SWIPE_DURATION = 300;
    private static final long NAVIGATION_DELAY = 1500; // 导航操作间延时
    
    /**
     * 导航步骤记录
     */
    private static class NavigationStep {
        public enum ActionType {
            CLICK, BACK, SCROLL, ENTER_PAGE, EXIT_PAGE
        }
        
        ActionType actionType;
        int x, y; // 点击坐标
        String pageType; // 页面类型
        long timestamp;
        String description;
        
        NavigationStep(ActionType actionType, String description) {
            this.actionType = actionType;
            this.description = description;
            this.timestamp = System.currentTimeMillis();
        }
        
        NavigationStep(ActionType actionType, int x, int y, String description) {
            this(actionType, description);
            this.x = x;
            this.y = y;
        }
    }
    
    public NavigationController(AccessibilityService service) {
        this.accessibilityService = service;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.navigationHistory = new ArrayList<>();
    }
    
    /**
     * 安全点击指定坐标
     */
    public boolean performClick(int x, int y) {
        return performClick(x, y, "点击坐标(" + x + "," + y + ")");
    }
    
    /**
     * 安全点击指定坐标（带描述）
     */
    public boolean performClick(int x, int y, String description) {
        if (isNavigating.get()) {
            Log.w(TAG, "导航操作进行中，跳过点击操作");
            return false;
        }
        
        try {
            isNavigating.set(true);
            
            Log.d(TAG, "执行点击操作: " + description + " 坐标(" + x + "," + y + ")");
            
            // 创建点击手势
            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            
            GestureDescription.StrokeDescription clickStroke = 
                new GestureDescription.StrokeDescription(clickPath, 0, CLICK_DURATION);
            
            GestureDescription clickGesture = new GestureDescription.Builder()
                .addStroke(clickStroke)
                .build();
            
            // 记录导航步骤
            recordNavigationStep(new NavigationStep(NavigationStep.ActionType.CLICK, x, y, description));
            
            // 执行手势
            boolean result = accessibilityService.dispatchGesture(clickGesture, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d(TAG, "点击手势执行完成");
                    
                    // 延时后重置导航状态
                    mainHandler.postDelayed(() -> {
                        isNavigating.set(false);
                    }, NAVIGATION_DELAY);
                }
                
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    Log.w(TAG, "点击手势被取消");
                    isNavigating.set(false);
                }
            }, null);
            
            if (!result) {
                isNavigating.set(false);
                Log.e(TAG, "点击手势分发失败");
            }
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "执行点击操作时发生错误", e);
            isNavigating.set(false);
            return false;
        }
    }
    
    /**
     * 点击节点
     */
    public boolean clickNode(AccessibilityNodeInfo node) {
        return clickNode(node, "点击节点");
    }
    
    /**
     * 点击节点（带描述）
     */
    public boolean clickNode(AccessibilityNodeInfo node, String description) {
        if (node == null) {
            Log.w(TAG, "节点为空，无法点击");
            return false;
        }
        
        try {
            // 首先尝试使用节点的performAction
            if (node.isClickable()) {
                Log.d(TAG, "使用节点performAction点击: " + description);
                boolean result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (result) {
                    recordNavigationStep(new NavigationStep(NavigationStep.ActionType.CLICK, description));
                    return true;
                }
            }
            
            // 如果节点点击失败，尝试手势点击
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            
            if (bounds.width() > 0 && bounds.height() > 0) {
                int clickX = bounds.centerX();
                int clickY = bounds.centerY();
                
                Log.d(TAG, "使用手势点击节点: " + description);
                return performClick(clickX, clickY, description + " (手势)");
            } else {
                Log.w(TAG, "节点边界无效，无法点击");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "点击节点时发生错误", e);
            return false;
        }
    }
    
    /**
     * 执行返回操作
     */
    public boolean performBack() {
        return performBack("系统返回");
    }
    
    /**
     * 执行返回操作（带描述）
     */
    public boolean performBack(String description) {
        if (isNavigating.get()) {
            Log.w(TAG, "导航操作进行中，跳过返回操作");
            return false;
        }
        
        try {
            isNavigating.set(true);
            
            Log.d(TAG, "执行返回操作: " + description);
            
            // 记录导航步骤
            recordNavigationStep(new NavigationStep(NavigationStep.ActionType.BACK, description));
            
            // 执行系统返回
            boolean result = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            
            // 延时后重置导航状态
            mainHandler.postDelayed(() -> {
                isNavigating.set(false);
            }, NAVIGATION_DELAY);
            
            if (result) {
                Log.d(TAG, "返回操作执行成功");
            } else {
                Log.w(TAG, "返回操作执行失败");
            }
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "执行返回操作时发生错误", e);
            isNavigating.set(false);
            return false;
        }
    }
    
    /**
     * 执行滚动操作
     */
    public boolean performScroll(AccessibilityNodeInfo scrollableNode, boolean scrollDown) {
        return performScroll(scrollableNode, scrollDown, "滚动页面");
    }
    
    /**
     * 执行滚动操作（带描述）
     */
    public boolean performScroll(AccessibilityNodeInfo scrollableNode, boolean scrollDown, String description) {
        if (scrollableNode == null) {
            Log.w(TAG, "滚动节点为空");
            return false;
        }
        
        try {
            Log.d(TAG, "执行滚动操作: " + description + " 方向: " + (scrollDown ? "向下" : "向上"));
            
            // 记录导航步骤
            recordNavigationStep(new NavigationStep(NavigationStep.ActionType.SCROLL, description));
            
            // 使用节点的滚动动作
            int action = scrollDown ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
            boolean result = scrollableNode.performAction(action);
            
            if (!result) {
                // 如果节点滚动失败，尝试手势滚动
                result = performSwipeGesture(scrollableNode, scrollDown);
            }
            
            if (result) {
                Log.d(TAG, "滚动操作执行成功");
            } else {
                Log.w(TAG, "滚动操作执行失败");
            }
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "执行滚动操作时发生错误", e);
            return false;
        }
    }
    
    /**
     * 执行滑动手势
     */
    private boolean performSwipeGesture(AccessibilityNodeInfo node, boolean scrollDown) {
        try {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            
            int startX = bounds.centerX();
            int startY = scrollDown ? bounds.bottom - 100 : bounds.top + 100;
            int endY = scrollDown ? bounds.top + 100 : bounds.bottom - 100;
            
            return performSwipe(startX, startY, startX, endY);
            
        } catch (Exception e) {
            Log.e(TAG, "执行滑动手势时发生错误", e);
            return false;
        }
    }
    
    /**
     * 执行滑动手势
     */
    public boolean performSwipe(int startX, int startY, int endX, int endY) {
        try {
            Log.d(TAG, "执行滑动手势: (" + startX + "," + startY + ") -> (" + endX + "," + endY + ")");
            
            Path swipePath = new Path();
            swipePath.moveTo(startX, startY);
            swipePath.lineTo(endX, endY);
            
            GestureDescription.StrokeDescription swipeStroke = 
                new GestureDescription.StrokeDescription(swipePath, 0, SWIPE_DURATION);
            
            GestureDescription swipeGesture = new GestureDescription.Builder()
                .addStroke(swipeStroke)
                .build();
            
            return accessibilityService.dispatchGesture(swipeGesture, null, null);
            
        } catch (Exception e) {
            Log.e(TAG, "执行滑动手势时发生错误", e);
            return false;
        }
    }
    
    /**
     * 等待页面稳定
     */
    public void waitForPageStable() {
        waitForPageStable(NAVIGATION_DELAY);
    }
    
    /**
     * 等待页面稳定（指定时间）
     */
    public void waitForPageStable(long delayMs) {
        try {
            Log.d(TAG, "等待页面稳定: " + delayMs + "ms");
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Log.w(TAG, "等待被中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 智能返回到指定页面类型
     */
    public boolean navigateBackToPageType(String targetPageType) {
        return navigateBackToPageType(targetPageType, 5); // 最多尝试5次返回
    }
    
    /**
     * 智能返回到指定页面类型（指定最大尝试次数）
     */
    public boolean navigateBackToPageType(String targetPageType, int maxAttempts) {
        try {
            Log.d(TAG, "尝试返回到页面类型: " + targetPageType + " 最大尝试次数: " + maxAttempts);
            
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                // 检查当前页面类型
                AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
                if (rootNode != null) {
                    PageStateManager pageStateManager = new PageStateManager();
                    PageStateManager.PageType currentType = pageStateManager.detectPageType(rootNode);
                    rootNode.recycle();
                    
                    String currentPageType = currentType.name();
                    Log.d(TAG, "当前页面类型: " + currentPageType + " 目标类型: " + targetPageType);
                    
                    if (targetPageType.equals(currentPageType)) {
                        Log.d(TAG, "已到达目标页面类型，返回成功");
                        return true;
                    }
                }
                
                // 执行返回操作
                if (!performBack("导航返回第" + (attempt + 1) + "次")) {
                    Log.w(TAG, "返回操作失败，停止尝试");
                    break;
                }
                
                // 等待页面稳定
                waitForPageStable();
            }
            
            Log.w(TAG, "达到最大尝试次数，未能返回到目标页面类型");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "智能返回时发生错误", e);
            return false;
        }
    }
    
    /**
     * 记录导航步骤
     */
    private void recordNavigationStep(NavigationStep step) {
        try {
            navigationHistory.add(step);
            
            // 限制历史记录大小
            if (navigationHistory.size() > maxHistorySize) {
                navigationHistory.remove(0);
            }
            
            Log.d(TAG, "记录导航步骤: " + step.actionType + " - " + step.description);
            
        } catch (Exception e) {
            Log.e(TAG, "记录导航步骤时发生错误", e);
        }
    }
    
    /**
     * 获取导航历史
     */
    public List<String> getNavigationHistory() {
        List<String> history = new ArrayList<>();
        try {
            for (NavigationStep step : navigationHistory) {
                String historyItem = String.format("[%s] %s - %s", 
                    new java.util.Date(step.timestamp).toString(),
                    step.actionType.name(),
                    step.description
                );
                history.add(historyItem);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取导航历史时发生错误", e);
        }
        return history;
    }
    
    /**
     * 清除导航历史
     */
    public void clearNavigationHistory() {
        try {
            navigationHistory.clear();
            Log.d(TAG, "导航历史已清除");
        } catch (Exception e) {
            Log.e(TAG, "清除导航历史时发生错误", e);
        }
    }
    
    /**
     * 检查是否正在导航
     */
    public boolean isNavigating() {
        return isNavigating.get();
    }
    
    /**
     * 强制重置导航状态
     */
    public void resetNavigationState() {
        isNavigating.set(false);
        Log.d(TAG, "导航状态已重置");
    }
    
    /**
     * 获取最近的导航步骤
     */
    public String getLastNavigationStep() {
        try {
            if (!navigationHistory.isEmpty()) {
                NavigationStep lastStep = navigationHistory.get(navigationHistory.size() - 1);
                return lastStep.actionType + " - " + lastStep.description;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取最近导航步骤时发生错误", e);
        }
        return "无导航历史";
    }
    
    /**
     * 手势结果回调
     */
    private static abstract class GestureResultCallback extends AccessibilityService.GestureResultCallback {
        // 可以在子类中重写需要的方法
    }
}