package com.omarea.gesture.ui.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.ui.settings.components.AppPickerDialog
import kotlinx.coroutines.launch

@Composable
fun OtherSettingsScreen(
    configRepository: AppConfigRepository
) {
    val scope = rememberCoroutineScope()
    val otherConfig by configRepository.otherConfig.collectAsState()

    var showBlacklistPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {


        // 2. 游戏优化与防误触
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "游戏防误触",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "横屏全屏时自动缩小手势触控热区，防止全屏游戏中误触发手势动作。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = otherConfig.gameOptimization,
                        onCheckedChange = { scope.launch { configRepository.updateGameOptimization(it) } }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 应用切换黑名单
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "应用切换黑名单",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "已排除 ${otherConfig.appSwitchBlacklist.size} 个应用。手势切换应用时将自动跳过这些应用与系统桌面。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Button(onClick = { showBlacklistPicker = true }) {
                        Text("选择应用")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 系统优化与功耗
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "系统设置与功耗",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 低功耗模式
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("低功耗模式", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "降低无障碍事件监听频率，减少唤醒以节省电量",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = otherConfig.lowPowerMode,
                        onCheckedChange = { scope.launch { configRepository.updateLowPowerMode(it) } }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 窗口监听
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("窗口监听", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "监听当前应用包名与类名变化，用于手势动态适配",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = otherConfig.windowWatch,
                        onCheckedChange = { scope.launch { configRepository.updateWindowWatch(it) } }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 隐藏桌面图标
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("隐藏桌面图标", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "从桌面启动器隐藏 EdgeGesture 图标（可从设置或快捷方式进入）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = otherConfig.hideStartIcon,
                        onCheckedChange = { scope.launch { configRepository.updateHideStartIcon(it) } }
                    )
                }
            }
        }
    }

    if (showBlacklistPicker) {
        AppPickerDialog(
            onDismiss = { showBlacklistPicker = false },
            onAppSelected = { launchApp ->
                val current = otherConfig.appSwitchBlacklist.toMutableSet()
                if (current.contains(launchApp.packageName)) {
                    current.remove(launchApp.packageName)
                } else {
                    current.add(launchApp.packageName)
                }
                scope.launch { configRepository.updateAppBlacklist(current) }
                showBlacklistPicker = false
            }
        )
    }
}
