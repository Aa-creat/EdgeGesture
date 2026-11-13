package com.omarea.gesture.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.omarea.gesture.ModernGestureService

enum class ServiceStatus {
    RUNNING,     // 服务正常运行中
    CRASHED,     // 系统设置已开启，但底层已断开/被杀 (mCrashed / 应用服务异常)
    DISABLED     // 服务未在系统设置中开启
}

object ServiceStateHelper {
    fun getServiceStatus(context: Context): ServiceStatus {
        val serviceCompName = "${context.packageName}/com.omarea.gesture.ModernGestureService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val isSettingEnabled = enabledServices.contains(serviceCompName)

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val isBound = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { it.resolveInfo?.serviceInfo?.packageName == context.packageName } ?: false

        val isRunning = ModernGestureService.isServiceRunning || isBound

        return when {
            isSettingEnabled && isRunning -> ServiceStatus.RUNNING
            isSettingEnabled && !isRunning -> ServiceStatus.CRASHED
            else -> ServiceStatus.DISABLED
        }
    }
}
