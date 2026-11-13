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
    public static final Handler handler = new Handler(Looper.getMainLooper());
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

        // 1. 主线程永生守卫机制（Cockroach Loop Guard）
        // 在 MainLooper 中持续循环 loop() 并捕获所有异常，防止主线程因动画、UI 渲染、触摸等偶发异常崩溃导致无障碍连接断开 (mCrashed)
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Looper.loop();
                    } catch (Throwable t) {
                        android.util.Log.e("EdgeGesture", "Main looper caught uncaught exception to keep process alive: ", t);
                    }
                }
            }
        });

        // 2. 全局未捕获异常兜底拦截（拦截所有后台线程未捕获异常）
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                android.util.Log.e("EdgeGesture", "Uncaught exception on thread: " + thread.getName(), throwable);
                android.util.Log.w("EdgeGesture", "Intercepted crash to prevent accessibility disconnection.");
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
