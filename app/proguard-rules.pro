# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Apache POI classes
-keep class org.apache.poi.** { *; }
-keep class com.zaxxer.sparsebits.** { *; }
-keep class org.apache.commons.** { *; }
-keep class org.apache.xmlbeans.** { *; }

# Keep accessibility service
-keep class com.jdcrawler.app.service.** { *; }

# Keep model classes
-keep class com.jdcrawler.app.model.** { *; }

# Keep utility classes
-keep class com.jdcrawler.app.util.** { *; }

# Don't warn about missing classes
-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.xmlbeans.**