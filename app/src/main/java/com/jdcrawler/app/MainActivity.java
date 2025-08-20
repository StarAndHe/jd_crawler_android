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
        
        if (!isJDAppInstalled()) {
            showToast("请先安装京东APP");
            return;
        }
        
        // 发送开始爬取的广播
        Intent intent = new Intent("com.jdcrawler.START_CRAWLING");
        sendBroadcast(intent);
        
        showToast("开始爬取，请打开京东APP并浏览商品页面");
    }
    
    /**
     * 停止爬取
     */
    private void stopCrawling() {
        Intent intent = new Intent("com.jdcrawler.STOP_CRAWLING");
        sendBroadcast(intent);
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
        sendBroadcast(intent);
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
            if (isServiceConnected) {
                tvServiceStatus.setText("服务状态：已连接");
                tvServiceStatus.setTextColor(getColor(R.color.status_ready));
            } else {
                tvServiceStatus.setText("服务状态：未连接");
                tvServiceStatus.setTextColor(getColor(R.color.status_error));
            }
            
            // 更新爬取状态
            if (isCrawling) {
                tvCrawlingStatus.setText("爬取状态：进行中");
                tvCrawlingStatus.setTextColor(getColor(R.color.status_crawling));
                btnStartCrawling.setEnabled(false);
                btnStopCrawling.setEnabled(true);
            } else {
                tvCrawlingStatus.setText("爬取状态：已停止");
                tvCrawlingStatus.setTextColor(getColor(R.color.status_stopped));
                btnStartCrawling.setEnabled(isServiceConnected);
                btnStopCrawling.setEnabled(false);
            }
            
            // 更新商品计数
            tvProductCount.setText(String.format("已收集商品：%d个", currentProductCount));
            
            // 更新进度
            if (totalProductCount > 0) {
                int progress = (int) ((float) currentProductCount / totalProductCount * 100);
                progressBar.setProgress(progress);
                tvCurrentProgress.setText(String.format("进度：%d/%d (%d%%)", 
                    currentProductCount, totalProductCount, progress));
                progressBar.setVisibility(View.VISIBLE);
                tvCurrentProgress.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                tvCurrentProgress.setVisibility(View.GONE);
            }
            
            // 更新按钮状态
            btnExportData.setEnabled(currentProductCount > 0);
            btnShareData.setEnabled(!lastExportedFile.isEmpty());
        });
    }
    
    /**
     * 检查无障碍服务是否启用
     */
    private boolean isAccessibilityServiceEnabled() {
        try {
            String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            
            if (enabledServices != null) {
                return enabledServices.contains(getPackageName() + "/" + 
                    "com.jdcrawler.app.service.JDCrawlerAccessibilityService");
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return false;
    }
    
    /**
     * 检查京东APP是否安装
     */
    private boolean isJDAppInstalled() {
        try {
            getPackageManager().getPackageInfo("com.jingdong.app.mall", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("存储权限已授予");
            } else {
                showToast("需要存储权限才能保存文件");
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                showToast("悬浮窗权限已开启");
            } else {
                showToast("悬浮窗权限未开启，将无法显示悬浮进度");
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }
    
    /**
     * 广播接收器
     */
    private class CrawlerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            
            switch (action) {
                case "com.jdcrawler.SERVICE_STATUS":
                    isServiceConnected = intent.getBooleanExtra("connected", false);
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
                    showToast("爬取已停止");
                    break;
                    
                case "com.jdcrawler.CRAWLING_COMPLETE":
                    isCrawling = false;
                    int productCount = intent.getIntExtra("productCount", 0);
                    currentProductCount = productCount;
                    updateUI();
                    showToast("爬取完成！共收集 " + productCount + " 个商品");
                    break;
            }
        }
    }
}
