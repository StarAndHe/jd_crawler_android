# 🚀 GitHub Actions自动构建APK指南

## 📋 快速开始

### 第一步：创建GitHub仓库
1. 登录GitHub，创建新仓库（建议命名为`jd-crawler-android`）
2. 将项目代码上传到仓库

### 第二步：启用GitHub Actions
1. 进入仓库页面
2. 点击顶部的"Actions"标签
3. GitHub会自动检测到Android项目并建议工作流

### 第三步：等待自动构建
1. 每次推送代码到`main`分支会自动触发构建
2. 构建过程大约需要5-10分钟
3. 完成后可以下载APK文件

## 📱 如何下载APK

### 方法1：从Actions页面下载
1. 进入仓库的"Actions"页面
2. 点击最新的构建记录
3. 在"Artifacts"部分下载APK文件

### 方法2：从Releases页面下载
1. 进入仓库的"Releases"页面
2. 下载最新版本的APK文件
3. 包含debug和release两个版本

## 🔧 构建配置说明

### 自动构建触发条件
- 推送到`main`或`master`分支
- 创建Pull Request
- 手动触发（在Actions页面点击"Run workflow"）

### 构建输出
- **JDCrawler-debug-日期.apk**: 调试版本，包含详细日志
- **JDCrawler-release-日期.apk**: 发布版本，体积更小，性能更好

### 文件保存期限
- Debug版本：保存30天
- Release版本：保存90天

## 📋 使用步骤

### 1. 上传项目到GitHub

#### 方法A：使用GitHub Desktop（推荐新手）
1. 下载并安装GitHub Desktop
2. 点击"File" → "Add local repository"
3. 选择项目文件夹
4. 点击"Publish repository"

#### 方法B：使用命令行
```bash
# 进入项目目录
cd jd_crawler_android

# 初始化Git仓库
git init

# 添加远程仓库
git remote add origin https://github.com/你的用户名/jd-crawler-android.git

# 添加所有文件
git add .

# 提交
git commit -m "初始版本"

# 推送到GitHub
git push -u origin main
```

#### 方法C：直接上传文件
1. 在GitHub仓库页面点击"uploading an existing file"
2. 将整个项目文件夹拖拽到页面
3. 添加提交信息并提交

### 2. 监控构建过程

1. **查看构建状态**
   - 绿色✓：构建成功
   - 红色✗：构建失败
   - 黄色●：正在构建

2. **查看构建日志**
   - 点击构建记录查看详细日志
   - 如果失败可以查看错误信息

3. **构建完成通知**
   - 可以在GitHub设置中开启邮件通知
   - 或者安装GitHub手机APP接收推送

### 3. 下载和安装APK

1. **下载APK文件**
   ```
   推荐下载：JDCrawler-release-日期.apk
   ```

2. **安装到Android设备**
   - 确保开启"未知来源"权限
   - 直接点击APK文件安装

3. **首次使用设置**
   - 开启无障碍服务权限
   - 授予存储权限
   - 打开京东APP测试

## ⚙️ 高级配置（可选）

### 自定义构建分支
如果想要在其他分支也触发构建，可以修改`.github/workflows/android.yml`：

```yaml
on:
  push:
    branches: [ main, master, develop, feature/* ]
```

### 添加构建通知
在仓库设置中配置Webhooks，可以将构建结果发送到钉钉、微信等：

```yaml
- name: 通知构建结果
  if: always()
  run: |
    curl -X POST "钉钉机器人URL" \
    -H "Content-Type: application/json" \
    -d '{"msgtype": "text", "text": {"content": "APK构建完成"}}'
```

### 自动发布到蒲公英
添加第三方分发平台的发布步骤：

```yaml
- name: 上传到蒲公英
  run: |
    curl -F "file=@release/JDCrawler-release.apk" \
    -F "apiKey=${{ secrets.PGY_API_KEY }}" \
    https://www.pgyer.com/apiv2/app/upload
```

## 🚨 常见问题解决

### 1. 构建失败
**错误：Gradle build failed**
- 检查`build.gradle`文件语法
- 确保所有依赖库版本正确

**错误：Java version mismatch**
- GitHub Actions使用Java 17，代码必须兼容

### 2. APK下载问题
**找不到Artifacts**
- 确保构建成功完成
- Artifacts可能需要登录GitHub才能下载

**APK文件过大**
- Release版本会自动压缩
- 可以在`proguard-rules.pro`中添加更多优化规则

### 3. 权限问题
**无法安装APK**
- 开启"允许安装未知来源应用"
- 检查设备是否支持Android 7.0+

**无障碍服务无法开启**
- 重启设备后重试
- 确保应用已完全安装

## 📊 构建状态徽章

在README.md中添加构建状态徽章：

```markdown
![Android CI](https://github.com/你的用户名/jd-crawler-android/workflows/Android%20CI/CD/badge.svg)
```

## 🎯 最佳实践

### 版本管理
- 使用语义化版本号（如v1.0.0, v1.1.0）
- 每次发布都创建Git标签

### 代码质量
- 每次提交前本地测试
- 使用有意义的提交信息

### 安全注意
- 不要在代码中包含密钥或敏感信息
- 使用GitHub Secrets存储敏感配置

---

## 📞 技术支持

如果遇到问题，可以：
1. 查看GitHub Actions的构建日志
2. 在仓库中创建Issue
3. 检查Android开发环境配置

**祝您使用愉快！** 🎉