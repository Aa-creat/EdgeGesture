# Shizuku 保护规则（保持 Binder 与反射方法）
-keep class rikka.shizuku.** { *; }
-keepclassmembers class rikka.shizuku.Shizuku {
    private static ** newProcess(**);
}

# 保持无障碍服务与主入口
-keep class com.omarea.gesture.AccessibilityServiceGesture { *; }
-keep class com.omarea.gesture.SettingsActivity { *; }
-keep class com.omarea.gesture.StartActivity { *; }

