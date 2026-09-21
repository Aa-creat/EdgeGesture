package com.omarea.gesture.util;

import com.omarea.gesture.ActionModel;

public class GlobalState {
    // 使用连续动作
    public static boolean consecutive = false;
    // 连续动作
    public static ActionModel consecutiveAction = null;

    // 电池电量
    public static int batteryCapacity = -1;
    // 小横条是否使用电量
    public static boolean useBatteryCapacity = false;

    public static long lastBackHomeTime = 0;

    public static boolean isLandscapf = false;
    public static boolean testMode = false;
    public static int iosBarColor = Integer.MIN_VALUE;
    public static Runnable updateBar;
    public static int displayHeight = 2340;
    public static int displayWidth = 1080;
    // 增强模式（需要Root或者ADB）
    public static boolean enhancedMode = false;
}
