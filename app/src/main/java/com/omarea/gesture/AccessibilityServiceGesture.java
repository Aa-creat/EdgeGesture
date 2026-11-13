package com.omarea.gesture;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import com.omarea.gesture.remote.RemoteAPI;
import com.omarea.gesture.ui.QuickPanel;
import com.omarea.gesture.util.GlobalState;
import com.omarea.gesture.util.Recents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import android.util.Log;

public class AccessibilityServiceGesture extends AccessibilityService {
    public Recents recents = new Recents();
    private com.omarea.gesture.ui.whitebar.ModernWhiteBar modernWhiteBar = null;
    private com.omarea.gesture.ui.gesture.ModernSideGestureBar modernSideGestureBar = null;
    private BroadcastReceiver configChanged = null;
    private BroadcastReceiver serviceDisable = null;
    private BroadcastReceiver screenStateReceiver = null;
    private SharedPreferences appSwitchBlackList;
    private BatteryReceiver batteryReceiver;

    private boolean ignored(String packageName) {
        if (packageName == null) return true;
        List<String> ims = recents.inputMethods;
        return ims != null && ims.contains(packageName);
    }

    // 检测应用是否是可以打开的
    private boolean canOpen(String packageName) {
        if (packageName == null) return false;
        if (recents.blackList.contains(packageName)) {
            return false;
        } else if (recents.whiteList.contains(packageName)) {
            return true;
        } else {
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    recents.whiteList.add(packageName);
                    return true;
                } else {
                    recents.blackList.add(packageName);
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
    }

    // 启动器应用（桌面）
    private ArrayList<String> getLauncherApps() {
        ArrayList<String> launcherApps = new ArrayList<>();
        try {
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
            resolveIntent.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> resolveinfoList = getPackageManager().queryIntentActivities(resolveIntent, 0);
            if (resolveinfoList != null) {
                for (ResolveInfo resolveInfo : resolveinfoList) {
                    if (resolveInfo != null && resolveInfo.activityInfo != null) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        if (!("com.android.settings".equals(packageName))) { // MIUI的设置也算个桌面，什么鬼
                            launcherApps.add(packageName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("AccessibilityGesture", "Failed to query launcher apps", e);
        }
        return launcherApps;
    }

    // 输入法应用
    private ArrayList<String> getInputMethods() {
        ArrayList<String> inputMethods = new ArrayList<>();
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                List<InputMethodInfo> imms = imm.getInputMethodList();
                if (imms != null) {
                    for (InputMethodInfo inputMethodInfo : imms) {
                        if (inputMethodInfo != null) {
                            inputMethods.add(inputMethodInfo.getPackageName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("AccessibilityGesture", "Failed to query input methods", e);
        }
        return inputMethods;
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        if (recents.inputMethods.isEmpty()) {
            recents.inputMethods.addAll(getInputMethods());
            recents.launcherApps.addAll(getLauncherApps());
        }

        CharSequence packageName = event.getPackageName();
        if (packageName == null || "com.omarea.filter".equals(packageName.toString())) {
            return;
        }

        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (GlobalState.testMode && !packageName.toString().equals(getPackageName())) {
                GlobalState.testMode = false;
                createPopupView(false);
            }
            handlePackageChanged(packageName.toString());
        }
    }

    private void handlePackageChanged(String packageNameStr) {
        if (packageNameStr == null || packageNameStr.equals(getPackageName())) {
            return;
        }

        try {
            List<String> launchers = recents.launcherApps;
            if (launchers != null && launchers.contains(packageNameStr)) {
                recents.addRecent(Intent.CATEGORY_HOME);
                GlobalState.lastBackHomeTime = System.currentTimeMillis();
            } else if (!ignored(packageNameStr) && canOpen(packageNameStr) && (appSwitchBlackList == null || !appSwitchBlackList.contains(packageNameStr))) {
                recents.addRecent(packageNameStr);
                GlobalState.lastBackHomeTime = 0;
            }
        } catch (Throwable t) {
            Log.e("AccessibilityGesture", "Error handling package changed", t);
        }
    }

    private void setBatteryReceiver() {
        if (batteryReceiver == null) {
            batteryReceiver = new BatteryReceiver(this);
            registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
            registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

        setServiceInfo();

        if (modernWhiteBar == null) {
            com.omarea.gesture.core.config.AppConfigRepository repo = com.omarea.gesture.core.config.AppConfigRepository.Companion.getInstance(this);
            com.omarea.gesture.core.dispatcher.ActionDispatcher dispatcher = new com.omarea.gesture.core.dispatcher.ActionDispatcher(this);
            com.omarea.gesture.core.haptics.HapticsManager haptics = new com.omarea.gesture.core.haptics.HapticsManager(this, repo);
            modernWhiteBar = new com.omarea.gesture.ui.whitebar.ModernWhiteBar(this, repo, dispatcher, haptics);
            modernSideGestureBar = new com.omarea.gesture.ui.gesture.ModernSideGestureBar(this, repo, dispatcher, haptics);
        }

        if (appSwitchBlackList == null) {
            appSwitchBlackList = getSharedPreferences(SpfConfig.AppSwitchBlackList, Context.MODE_PRIVATE);
        }

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        GlobalState.displayWidth = point.x;
        GlobalState.displayHeight = point.y;
        GlobalState.consecutive = Gesture.config.getBoolean(SpfConfig.IOS_BAR_CONSECUTIVE, SpfConfig.IOS_BAR_CONSECUTIVE_DEFAULT);

        GlobalState.useBatteryCapacity = Gesture.config.getBoolean(SpfConfig.IOS_BAR_POP_BATTERY, SpfConfig.IOS_BAR_POP_BATTERY_DEFAULT);
        if (GlobalState.useBatteryCapacity) {
            setBatteryReceiver();
        }

        if (configChanged == null) {
            configChanged = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    GlobalState.consecutive = Gesture.config.getBoolean(SpfConfig.IOS_BAR_CONSECUTIVE, SpfConfig.IOS_BAR_CONSECUTIVE_DEFAULT);
                    GlobalState.useBatteryCapacity = Gesture.config.getBoolean(SpfConfig.IOS_BAR_POP_BATTERY, SpfConfig.IOS_BAR_POP_BATTERY_DEFAULT);
                    if (GlobalState.useBatteryCapacity) {
                        setBatteryReceiver();
                    } else if (batteryReceiver != null) {
                        unregisterReceiver(batteryReceiver);
                        batteryReceiver = null;
                    }

                    String action = intent != null ? intent.getAction() : null;
                    if (action != null && action.equals(getString(R.string.app_switch_changed))) {
                        if (recents != null) {
                            recents.clear();
                            Gesture.toast("OK！", Toast.LENGTH_SHORT);
                        }
                    } else {
                        new AdbProcessExtractor().updateAdbProcessState(context, false);
                        if (action != null && action.equals(getString(R.string.action_adb_process))) {
                            if (GlobalState.enhancedMode) {
                                setResultCode(0);
                                setResultData("Nice, The enhancement mode has been activated ^_^");
                            } else {
                                setResultCode(5);
                                setResultData("Unable to start enhanced mode >_<");
                            }
                        }
                        createPopupView(false);
                    }
                }
            };

            ContextCompat.registerReceiver(this, configChanged, new IntentFilter(getString(R.string.action_config_changed)), ContextCompat.RECEIVER_NOT_EXPORTED);
            ContextCompat.registerReceiver(this, configChanged, new IntentFilter(getString(R.string.app_switch_changed)), ContextCompat.RECEIVER_NOT_EXPORTED);
            ContextCompat.registerReceiver(this, configChanged, new IntentFilter(getString(R.string.action_adb_process)), ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        if (serviceDisable == null) {
            serviceDisable = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        disableSelf();
                    }
                    stopSelf();
                }
            };
            ContextCompat.registerReceiver(this, serviceDisable, new IntentFilter(getString(R.string.action_service_disable)), ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        createPopupView(false);

        if (screenStateReceiver == null) {
            screenStateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                        if (modernWhiteBar != null) {
                            modernWhiteBar.onScreenOff();
                        }
                    } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                        if (modernWhiteBar != null) {
                            modernWhiteBar.onScreenOn();
                        }
                    }
                }
            };
            IntentFilter screenFilter = new IntentFilter();
            screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenFilter.addAction(Intent.ACTION_USER_PRESENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                screenFilter.addAction(Intent.ACTION_USER_UNLOCKED);
            }
            registerReceiver(screenStateReceiver, screenFilter);
        }

        Collections.addAll(recents.blackList, getResources().getStringArray(R.array.app_switch_black_list));

        new AdbProcessExtractor().updateAdbProcessState(this, true);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
    }

    // 监测屏幕旋转
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (modernSideGestureBar != null) {
            modernSideGestureBar.onConfigurationChanged();
        }
    }

    private void createPopupView(boolean delayed) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable(){
            @Override
            public void run() {
                if (modernWhiteBar != null) {
                    modernWhiteBar.refreshTestMode();
                }
                if (modernSideGestureBar != null) {
                    modernSideGestureBar.refreshTestMode();
                }
            }
        }, (delayed ? 500 : 0));
    }

