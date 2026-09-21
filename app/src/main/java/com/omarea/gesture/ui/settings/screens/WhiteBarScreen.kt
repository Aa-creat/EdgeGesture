package com.omarea.gesture.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.model.Action
import com.omarea.gesture.ui.settings.components.ActionSelectDialog
import com.omarea.gesture.ui.settings.components.ColorHexText
import com.omarea.gesture.ui.settings.components.ColorPickerDialog
import kotlinx.coroutines.launch

@Composable
fun WhiteBarScreen(configRepository: AppConfigRepository) {
    val scope = rememberCoroutineScope()
    val config by configRepository.whiteBarConfig.collectAsState()

    var editingActionType by remember { mutableStateOf<String?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            title = "自定义小白条颜色",
            initialColor = config.color.toInt(),
            initialAlpha = config.alpha,
            onDismiss = { showColorPicker = false },
            onColorSelected = { c, a ->
                scope.launch {
                    configRepository.updateWhiteBarAppearance(c.toLong(), a)
                }
            }
        )
    }

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
        // 1. 总开关与横竖屏卡片
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
                        text = "启用底部小白条",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { scope.launch { configRepository.updateWhiteBarEnabled(it) } },
                        thumbContent = if (config.enabled) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                }

                if (config.enabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("横屏", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = config.landscapeEnabled,
                            onCheckedChange = { scope.launch { configRepository.updateWhiteBarOrientation(it, config.portraitEnabled) } }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("竖屏", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = config.portraitEnabled,
                            onCheckedChange = { scope.launch { configRepository.updateWhiteBarOrientation(config.landscapeEnabled, it) } }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 尺寸与热区调节卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "外观尺寸与触控热区", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // 尺寸与热区调节滑块
                Text("小白条宽度: ${config.widthDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.widthDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(it, config.heightDp, config.touchHeightDp, config.touchWidthDp, config.bottomMarginDp, config.radiusDp)
                        }
                    },
                    valueRange = 60f..300f
                )

                Text("触控热区宽度: ${config.touchWidthDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.touchWidthDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(config.widthDp, config.heightDp, config.touchHeightDp, it, config.bottomMarginDp, config.radiusDp)
                        }
                    },
                    valueRange = 60f..400f
                )

                Text("触控热区高度: ${config.touchHeightDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.touchHeightDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(config.widthDp, config.heightDp, it, config.touchWidthDp, config.bottomMarginDp, config.radiusDp)
                        }
                    },
                    valueRange = 8f..80f
                )

                Text("线条高度: ${config.heightDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.heightDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(config.widthDp, it, config.touchHeightDp, config.touchWidthDp, config.bottomMarginDp, config.radiusDp)
                        }
                    },
                    valueRange = 2f..20f
                )

                Text("底部边距: ${config.bottomMarginDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.bottomMarginDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(config.widthDp, config.heightDp, config.touchHeightDp, config.touchWidthDp, it, config.radiusDp)
                        }
                    },
                    valueRange = 0f..60f
                )

                Text("圆角半径: ${config.radiusDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.radiusDp,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarDimensions(config.widthDp, config.heightDp, config.touchHeightDp, config.touchWidthDp, config.bottomMarginDp, it)
                        }
                    },
                    valueRange = 0f..20f
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "防止烧屏 (微位移)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "定期微调位置防烧屏",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.burnInProtection,
                        onCheckedChange = { scope.launch { configRepository.updateWhiteBarBurnInProtection(it) } },
                        thumbContent = if (config.burnInProtection) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 颜色与电量指示卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "外观色彩与电量", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "跟随电量指示", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "充当电量指示条",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.batteryLevelEnabled,
                        onCheckedChange = { scope.launch { configRepository.updateWhiteBarBatteryLevel(it) } },
                        thumbContent = if (config.batteryLevelEnabled) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                        } else null
                    )
                }

                if (config.batteryLevelEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "现代平滑动态渐变", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (config.batterySmoothGradient) "平滑色彩渐变" else "7 档色阶",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.batterySmoothGradient,
                            onCheckedChange = { scope.launch { configRepository.updateWhiteBarBatterySmoothGradient(it) } },
                            thumbContent = if (config.batterySmoothGradient) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 色彩与调色盘入口卡片式布局
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showColorPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 模拟小白条胶囊预览（展示实际颜色与透明度）
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(18.dp)
                            .clip(CircleShape)
                            .background(Color(config.color).copy(alpha = config.alpha))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "小白条色彩",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "色号: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ColorHexText(hexText = String.format("#%06X", (0xFFFFFF and config.color.toInt())))
                        }
                    }
                    FilledTonalButton(
                        onClick = { showColorPicker = true },
                        shape = CircleShape
                    ) {
                        Text("调色盘")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 不透明度独立调节滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "不透明度",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(config.alpha * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = config.alpha,
                    onValueChange = {
                        scope.launch {
                            configRepository.updateWhiteBarAppearance(config.color, it)
                        }
                    },
                    valueRange = 0f..1f
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 核心手势动作配置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "手势动作映射", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

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
