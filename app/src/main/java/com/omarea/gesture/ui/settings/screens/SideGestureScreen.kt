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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
fun SideGestureScreen(configRepository: AppConfigRepository) {
    val config by configRepository.sideGestureConfig.collectAsState()
    val scope = rememberCoroutineScope()

    var editingActionType by remember { mutableStateOf<String?>(null) }

    editingActionType?.let { type ->
        val title = when (type) {
            "left_slide" -> "设置左侧向右滑动动作"
            "left_hover" -> "设置左侧右滑悬停动作"
            "right_slide" -> "设置右侧向左滑动动作"
            "right_hover" -> "设置右侧左滑悬停动作"
            "bottom_slide" -> "设置底部向上滑动动作"
            "bottom_hover" -> "设置底部上滑悬停动作"
            else -> ""
        }
        val currentAction = when (type) {
            "left_slide" -> config.leftSlideAction
            "left_hover" -> config.leftHoverAction
            "right_slide" -> config.rightSlideAction
            "right_hover" -> config.rightHoverAction
            "bottom_slide" -> config.bottomSlideAction
            "bottom_hover" -> config.bottomHoverAction
            else -> Action.None
        }

        ActionSelectDialog(
            title = title,
            currentAction = currentAction,
            onDismiss = { editingActionType = null },
            onActionSelected = { action ->
                scope.launch {
                    when (type) {
                        "left_slide" -> configRepository.updateSideLeftActions(action, config.leftHoverAction)
                        "left_hover" -> configRepository.updateSideLeftActions(config.leftSlideAction, action)
                        "right_slide" -> configRepository.updateSideRightActions(action, config.rightHoverAction)
                        "right_hover" -> configRepository.updateSideRightActions(config.rightSlideAction, action)
                        "bottom_slide" -> configRepository.updateSideBottomActions(action, config.bottomHoverAction)
                        "bottom_hover" -> configRepository.updateSideBottomActions(config.bottomSlideAction, action)
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
        // 1. 底部边缘手势卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "底部边缘手势", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("横屏", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = config.bottomLandscape,
                            onCheckedChange = { scope.launch { configRepository.updateSideBottomOrientation(it, config.bottomPortrait) } }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("竖屏", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = config.bottomPortrait,
                            onCheckedChange = { scope.launch { configRepository.updateSideBottomOrientation(config.bottomLandscape, it) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("底部热区宽度: ${(config.bottomWidthPercent * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.bottomWidthPercent,
                    onValueChange = { scope.launch { configRepository.updateSideBottomDimensions(config.bottomHeightDp, it) } },
                    valueRange = 0.1f..1.0f
                )

                Text("底部热区高度: ${config.bottomHeightDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.bottomHeightDp,
                    onValueChange = { scope.launch { configRepository.updateSideBottomDimensions(it, config.bottomWidthPercent) } },
                    valueRange = 4f..40f
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                GestureActionRow("向上滑动", config.bottomSlideAction.title) {
                    editingActionType = "bottom_slide"
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                GestureActionRow("上滑悬停", config.bottomHoverAction.title) {
                    editingActionType = "bottom_hover"
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 左侧边缘手势卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "左侧边缘手势", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("横屏", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = config.leftLandscape,
                            onCheckedChange = { scope.launch { configRepository.updateSideLeftOrientation(it, config.leftPortrait) } }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("竖屏", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = config.leftPortrait,
                            onCheckedChange = { scope.launch { configRepository.updateSideLeftOrientation(config.leftLandscape, it) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("热区宽度: ${config.leftWidthDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.leftWidthDp,
                    onValueChange = { scope.launch { configRepository.updateSideLeftDimensions(it, config.leftHeightPercent, config.leftYOffsetPercent) } },
                    valueRange = 6f..40f
                )

                Text("热区高度: ${(config.leftHeightPercent * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.leftHeightPercent,
                    onValueChange = { scope.launch { configRepository.updateSideLeftDimensions(config.leftWidthDp, it, config.leftYOffsetPercent) } },
                    valueRange = 0.1f..1.0f
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                GestureActionRow("向右滑动", config.leftSlideAction.title) {
                    editingActionType = "left_slide"
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                GestureActionRow("右滑悬停", config.leftHoverAction.title) {
                    editingActionType = "left_hover"
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 右侧边缘手势卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "右侧边缘手势", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("横屏", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = config.rightLandscape,
                            onCheckedChange = { scope.launch { configRepository.updateSideRightOrientation(it, config.rightPortrait) } }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("竖屏", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = config.rightPortrait,
                            onCheckedChange = { scope.launch { configRepository.updateSideRightOrientation(config.rightLandscape, it) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("热区宽度: ${config.rightWidthDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.rightWidthDp,
                    onValueChange = { scope.launch { configRepository.updateSideRightDimensions(it, config.rightHeightPercent, config.rightYOffsetPercent) } },
                    valueRange = 6f..40f
                )

                Text("热区高度: ${(config.rightHeightPercent * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = config.rightHeightPercent,
                    onValueChange = { scope.launch { configRepository.updateSideRightDimensions(config.rightWidthDp, it, config.rightYOffsetPercent) } },
                    valueRange = 0.1f..1.0f
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                GestureActionRow("向左滑动", config.rightSlideAction.title) {
                    editingActionType = "right_slide"
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                GestureActionRow("左滑悬停", config.rightHoverAction.title) {
                    editingActionType = "right_hover"
                }
            }
        }
    }
}
