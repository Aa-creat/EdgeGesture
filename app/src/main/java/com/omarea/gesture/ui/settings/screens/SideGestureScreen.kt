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
import androidx.compose.ui.unit.dp
import com.omarea.gesture.core.config.AppConfigRepository
import com.omarea.gesture.core.model.Action
import com.omarea.gesture.ui.settings.components.ActionSelectDialog
import kotlinx.coroutines.launch

@Composable
fun SideGestureScreen(configRepository: AppConfigRepository) {
    val config by configRepository.sideGestureConfig.collectAsState()
    val scope = rememberCoroutineScope()

    var editingActionType by remember { mutableStateOf<String?>(null) }

    editingActionType?.let { type ->
        val title = when (type) {
            "left_slide" -> "设置左侧内滑动作"
            "left_hover" -> "设置左侧内滑悬停动作"
            "right_slide" -> "设置右侧内滑动作"
            "right_hover" -> "设置右侧内滑悬停动作"
            else -> ""
        }
        val currentAction = when (type) {
            "left_slide" -> config.leftSlideAction
            "left_hover" -> config.leftHoverAction
            "right_slide" -> config.rightSlideAction
            "right_hover" -> config.rightHoverAction
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
        // 左侧手势
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "左侧边缘手势", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "适配 Android 10+ 手势排除区域，防止系统侧滑冲突",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.leftEnabled,
                        onCheckedChange = { scope.launch { configRepository.updateSideLeftEnabled(it) } }
                    )
                }

                if (config.leftEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GestureActionRow("向内滑动", config.leftSlideAction.title) {
                        editingActionType = "left_slide"
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    GestureActionRow("向内滑动并悬停", config.leftHoverAction.title) {
                        editingActionType = "left_hover"
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 右侧手势
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "右侧边缘手势", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "适配 Android 10+ 手势排除区域，防止系统侧滑冲突",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.rightEnabled,
                        onCheckedChange = { scope.launch { configRepository.updateSideRightEnabled(it) } }
                    )
                }

                if (config.rightEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GestureActionRow("向内滑动", config.rightSlideAction.title) {
                        editingActionType = "right_slide"
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    GestureActionRow("向内滑动并悬停", config.rightHoverAction.title) {
                        editingActionType = "right_hover"
                    }
                }
            }
        }
    }
}
