package com.omarea.gesture.ui.settings.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.shizuku.ShizukuManager
import com.omarea.gesture.core.shizuku.ShizukuStatus
import com.omarea.gesture.util.ServiceStateHelper
import com.omarea.gesture.util.ServiceStatus
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun BasicSettingsScreen(
    configRepository: AppConfigRepository,
    shizukuManager: ShizukuManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val basicConfig by configRepository.basicConfig.collectAsState()
    val shizukuStatus by shizukuManager.status.collectAsState()

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var serviceStatus by remember { mutableStateOf(ServiceStateHelper.getServiceStatus(context)) }
    var isIgnoringBattery by remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                serviceStatus = ServiceStateHelper.getServiceStatus(context)
                isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. 无障碍服务总开关与自愈卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = when (serviceStatus) {
                    ServiceStatus.RUNNING -> MaterialTheme.colorScheme.surfaceContainer
                    ServiceStatus.CRASHED -> MaterialTheme.colorScheme.errorContainer
                    ServiceStatus.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (serviceStatus == ServiceStatus.RUNNING) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = when (serviceStatus) {
                            ServiceStatus.RUNNING -> MaterialTheme.colorScheme.primary
                            ServiceStatus.CRASHED -> MaterialTheme.colorScheme.error
                            ServiceStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "无障碍手势服务",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (serviceStatus) {
                                ServiceStatus.RUNNING -> "服务已开启并正常运行（前台服务保活中）"
                                ServiceStatus.CRASHED -> "服务被系统休眠断开（应用服务异常），点击自愈修复"
                                ServiceStatus.DISABLED -> "服务未启用，点击前往系统设置开启"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (serviceStatus) {
                                ServiceStatus.RUNNING -> MaterialTheme.colorScheme.onSurfaceVariant
                                ServiceStatus.CRASHED -> MaterialTheme.colorScheme.onErrorContainer
                                ServiceStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // 若处于 CRASHED 异常断开状态，或有 Shizuku 授权，展示一键自愈修复按钮
                if (serviceStatus != ServiceStatus.RUNNING) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (shizukuStatus is ShizukuStatus.Authorized) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = shizukuManager.restartAccessibilityService()
                                        if (res.isSuccess) {
                                            serviceStatus = ServiceStateHelper.getServiceStatus(context)
                                            isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                                            Toast.makeText(context, "已通过 Shizuku 成功重启手势服务！", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "自愈失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⚡ 一键自愈重启服务")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("前往无障碍设置重新开启")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 后台防杀与电池优化卡片 (针对 ColorOS Athena / 休眠查杀)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isIgnoringBattery) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isIgnoringBattery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "后台防杀与电池优化",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isIgnoringBattery)
                                "已加入电池优化白名单，免疫 ColorOS Athena 待机休眠清理"
                            else
                                "未加入电池白名单，灭屏待机时极易被系统休眠冻结杀掉",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                if (!isIgnoringBattery) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("申请加入电池白名单")
                        }

                        if (shizukuStatus is ShizukuStatus.Authorized) {
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        shizukuManager.grantKeepAlivePermissions()
                                        isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                                        Toast.makeText(context, "已通过特权写入后台防杀白名单", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("⚡ 特权加入")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 悬停时长调节卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "悬停时长",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${basicConfig.hoverTimeMs} ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = basicConfig.hoverTimeMs.toFloat(),
                    onValueChange = { scope.launch { configRepository.updateHoverTime(it.roundToInt()) } },
                    valueRange = 100f..600f,
                    steps = 49
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 震动反馈卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "震动反馈",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("系统默认震感", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "优先使用系统触感引擎（如适用）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = basicConfig.vibratorUseSystem,
                        onCheckedChange = { scope.launch { configRepository.updateVibratorUseSystem(it) } },
                        thumbContent = if (basicConfig.vibratorUseSystem) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                }

                if (!basicConfig.vibratorUseSystem) {
                    Spacer(modifier = Modifier.height(16.dp))
                    // 轻触/点击震动时长
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("轻触/点击时长", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${basicConfig.vibratorTapTimeMs} ms", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = basicConfig.vibratorTapTimeMs.toFloat(),
                        onValueChange = {
                            scope.launch {
                                configRepository.updateVibratorCustom(it.roundToInt(), basicConfig.vibratorHoverTimeMs)
                            }
                        },
                        valueRange = 0f..100f
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    // 悬停/长按震动时长
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("悬停/长按时长", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${basicConfig.vibratorHoverTimeMs} ms", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = basicConfig.vibratorHoverTimeMs.toFloat(),
                        onValueChange = {
                            scope.launch {
                                configRepository.updateVibratorCustom(basicConfig.vibratorTapTimeMs, it.roundToInt())
                            }
                        },
                        valueRange = 0f..100f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. 扩展模式 (Shizuku) 卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "扩展模式 (Shizuku)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = when (shizukuStatus) {
                            is ShizukuStatus.Authorized -> "已授权"
                            is ShizukuStatus.Unauthorized -> "未授权"
                            is ShizukuStatus.NotRunning -> "未运行"
                            is ShizukuStatus.NotInstalled -> "未安装"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (shizukuStatus is ShizukuStatus.Authorized)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "通过 Shizuku 免 Root 获得高级系统权限，支持一键重启自愈无障碍服务、后台防杀配置与极速启动应用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (shizukuStatus !is ShizukuStatus.Authorized) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { shizukuManager.requestPermission() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("申请 Shizuku 授权")
                    }
                }
            }
        }
    }
}
