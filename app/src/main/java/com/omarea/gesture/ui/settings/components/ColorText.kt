package com.omarea.gesture.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 十六进制色号灵活解析器
 * 支持：
 * - 3 位: #RGB -> #RRGGBB (如 #FFF -> 白色)
 * - 4 位: #RGBA (如 #0001 -> R=0, G=0, B=0, A=0.067f，若纯RGB扩展则为微深色)
 * - 6 位: #RRGGBB (如 #138ED6, #ffffff)
 * - 8 位: #AARRGGBB
 */
fun parseColorHex(hex: String): Color? {
    val clean = hex.trim().removePrefix("#").trim()
    return try {
        when (clean.length) {
            3 -> {
                val r = clean.substring(0, 1).repeat(2).toInt(16)
                val g = clean.substring(1, 2).repeat(2).toInt(16)
                val b = clean.substring(2, 3).repeat(2).toInt(16)
                Color(r, g, b)
            }
            4 -> {
                // 支持 #RGBA 格式，同时保证即使 A 极低在文字展示时也呈现其色相
                val r = clean.substring(0, 1).repeat(2).toInt(16)
                val g = clean.substring(1, 2).repeat(2).toInt(16)
                val b = clean.substring(2, 3).repeat(2).toInt(16)
                val a = clean.substring(3, 4).repeat(2).toInt(16)
                Color(r, g, b, (a.coerceAtLeast(60))) // 最低保留一定不透明度以使文字可见
            }
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * 色号带颜色展示组件（严格使用文字字体呈现颜色，不使用额外色块）
 * 例如 “#ffffff” 为纯白色字体，“#138ED6” 为科技蓝字体。
 * 自带防看不清的微弱微底纹与圆角，保证在深色/浅色卡片背景下任何色号字体（如纯白、纯黑）均清晰可读。
 */
@Composable
fun ColorHexText(
    hexText: String,
    modifier: Modifier = Modifier,
    overrideColor: Color? = null,
    fontSize: TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val displayColor = overrideColor ?: parseColorHex(hexText) ?: MaterialTheme.colorScheme.primary

    // 根据字体颜色的明度，动态选择高对比度的微底色
    // 浅色字体配深色半透明底，深色字体配浅色半透明底，彻底杜绝 #ffffff 白色字体在浅色卡片看不清
    val luminance = 0.299f * displayColor.red + 0.587f * displayColor.green + 0.114f * displayColor.blue
    val pillBgColor = if (luminance > 0.6f) {
        Color(0xCC1A1A1A) // 深色微底，衬托白色/浅亮色字体
    } else {
        Color(0x33E0E0E0) // 浅色微底，衬托深色字体
    }
    val pillBorderColor = if (luminance > 0.6f) {
        Color(0x44FFFFFF)
    } else {
        Color(0x22000000)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(pillBgColor)
            .border(0.8.dp, pillBorderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (hexText.startsWith("#")) hexText else "#$hexText",
            color = displayColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = FontFamily.Monospace
        )
    }
}
