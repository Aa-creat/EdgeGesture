package com.omarea.gesture.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.model.Action
import com.omarea.gesture.ui.settings.components.ActionSelectDialog
import kotlinx.coroutines.launch

@Composable
fun WhiteBarScreen(configRepository: AppConfigRepository) {
    val config by configRepository.whiteBarConfig.collectAsState()
    val scope = rememberCoroutineScope()

    var editingActionType by remember { mutableStateOf<String?>(null) }

    editingActionType?.let { type ->
        val title = when (type) {
            "click" -> "设置小白条单击动作"
            "long_press" -> "设置小白条长按动作"
            "swipe_left" -> "设置左滑动动作"
            "swipe_right" -> "设置右滑动动作"
            "swipe_up" -> "设置上滑动动作"
            "swipe_up_hold" -> "设置上滑停顿动作"
            else -> ""
        }
        val currentAction = when (type) {
            "click" -> config.clickAction
            "long_press" -> config.longPressAction
            "swipe_left" -> config.swipeLeftAction
            "swipe_right" -> config.swipeRightAction
            "swipe_up" -> config.swipeUpAction
            "swipe_up_hold" -> config.swipeUpHoldAction
            else -> Action.None
        }

        ActionSelectDialog(
            title = title,
            currentAction = currentAction,
            onDismiss = { editingActionType = null },
            onActionSelected = { action ->
                scope.launch {
                    when (type) {
                        "click" -> configRepository.updateClickAction(action)
                        "long_press" -> configRepository.updateLongPressAction(action)
                        "swipe_left" -> configRepository.updateSwipeLeftAction(action)
                        "swipe_right" -> configRepository.updateSwipeRightAction(action)
                        "swipe_up" -> configRepository.updateSwipeUpAction(action)
                        "swipe_up_hold" -> configRepository.updateSwipeUpHoldAction(action)
                    }
                }
                editingActionType = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. 总开关卡片
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
                    Text(text = "启用底部小白条", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "类似 iOS 风格的底部轻量级手势导航条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { scope.launch { configRepository.updateWhiteBarEnabled(it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 实时外观预览卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "实时外观预览", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .width(config.widthDp.dp)
                        .height(config.heightDp.dp)
                        .clip(RoundedCornerShape(config.radiusDp.dp))
                        .background(Color(config.color).copy(alpha = config.alpha))
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 尺寸调节滑块
                Text("宽度: ${config.widthDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.widthDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(it, config.heightDp, config.bottomMarginDp, config.radiusDp)
                        }
                    },
                    valueRange = 60f..260f
                )

                Text("高度: ${config.heightDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.heightDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(config.widthDp, it, config.bottomMarginDp, config.radiusDp)
                        }
                    },
                    valueRange = 2f..10f
                )

                Text("底部边距: ${config.bottomMarginDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.bottomMarginDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(config.widthDp, config.heightDp, it, config.radiusDp)
                        }
                    },
                    valueRange = 0f..24f
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 核心手势动作配置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "手势动作映射", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "支持点击直接绑定打开常用应用（如微信、相机等）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                GestureActionRow("单击 (Click)", config.clickAction.title) {
                    editingActionType = "click"
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                GestureActionRow("长按 (Long Press)", config.longPressAction.title) {
                    editingActionType = "long_press"
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                GestureActionRow("向左滑动", config.swipeLeftAction.title) {
                    editingActionType = "swipe_left"
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                GestureActionRow("向右滑动", config.swipeRightAction.title) {
                    editingActionType = "swipe_right"
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                GestureActionRow("向上滑动", config.swipeUpAction.title) {
                    editingActionType = "swipe_up"
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                GestureActionRow("上滑停顿 (Hover)", config.swipeUpHoldAction.title) {
                    editingActionType = "swipe_up_hold"
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 触觉反馈开关
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
                    Text(text = "线性马达触感反馈", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "针对 Android 10+ 线性马达提供清脆单击与长按顿挫反馈",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.hapticsEnabled,
                    onCheckedChange = { scope.launch { configRepository.updateHapticsEnabled(it) } }
                )
            }
        }
    }
}

@Composable
fun GestureActionRow(label: String, actionTitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = actionTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
