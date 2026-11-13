package com.omarea.gesture.core.shizuku

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class ShizukuStatus {
    object NotInstalled : ShizukuStatus()
    object NotRunning : ShizukuStatus()
    object Unauthorized : ShizukuStatus()
    object Authorized : ShizukuStatus()
}

/**
 * 现代 Shizuku 特权管理器
 * 基于官方 Binder IPC 实现，支持非 Root/无线调试权限调用，具备优雅降级与状态监听
 */
class ShizukuManager private constructor(private val context: Context) {

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE_PERMISSION = 1001

        @Volatile
        private var INSTANCE: ShizukuManager? = null

        fun getInstance(context: Context): ShizukuManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShizukuManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _status = MutableStateFlow<ShizukuStatus>(ShizukuStatus.NotRunning)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _status.value = ShizukuStatus.NotRunning
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                _status.value = ShizukuStatus.Authorized
            } else {
                _status.value = ShizukuStatus.Unauthorized
            }
        }
    }

    init {
        registerListeners()
        checkStatus()
    }

    private fun registerListeners() {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        } catch (_: Exception) {
        }
    }

    fun checkStatus() {
        if (!isShizukuInstalled()) {
            _status.value = ShizukuStatus.NotInstalled
            return
        }

        if (!Shizuku.pingBinder()) {
            _status.value = ShizukuStatus.NotRunning
            return
        }

        if (Shizuku.isPreV11()) {
            _status.value = ShizukuStatus.Unauthorized
            return
        }

        val isGranted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }

        _status.value = if (isGranted) ShizukuStatus.Authorized else ShizukuStatus.Unauthorized
    }

    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isAvailable(): Boolean {
        return _status.value is ShizukuStatus.Authorized
    }

    fun requestPermission() {
        if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 在后台通过 Shizuku 执行特权 Shell 命令
     */
    suspend fun executeShell(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            return@withContext Result.failure(IllegalStateException("Shizuku 未授权或未运行"))
        }

        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            Result.success(output.toString().trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 绕过后台启动限制启动应用 (am start)
     */
    suspend fun launchAppPrivileged(packageName: String, activityName: String? = null): Result<Unit> {
        val target = if (!activityName.isNullOrBlank()) {
            "$packageName/$activityName"
        } else {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName) ?: return Result.failure(IllegalArgumentException("无法找到目标应用入口"))
            val comp = intent.component ?: return Result.failure(IllegalArgumentException("无法解析 Component"))
            "${comp.packageName}/${comp.className}"
        }

        val cmd = "am start -n $target --activity-clear-top"
        return executeShell(cmd).map { }
    }

    /**
     * 通过 Shizuku 彻底自愈并重新绑定无障碍服务（清除 mCrashed 异常状态）
     */
    suspend fun restartAccessibilityService(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            return@withContext Result.failure(IllegalStateException("Shizuku 未授权或未运行"))
        }

        try {
            val serviceComp = "${context.packageName}/com.omarea.gesture.ModernGestureService"
            val getCmd = "settings get secure enabled_accessibility_services"
            val current = executeShell(getCmd).getOrNull() ?: ""
            val cleaned = current.split(":")
                .filter { it.isNotBlank() && !it.contains(context.packageName) }
                .joinToString(":")

            // 1. 先临时移除本服务，触发系统 AMS 清除 mCrashed 崩溃标记
            executeShell("settings put secure enabled_accessibility_services \"$cleaned\"")
            kotlinx.coroutines.delay(200)

            // 2. 重新加入并启用无障碍
            val restored = if (cleaned.isBlank()) serviceComp else "$cleaned:$serviceComp"
            executeShell("settings put secure enabled_accessibility_services \"$restored\"")
            executeShell("settings put secure accessibility_enabled 1")

            // 3. 同时一键配置电池优化白名单与 ColorOS 后台权限
            grantKeepAlivePermissions()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 通过 Shizuku 授予 ColorOS / AOSP 电池白名单与后台免杀权限
     */
    suspend fun grantKeepAlivePermissions(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            return@withContext Result.failure(IllegalStateException("Shizuku 未授权或未运行"))
        }

        try {
            val pkg = context.packageName
            executeShell("dumpsys deviceidle whitelist +$pkg")
            executeShell("cmd appops set $pkg RUN_IN_BACKGROUND allow")
            executeShell("cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow")
            executeShell("cmd appops set $pkg ACCESS_RESTRICTED_SETTINGS allow")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (_: Exception) {
        }
    }
}
