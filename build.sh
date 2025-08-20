#!/bin/bash

# 京东爬虫助手构建脚本
# 用于快速构建和安装应用

echo "======================================"
echo "京东商品深度爬虫助手 - 构建脚本"
echo "======================================"

# 检查是否在正确的目录
if [ ! -f "settings.gradle" ]; then
    echo "❌ 错误: 请在项目根目录运行此脚本"
    exit 1
fi

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到Java运行环境"
    echo "请安装JDK 11或更高版本"
    exit 1
fi

# 检查Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  警告: 未设置ANDROID_HOME环境变量"
    echo "请确保已安装Android SDK"
fi

echo "🔧 开始构建应用..."

# 清理项目
echo "📦 清理项目..."
./gradlew clean

if [ $? -ne 0 ]; then
    echo "❌ 清理失败"
    exit 1
fi

# 构建APK
echo "🏗️  构建APK..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ 构建失败"
    exit 1
fi

# 检查APK是否生成
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "✅ 构建成功!"
    echo "📱 APK文件位置: $APK_PATH"
    echo "📊 文件大小: $(du -h $APK_PATH | cut -f1)"
else
    echo "❌ APK文件未找到"
    exit 1
fi

# 检查是否有连接的设备
echo "🔍 检查连接的Android设备..."
ADB_DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)

if [ "$ADB_DEVICES" -gt 0 ]; then
    echo "📱 找到 $ADB_DEVICES 个连接的设备"
    read -p "是否要安装到设备上? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "📲 安装应用到设备..."
        adb install -r "$APK_PATH"
        
        if [ $? -eq 0 ]; then
            echo "✅ 安装成功!"
            echo ""
            echo "📋 使用说明:"
            echo "1. 在设备上找到'京东爬虫助手'应用"
            echo "2. 打开应用并授予无障碍服务权限"
            echo "3. 打开京东APP进入店铺页面"
            echo "4. 回到爬虫应用点击'开始爬取'"
            echo ""
            echo "📁 爬取的Excel文件将保存在:"
            echo "   /Android/data/com.jdcrawler.app/files/Documents/JDCrawler/"
        else
            echo "❌ 安装失败"
            exit 1
        fi
    fi
else
    echo "📱 未检测到连接的Android设备"
    echo "💡 要安装应用，请:"
    echo "1. 用USB线连接Android设备"
    echo "2. 开启USB调试模式"
    echo "3. 运行: adb install -r $APK_PATH"
fi

echo ""
echo "✨ 构建完成!"
echo "======================================"