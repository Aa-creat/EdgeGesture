package com.omarea.gesture.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * 现代 Material 3 专业调色盘弹窗
 * 具备 RGB 滑块微调、HEX 十六进制输入、常用预设调色板与透明度控制，杜绝界面截断
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Int,
    initialAlpha: Float = 1.0f,
    onDismiss: () -> Unit,
    onColorSelected: (colorArgb: Int, alpha: Float) -> Unit
) {
    val initialC = Color(initialColor)
    var red by remember { mutableFloatStateOf(initialC.red * 255f) }
    var green by remember { mutableFloatStateOf(initialC.green * 255f) }
    var blue by remember { mutableFloatStateOf(initialC.blue * 255f) }
    var alpha by remember { mutableFloatStateOf(initialAlpha.coerceIn(0f, 1f)) }

    fun currentColor(): Color {
        return Color(
            red = (red / 255f).coerceIn(0f, 1f),
            green = (green / 255f).coerceIn(0f, 1f),
            blue = (blue / 255f).coerceIn(0f, 1f),
            alpha = alpha
        )
    }

    var hexText by remember {
        mutableStateOf(String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt()))
    }

    val presetColors = remember {
        listOf(
            0xFFFFFFFF.toInt() to "纯白",
            0xFF222222.toInt() to "深黑",
            0xFF888888.toInt() to "素灰",
            0xFF138ED6.toInt() to "科技蓝",
            0xFF00B9C2.toInt() to "青蓝",
            0xFF00D5D9.toInt() to "湖青",
            0xFF02D98D.toInt() to "翠绿",
            0xFF87CB00.toInt() to "柠檬黄",
            0xFFFC8A1B.toInt() to "暖橙",
            0xFFF9592F.toInt() to "赤红",
            0xFF9C27B0.toInt() to "雅紫",
            0xFF673AB7.toInt() to "深紫"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 色彩预览框
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentColor())
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "当前色号: ",
                                style = MaterialTheme.typography.titleMedium
                            )
                            ColorHexText(
                                hexText = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt()),
                                overrideColor = currentColor()
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "透明度: ${(alpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 色号输入框（支持 #ffffff, #138ED6, #0001 等，直接显示彩色字体）
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { newHex ->
                        hexText = newHex
                        val parsed = parseColorHex(newHex)
                        if (parsed != null) {
                            red = parsed.red * 255f
                            green = parsed.green * 255f
                            blue = parsed.blue * 255f
                            val cleanLen = newHex.trim().removePrefix("#").length
                            if (cleanLen == 4 || cleanLen == 8) {
                                alpha = parsed.alpha
                            }
                        }
                    },
                    label = { Text("输入色号 (如 #ffffff, #138ED6, #0001)") },
                    trailingIcon = {
                        ColorHexText(
                            hexText = hexText,
                            overrideColor = currentColor()
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 常用色彩预设矩阵（采用 FlowRow 自适应网格，绝不截断）
                Text(text = "预设色板", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { (colorVal, _) ->
                        val c = Color(colorVal)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (red.toInt() == (c.red * 255).toInt() &&
                                        green.toInt() == (c.green * 255).toInt() &&
                                        blue.toInt() == (c.blue * 255).toInt()
                                    ) 2.5.dp else 1.dp,
                                    color = if (red.toInt() == (c.red * 255).toInt() &&
                                        green.toInt() == (c.green * 255).toInt() &&
                                        blue.toInt() == (c.blue * 255).toInt()
                                    ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable {
                                    red = c.red * 255f
                                    green = c.green * 255f
                                    blue = c.blue * 255f
                                    hexText = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RGB 精细调节滑块
                Text(text = "红 (R): ${red.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = red,
                    onValueChange = {
                        red = it
                        hexText = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
                    },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                )

                Text(text = "绿 (G): ${green.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = green,
                    onValueChange = {
                        green = it
                        hexText = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
                    },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green)
                )

                Text(text = "蓝 (B): ${blue.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = blue,
                    onValueChange = {
                        blue = it
                        hexText = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
                    },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue)
                )

                Text(text = "不透明度: ${(alpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it },
                    valueRange = 0f..1.0f
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalColor = Color(
                        red = (red / 255f).coerceIn(0f, 1f),
                        green = (green / 255f).coerceIn(0f, 1f),
                        blue = (blue / 255f).coerceIn(0f, 1f),
                        alpha = 1.0f
                    ).toArgb()
                    onColorSelected(finalColor, alpha)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
