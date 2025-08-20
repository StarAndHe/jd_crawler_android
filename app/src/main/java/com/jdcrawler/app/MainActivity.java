package com.jdcrawler.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;

/**
 * 主界面Activity
 * 提供用户控制爬虫和查看进度的界面
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_OVERLAY_PERMISSION = 1002;
    
    // UI组件
    private TextView tvServiceStatus;
    private TextView tvCrawlingStatus;
    private TextView tvProductCount;
    private TextView tvCurrentProgress;
    private ProgressBar progressBar;
    private Button btnStartCrawling;
    private Button btnStopCrawling;
    private Button btnOpenSettings;
    private Button btnExportData;
    private Button btnShareData;
    private FloatingActionButton fabHelp;
    private MaterialCardView cardStatus;
    private MaterialCardView cardProgress;
    private MaterialCardView cardActions;
    
    // 状态变量
    private boolean isServiceConnected = false;
    private boolean isCrawling = false;
    private int currentProductCount = 0;
    private int totalProductCount = 0;
    private String lastExportedFile = "";
    
    // 广播接收器
    private CrawlerBroadcastReceiver broadcastReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
        registerBroadcastReceiver();
        checkPermissions();
        updateUI();
        
        // 显示使用提示
        showUsageTip();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        tvServiceStatus = findViewById(R.id.tv_service_status);
        tvCrawlingStatus = findViewById(R.id.tv_crawling_status);
        tvProductCount = findViewById(R.id.tv_product_count);
        tvCurrentProgress = findViewById(R.id.tv_current_progress);
        progressBar = findViewById(R.id.progress_bar);
        btnStartCrawling = findViewById(R.id.btn_start_crawling);
        btnStopCrawling = findViewById(R.id.btn_stop_crawling);
        btnOpenSettings = findViewById(R.id.btn_open_settings);
        btnExportData = findViewById(R.id.btn_export_data);
        btnShareData = findViewById(R.id.btn_share_data);
        fabHelp = findViewById(R.id.fab_help);
        cardStatus = findViewById(R.id.card_status);
        cardProgress = findViewById(R.id.card_progress);
        cardActions = findViewById(R.id.card_actions);
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        btnStartCrawling.setOnClickListener(v -> startCrawling());
        btnStopCrawling.setOnClickListener(v -> stopCrawling());
        btnOpenSettings.setOnClickListener(v -> openAccessibilitySettings());
        btnExportData.setOnClickListener(v -> exportData());
        btnShareData.setOnClickListener(v -> shareData());
        fabHelp.setOnClickListener(v -> showHelpDialog());
    }
    
    /**
     * 注册广播接收器
     */
    private void registerBroadcastReceiver() {
        broadcastReceiver = new CrawlerBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.jdcrawler.SERVICE_STATUS");
        filter.addAction("com.jdcrawler.PROGRESS_UPDATE");
        filter.addAction("com.jdcrawler.CRAWLING_START");
        filter.addAction("com.jdcrawler.CRAWLING_STOP");
        filter.addAction("com.jdcrawler.CRAWLING_COMPLETE");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, filter);
        }
    }
    
    /**
     * 检查权限
     */
    private void checkPermissions() {
        // 检查存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission();
            }
        }
        
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        }
    }
    
    /**
     * 请求存储权限
     */
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                .setTitle("需要存储权限")
                .setMessage("应用需要存储权限来保存Excel文件")
                .setPositiveButton("授权", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                        REQUEST_STORAGE_PERMISSION);
                })
                .setNegativeButton("拒绝", null)
                .show();
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                REQUEST_STORAGE_PERMISSION);
        }
    }
    
    /**
     * 请求悬浮窗权限
     */
    private void requestOverlayPermission() {
        new AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("开启悬浮窗权限可在爬取过程中显示进度")
            .setPositiveButton("去设置", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            })
            .setNegativeButton("跳过", null)
            .show();
    }
    
    /**
     * 开始爬取
     */
    private void startCrawling() {
        if (!isAccessibilityServiceEnabled()) {
            showToast("请先开启无障碍服务权限");
            openAccessibilitySettings();
            return;
        }
        
        // 简化检测：只做一次安装检测，用户可以选择跳过
        if (!isJDAppInstalled()) {
            new AlertDialog.Builder(this)
                .setTitle("京东APP检测")
                .setMessage("未检测到京东APP，请选择：\n\n" +
                           "• 确保已安装京东APP后重新检测\n" +
                           "• 跳过检测，直接开始（推荐）\n\n" +
                           "提示：爬虫启动后请立即切换到京东APP浏览商品")
                .setPositiveButton("跳过检测，开始爬取", (dialog, which) -> {
                    startCrawlingWithInstructions();
                })
                .setNegativeButton("重新检测", (dialog, which) -> {
                    startCrawling();
                })
                .setNeutralButton("取消", null)
                .show();
            return;
        }
        
        // 检测到京东APP，直接开始
        startCrawlingWithInstructions();
    }
    
    /**
     * 开始爬取并显示使用说明
     */
    private void startCrawlingWithInstructions() {
        // 显示详细的使用说明
        new AlertDialog.Builder(this)
            .setTitle("爬虫已启动")
            .setMessage("请按以下步骤操作：\n\n" +
                       "1. 点击'确定'后，本APP会最小化到后台\n" +
                       "2. 立即打开京东APP\n" +
                       "3. 进入任意店铺的商品列表页面\n" +
                       "4. 爬虫会自动开始工作\n\n" +
                       "注意：爬虫在后台运行，您可以从通知栏查看进度")
            .setPositiveButton("确定，开始爬取", (dialog, which) -> {
                // 启动爬取服务 - 使用显式Intent和系统级广播
                Intent intent = new Intent("com.jdcrawler.START_CRAWLING");
                // 添加系统级广播标志
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                intent.setPackage(getPackageName());
                
                sendBroadcast(intent);
                
                Log.d(TAG, "✓ 已发送开始爬取广播: com.jdcrawler.START_CRAWLING (系统级)");
                
                // 最小化应用
                moveTaskToBack(true);
                
                showToast("爬虫已启动，请打开京东APP");
            })
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show();
    }
    
    /**
     * 停止爬取
     */
    private void stopCrawling() {
        Intent intent = new Intent("com.jdcrawler.STOP_CRAWLING");
        // 添加系统级广播标志
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setPackage(getPackageName());
        
        sendBroadcast(intent);
        
        Log.d(TAG, "✓ 已发送停止爬取广播: com.jdcrawler.STOP_CRAWLING (系统级)");
        
        showToast("正在停止爬取...");
    }
    
    /**
     * 导出数据
     */
    private void exportData() {
        if (currentProductCount == 0) {
            showToast("没有数据可以导出");
            return;
        }
        
        // 这里可以触发导出操作
        Intent intent = new Intent("com.jdcrawler.EXPORT_DATA");
        // 添加系统级广播标志
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setPackage(getPackageName());
        
        sendBroadcast(intent);
        Log.d(TAG, "✓ 已发送导出数据广播: com.jdcrawler.EXPORT_DATA (系统级)");
        showToast("正在导出数据...");
    }
    
    /**
     * 分享数据
     */
    private void shareData() {
        if (lastExportedFile.isEmpty()) {
            showToast("请先导出数据");
            return;
        }
        
        try {
            File file = new File(lastExportedFile);
            if (!file.exists()) {
                showToast("文件不存在，请重新导出");
                return;
            }
            
            Uri fileUri = FileProvider.getUriForFile(this, 
                getPackageName() + ".fileprovider", file);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "京东商品数据");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "分享Excel文件"));
            
        } catch (Exception e) {
            showToast("分享失败: " + e.getMessage());
        }
    }
    
    /**
     * 打开无障碍设置
     */
    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            showToast("请找到并开启'京东爬虫助手'服务");
        } catch (Exception e) {
            showToast("无法打开设置页面");
        }
    }
    
    /**
     * 显示帮助对话框
     */
    private void showHelpDialog() {
        new AlertDialog.Builder(this)
            .setTitle("使用帮助")
            .setMessage(getString(R.string.tip_how_to_use))
            .setPositiveButton("我知道了", null)
            .setNeutralButton("打开设置", (dialog, which) -> openAccessibilitySettings())
            .show();
    }
    
    /**
     * 显示使用提示
     */
    private void showUsageTip() {
        if (!isAccessibilityServiceEnabled()) {
            new AlertDialog.Builder(this)
                .setTitle("欢迎使用京东爬虫助手")
                .setMessage("首次使用需要开启无障碍服务权限\\n\\n点击'开启权限'按钮前往设置页面")
                .setPositiveButton("开启权限", (dialog, which) -> openAccessibilitySettings())
                .setNegativeButton("稍后设置", null)
                .setCancelable(false)
                .show();
        }
    }
    /**
     * 更新UI状态
     */
    private void updateUI() {
        runOnUiThread(() -> {
            // 更新服务状态
            boolean serviceEnabled = isAccessibilityServiceEnabled();
            tvServiceStatus.setText(serviceEnabled ? "无障碍服务：已开启" : "无障碍服务：未开启");
            tvServiceStatus.setTextColor(serviceEnabled ? 
                getResources().getColor(android.R.color.holo_green_dark) : 
                getResources().getColor(android.R.color.holo_red_dark));
            
            // 更新爬取状态
            tvCrawlingStatus.setText(isCrawling ? "状态：正在爬取" : "状态：待机中");
            tvCrawlingStatus.setTextColor(isCrawling ? 
                getResources().getColor(android.R.color.holo_blue_bright) : 
                getResources().getColor(android.R.color.darker_gray));
            
            // 更新商品计数
            tvProductCount.setText("已收集商品：" + currentProductCount);
            
            // 更新进度
            if (totalProductCount > 0) {
                int progress = (int) ((currentProductCount * 100.0) / totalProductCount);
                progressBar.setProgress(progress);
                tvCurrentProgress.setText(progress + "%");
            } else {
                progressBar.setProgress(0);
                tvCurrentProgress.setText("0%");
            }
            
            // 更新按钮状态
            btnStartCrawling.setEnabled(serviceEnabled && !isCrawling);
            btnStopCrawling.setEnabled(isCrawling);
            btnExportData.setEnabled(currentProductCount > 0);
            btnShareData.setEnabled(!lastExportedFile.isEmpty());
        });
    }
    
    /**
     * 检查无障碍服务是否启用
     */
    private boolean isAccessibilityServiceEnabled() {
        try {
            String service = getPackageName() + "/com.jdcrawler.app.service.JDCrawlerAccessibilityService";
            String enabledServices = Settings.Secure.getString(
                getContentResolver(), 
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            
            if (enabledServices != null) {
                return enabledServices.contains(service);
            }
        } catch (Exception e) {
            Log.e(TAG, "检查无障碍服务状态失败", e);
        }
        return false;
    }
    
    /**
     * 检查京东APP是否已安装
     */
    private boolean isJDAppInstalled() {
        // 方案1: 检测常见京东APP包名
        String[] jdPackageNames = {
            "com.jingdong.app.mall",           // 官方版本
            "com.jd.jdmobile",                 // 国际版本
            "com.jingdong.jdlite",            // 京东极速版
            "com.jd.jdlite",                   // 另一个极速版包名
            "com.jingdong.app.mall.ptlogin"   // 登录相关
        };
        
        PackageManager pm = getPackageManager();
        
        // 第一阶段：通过PackageManager检查
        for (String packageName : jdPackageNames) {
            try {
                android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                Log.d(TAG, "✓ 通过PackageManager找到京东APP: " + packageName);
                Log.d(TAG, "应用版本: " + packageInfo.versionName);
                showToast("检测到京东APP: " + packageName);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "× 未找到包名: " + packageName);
            }
        }
        
        // 第二阶段：通过Intent检查
        try {
            Intent launchIntent = pm.getLaunchIntentForPackage("com.jingdong.app.mall");
            if (launchIntent != null) {
                Log.d(TAG, "✓ 通过Intent找到京东APP");
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Intent检查失败", e);
        }
        
        // 第三阶段：通过应用列表检查
        try {
            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo("com.jingdong.app.mall", 0);
            if (appInfo != null) {
                Log.d(TAG, "✓ 通过应用信息找到京东APP");
                Log.d(TAG, "应用路径: " + appInfo.sourceDir);
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "× 应用信息检查未找到京东APP");
        }
        
        // 第四阶段：列出所有可能相关的应用（调试用）
        listPossibleJDApps();
        
        Log.w(TAG, "× 所有检测方案都未找到京东APP");
        return false;
    }
    
    /**
     * 列出可能的京东相关应用（调试用）
     */
    private void listPossibleJDApps() {
        try {
            PackageManager pm = getPackageManager();
            List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            
            Log.d(TAG, "=== 搜索可能的京东相关应用 ===");
            int foundCount = 0;
            
            for (android.content.pm.ApplicationInfo app : apps) {
                String packageName = app.packageName.toLowerCase();
                if (packageName.contains("jd") || 
                    packageName.contains("jingdong") || 
                    packageName.contains("京东")) {
                    
                    String appName = pm.getApplicationLabel(app).toString();
                    Log.d(TAG, "可能的京东应用: " + app.packageName + " (" + appName + ")");
                    foundCount++;
                }
            }
            
            if (foundCount == 0) {
                Log.d(TAG, "未找到任何京东相关应用");
            } else {
                Log.d(TAG, "共找到 " + foundCount + " 个可能相关的应用");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "列出应用时发生错误", e);
        }
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 权限请求结果处理
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("存储权限已授予");
            } else {
                showToast("存储权限被拒绝，可能影响数据导出功能");
            }
        }
    }
    
    /**
     * Activity结果处理
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                showToast("悬浮窗权限已开启");
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }
    
    /**
     * 广播接收器 - 接收爬虫服务的状态更新
     */
    private class CrawlerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            
            switch (action) {
                case "com.jdcrawler.SERVICE_STATUS":
                    boolean connected = intent.getBooleanExtra("connected", false);
                    isServiceConnected = connected;
                    updateUI();
                    break;
                    
                case "com.jdcrawler.PROGRESS_UPDATE":
                    currentProductCount = intent.getIntExtra("current", 0);
                    totalProductCount = intent.getIntExtra("total", 0);
                    updateUI();
                    break;
                    
                case "com.jdcrawler.CRAWLING_START":
                    isCrawling = true;
                    updateUI();
                    showToast("爬取已开始");
                    break;
                    
                case "com.jdcrawler.CRAWLING_STOP":
                    isCrawling = false;
                    updateUI();
                    break;
                    
                case "com.jdcrawler.CRAWLING_COMPLETE":
                    isCrawling = false;
                    int productCount = intent.getIntExtra("productCount", 0);
                    updateUI();
                    showToast("爬取完成！共收集 " + productCount + " 个商品");
                    break;
            }
        }
    }
}
