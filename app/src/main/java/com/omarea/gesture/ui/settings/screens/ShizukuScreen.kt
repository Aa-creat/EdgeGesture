package com.omarea.gesture.ui.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.shizuku.ShizukuManager
import com.omarea.gesture.core.shizuku.ShizukuStatus
import kotlinx.coroutines.launch

@Composable
fun ShizukuScreen(
    configRepository: AppConfigRepository,
    shizukuManager: ShizukuManager
) {
    val status by shizukuManager.status.collectAsState()
    val config by configRepository.whiteBarConfig.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. Shizuku 状态卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "Shizuku 运行状态", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val (statusText, statusColor) = when (status) {
                    is ShizukuStatus.Authorized -> "已授权且正在运行 (正常)" to MaterialTheme.colorScheme.primary
                    is ShizukuStatus.Unauthorized -> "已运行，等待用户授权" to MaterialTheme.colorScheme.tertiary
                    is ShizukuStatus.NotRunning -> "未运行 (请在 Shizuku 应用中启动服务)" to MaterialTheme.colorScheme.error
                    is ShizukuStatus.NotInstalled -> "未安装 Shizuku" to MaterialTheme.colorScheme.outline
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = statusColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (status is ShizukuStatus.Unauthorized) {
                    Button(
                        onClick = { shizukuManager.requestPermission() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("申请 Shizuku 权限")
                    }
                } else {
                    Button(
                        onClick = { shizukuManager.checkStatus() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("刷新状态")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 特权加速设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "启用 Shizuku 特权加速", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "通过特权 Binder 启动应用，彻底规避 Android 对后台启动 Activity 的 5 秒限制",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.shizukuEnabled,
                    enabled = (status is ShizukuStatus.Authorized),
                    onCheckedChange = { scope.launch { configRepository.updateShizukuEnabled(it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "关于 Shizuku 增强模式", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Shizuku 模式完全是可选的。在未开启或未授权时，EdgeGesture 仍然 100% 正常工作于系统无障碍服务之下。\n" +
                            "• 开启 Shizuku 特权加速后，可消除极少数特定系统下按 Home 后后台打开 App 的冷却延迟，并支持直接执行 Shell 特权脚本。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
