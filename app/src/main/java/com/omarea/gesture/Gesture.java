package com.omarea.gesture;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Toast;

public class Gesture extends Application {
    public static final Handler handler = new Handler();
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    private static Vibrator vibrator;
    public static SharedPreferences config;

    public static void vibrate(VibrateMode mode, View view) {
        if (vibrator == null) {
            vibrator = (Vibrator) (context.getSystemService(Context.VIBRATOR_SERVICE));
        }

        if (vibrator.hasVibrator()) {
            if (config.getBoolean(SpfConfig.VIBRATOR_USE_SYSTEM, SpfConfig.VIBRATOR_USE_SYSTEM_DEFAULT)) {
                switch (mode) {
                    case VIBRATE_CLICK: {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        return;
                    }
                    case VIBRATE_PRESS: {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        return;
                    }
                    case VIBRATE_SLIDE_HOVER: {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        return;
                    }
                    case VIBRATE_SLIDE: {
                        if (config.getBoolean(SpfConfig.VIBRATOR_QUICK_SLIDE, SpfConfig.VIBRATOR_QUICK_SLIDE_DEFAULT)) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        }
                        return;
                    }
                    default: {
                    }
                }
            } else {
                if (mode == VibrateMode.VIBRATE_SLIDE) {
                    if (!config.getBoolean(SpfConfig.VIBRATOR_QUICK_SLIDE, SpfConfig.VIBRATOR_QUICK_SLIDE_DEFAULT)) {
                        return;
                    }
                }

                boolean longTime = mode == VibrateMode.VIBRATE_SLIDE_HOVER || mode == VibrateMode.VIBRATE_PRESS;

                vibrator.cancel();
                int time = longTime ? config.getInt(SpfConfig.VIBRATOR_TIME_LONG, SpfConfig.VIBRATOR_TIME_LONG_DEFAULT) : config.getInt(SpfConfig.VIBRATOR_TIME, SpfConfig.VIBRATOR_TIME_DEFAULT);
                int amplitude = longTime ? config.getInt(SpfConfig.VIBRATOR_AMPLITUDE_LONG, SpfConfig.VIBRATOR_AMPLITUDE_LONG_DEFAULT) : config.getInt(SpfConfig.VIBRATOR_AMPLITUDE, SpfConfig.VIBRATOR_AMPLITUDE_DEFAULT);
                if (time > 0 && amplitude > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(time, amplitude));
                    } else {
                        vibrator.vibrate(new long[]{0, time, amplitude}, -1);
                    }
                }
            }
        }
    }

    public static void toast(final String text, final int time) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, time).show();
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        config = getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        context = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                android.util.Log.e("EdgeGesture", "Uncaught exception on thread: " + thread.getName(), throwable);

                // 如果是非主线程异常（如后台解析、异步任务等），拦截并不终止主进程，避免无障碍服务被系统杀死导致显示服务异常
                if (Looper.getMainLooper().getThread() != thread) {
                    android.util.Log.w("EdgeGesture", "Intercepted background crash to keep AccessibilityService alive.");
                    return;
                }

                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }

    public static enum VibrateMode {
        VIBRATE_CLICK,
        VIBRATE_PRESS,
        VIBRATE_SLIDE_HOVER,
        VIBRATE_SLIDE
    }
}
