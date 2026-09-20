package com.omarea.gesture.core.model

import android.accessibilityservice.AccessibilityService
import android.os.Build

/**
 * 统一手势动作模型
 */
sealed class Action(val id: String, val title: String) {
    object None : Action("none", "无动作")
    object Back : Action("back", "返回键")
    object Home : Action("home", "回到桌面")
    object Recents : Action("recents", "最近任务")
    object Notifications : Action("notifications", "下拉通知栏")
    object QuickSettings : Action("quick_settings", "快捷控制中心")
    object PowerDialog : Action("power_dialog", "电源菜单")
    object LockScreen : Action("lock_screen", "锁屏")
    object Screenshot : Action("screenshot", "截屏")
    object SplitScreen : Action("split_screen", "分屏")
    object SwitchPreviousApp : Action("switch_prev_app", "切换上一个应用")

    data class LaunchApp(
        val packageName: String,
        val appName: String,
        val activityName: String? = null
    ) : Action("launch_app:$packageName", "打开应用: $appName")

    data class RunShell(
        val command: String
    ) : Action("shell:$command", "执行脚本: $command")

    companion object {
        fun getAllPresetActions(): List<Action> {
            val list = mutableListOf(
                None,
                Back,
                Home,
                Recents,
                Notifications,
                QuickSettings,
                PowerDialog,
                SwitchPreviousApp
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                list.add(SplitScreen)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                list.add(LockScreen)
                list.add(Screenshot)
            }
            return list
        }

        fun fromString(encoded: String): Action {
            if (encoded.isBlank() || encoded == "none") return None
            return when {
                encoded == "back" -> Back
                encoded == "home" -> Home
                encoded == "recents" -> Recents
                encoded == "notifications" -> Notifications
                encoded == "quick_settings" -> QuickSettings
                encoded == "power_dialog" -> PowerDialog
                encoded == "lock_screen" -> LockScreen
                encoded == "screenshot" -> Screenshot
                encoded == "split_screen" -> SplitScreen
                encoded == "switch_prev_app" -> SwitchPreviousApp
                encoded.startsWith("launch_app:") -> {
                    val parts = encoded.removePrefix("launch_app:").split("|")
                    val pkg = parts.getOrNull(0) ?: ""
                    val name = parts.getOrNull(1) ?: pkg
                    val activity = parts.getOrNull(2)
                    LaunchApp(pkg, name, activity)
                }
                encoded.startsWith("shell:") -> {
                    val cmd = encoded.removePrefix("shell:")
                    RunShell(cmd)
                }
                else -> None
            }
        }

        fun toString(action: Action): String {
            return when (action) {
                is None -> "none"
                is Back -> "back"
                is Home -> "home"
                is Recents -> "recents"
                is Notifications -> "notifications"
                is QuickSettings -> "quick_settings"
                is PowerDialog -> "power_dialog"
                is LockScreen -> "lock_screen"
                is Screenshot -> "screenshot"
                is SplitScreen -> "split_screen"
                is SwitchPreviousApp -> "switch_prev_app"
                is LaunchApp -> "launch_app:${action.packageName}|${action.appName}|${action.activityName.orEmpty()}"
                is RunShell -> "shell:${action.command}"
            }
        }
    }
}
