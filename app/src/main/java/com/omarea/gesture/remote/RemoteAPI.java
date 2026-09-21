package com.omarea.gesture.remote;

import android.content.Context;
import com.omarea.gesture.Gesture;
import com.omarea.gesture.core.shizuku.ShizukuManager;

/**
 * 兼容性存根类，原本地 HTTP 通信已全部迁移至 Shizuku
 */
public class RemoteAPI {
    public static boolean isOnline() {
        Context ctx = Gesture.context;
        if (ctx != null) {
            return ShizukuManager.Companion.getInstance(ctx).isAvailable();
        }
        return false;
    }

    public static String[] getRecents() {
        return new String[0];
    }

    public static int getBarAutoColor(boolean delayScreenCap) {
        return Integer.MIN_VALUE;
    }

    public static String getColorPollingApps() {
        return "";
    }

    public static void fixDelay() {
    }

    public static void xiaomiHandymode(final int mode) {
    }

    public static void startActivity(final String params) {
    }
}