    private void setServiceInfo() {
        try {
            AccessibilityServiceInfo accessibilityServiceInfo = getServiceInfo();
            if (accessibilityServiceInfo == null) {
                accessibilityServiceInfo = new AccessibilityServiceInfo();
            }
            accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            accessibilityServiceInfo.notificationTimeout = 100;
            accessibilityServiceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            accessibilityServiceInfo.flags = 0;
            setServiceInfo(accessibilityServiceInfo);
        } catch (Exception e) {
            Log.e("AccessibilityGesture", "Failed to set service info", e);
        }
    }

    @Override
    public void onDestroy() {
        if (modernWhiteBar != null) {
            modernWhiteBar.onDestroy();
            modernWhiteBar = null;
        }

        if (modernSideGestureBar != null) {
            modernSideGestureBar.onDestroy();
            modernSideGestureBar = null;
        }

        if (configChanged != null) {
            try {
                unregisterReceiver(configChanged);
            } catch (Exception ignored) {}
            configChanged = null;
        }

        if (screenStateReceiver != null) {
            try {
                unregisterReceiver(screenStateReceiver);
            } catch (Exception ignored) {}
            screenStateReceiver = null;
        }

        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
            } catch (Exception ignored) {}
            batteryReceiver = null;
        }

        if (serviceDisable != null) {
            try {
                unregisterReceiver(serviceDisable);
            } catch (Exception ignored) {}
            serviceDisable = null;
        }

        super.onDestroy();
    }
}
