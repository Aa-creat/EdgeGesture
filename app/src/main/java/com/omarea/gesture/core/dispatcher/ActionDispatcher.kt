package com.omarea.gesture.core.dispatcher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.omarea.gesture.AccessibilityServiceGesture
import com.omarea.gesture.core.model.Action

/**
 * 动作调度分发中枢
 * 负责将手势触发的 Action 路由至无障碍全局操作、应用启动或 Shizuku 特权执行
 */
class ActionDispatcher(private val context: Context) {

    fun dispatch(action: Action, accessibilityService: AccessibilityServiceGesture? = null) {
        when (action) {
            is Action.None -> {
                // 无操作
            }
            is Action.Back -> {
                performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_BACK)
            }
            is Action.Home -> {
                performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_HOME)
            }
            is Action.Recents -> {
                performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
            is Action.Notifications -> {
                performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            }
            is Action.QuickSettings -> {
                performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            }
            is Action.PowerDialog -> {
                performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            }
            is Action.LockScreen -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            is Action.Screenshot -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            is Action.SplitScreen -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                }
            }
            is Action.SwitchPreviousApp -> {
                switchPreviousApp(accessibilityService)
            }
            is Action.LaunchApp -> {
                launchApp(action.packageName, action.activityName)
            }
            is Action.RunShell -> {
                runShellCommand(action.command)
            }
        }
    }

    private fun performGlobalAction(service: AccessibilityServiceGesture?, globalActionId: Int) {
        if (service != null) {
            service.performGlobalAction(globalActionId)
        } else {
            Toast.makeText(context, "请先在系统设置中启用 EdgeGesture 无障碍服务", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(packageName: String, activityName: String?) {
        try {
            val pm: PackageManager = context.packageManager
            val intent: Intent? = if (!activityName.isNullOrBlank()) {
                Intent().apply {
                    setClassName(packageName, activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                pm.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            if (intent != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "无法启动应用: $packageName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "启动应用失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchPreviousApp(service: AccessibilityServiceGesture?) {
        val prevPkg = service?.recents?.movePrevious()
        if (prevPkg != null) {
            if (prevPkg == Intent.CATEGORY_HOME) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            } else {
                launchApp(prevPkg, null)
            }
        } else {
            // 回退到多任务键
            performGlobalAction(service, AccessibilityService.GLOBAL_ACTION_RECENTS)
        }
    }

    private fun runShellCommand(command: String) {
        // 后续结合 Shizuku 模块执行
        Toast.makeText(context, "执行指令: $command", Toast.LENGTH_SHORT).show()
    }
}
