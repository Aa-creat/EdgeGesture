package com.omarea.gesture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * 现代版无障碍服务入口，继承自原有服务核心。
 * 类名定为 ModernGestureService，严格避免以 "AccessibilityServiceGesture" 结尾，
 * 彻底解决旧版应用通过 endsWith("AccessibilityServiceGesture") 产生的误判混淆。
 * 
 * 同时接入 Android 14+ / Android 16 (targetSdk 36) 前台服务保活通道 (FOREGROUND_SERVICE_SPECIAL_USE)，
 * 将进程优先级提升为 FOREGROUND_SERVICE，彻底免疫 ColorOS Athena 等激进后台待机清理。
 */
class ModernGestureService : AccessibilityServiceGesture() {

    companion object {
        private const val TAG = "ModernGestureService"
        private const val CHANNEL_ID = "edge_gesture_keep_alive"
        private const val NOTIFICATION_ID = 9527

        @Volatile
        var isServiceRunning: Boolean = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        startKeepAliveForeground()
    }

    private fun startKeepAliveForeground() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "手势服务常驻保活",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "保持无障碍手势服务常驻，防止系统休眠时被杀导致应用服务异常"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("EdgeGesture 手势服务运行中")
                .setContentText("轻触打开设置")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "Foreground keep-alive service started successfully.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }
}
