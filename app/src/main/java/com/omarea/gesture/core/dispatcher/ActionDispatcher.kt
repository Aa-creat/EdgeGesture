package com.omarea.gesture.core.dispatcher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.gesture.AccessibilityServiceGesture
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.model.Action
import com.omarea.gesture.core.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 动作调度分发中枢
 * 负责将手势触发的 Action 路由至无障碍全局操作、应用启动或 Shizuku 特权执行
 */
class ActionDispatcher @JvmOverloads constructor(
    private val context: Context,
    private val configRepository: AppConfigRepository = AppConfigRepository.getInstance(context),
    private val shizukuManager: ShizukuManager = ShizukuManager.getInstance(context)
) {
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

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
        // 如果开启了 Shizuku 模式且已授权，尝试特权启动以绕过后台启动限制
        if (configRepository.whiteBarConfig.value.shizukuEnabled && shizukuManager.isAvailable()) {
            mainScope.launch {
                val result = shizukuManager.launchAppPrivileged(packageName, activityName)
                if (result.isFailure) {
                    // 特权启动失败时回退到标准启动
                    standardLaunchApp(packageName, activityName)
                }
            }
            return
        }

        standardLaunchApp(packageName, activityName)
    }

    private fun standardLaunchApp(packageName: String, activityName: String?) {
        try {
            val pm: PackageManager = context.packageManager
            val intent: Intent? = if (!activityName.isNullOrBlank()) {
                Intent().apply {
                    setClassName(packageName, activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            } else {
                pm.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
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
        if (shizukuManager.isAvailable()) {
            mainScope.launch {
                val result = shizukuManager.executeShell(command)
                result.onSuccess { output ->
                    if (output.isNotBlank()) {
                        Toast.makeText(context, output.take(100), Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(context, "执行失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "需要激活并授权 Shizuku 才能执行特权 Shell 指令", Toast.LENGTH_SHORT).show()
        }
    }
}
