package com.omarea.gesture.ui.settings.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// MD3 Expressive Color Tokens (Cyan / Teal / Indigo Expressive Palette matching app icon)
private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1),          // Cyan 300
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF8CF4FF),
    secondary = Color(0xFF80CBC4),        // Teal 200
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF1E4E49),
    onSecondaryContainer = Color(0xFFA7F2E9),
    tertiary = Color(0xFF81D4FA),         // Light Blue 200
    onTertiary = Color(0xFF003548),
    tertiaryContainer = Color(0xFF004D67),
    onTertiaryContainer = Color(0xFFC3E8FF),
    background = Color(0xFF111415),
    onBackground = Color(0xFFE1E3E3),
    surface = Color(0xFF111415),
    onSurface = Color(0xFFE1E3E3),
    surfaceVariant = Color(0xFF1F2426),
    onSurfaceVariant = Color(0xFFC0C8C8),
    surfaceContainerLowest = Color(0xFF0C0F10),
    surfaceContainerLow = Color(0xFF191C1D),
    surfaceContainer = Color(0xFF1D2021),
    surfaceContainerHigh = Color(0xFF272A2B),
    surfaceContainerHighest = Color(0xFF323536),
    outline = Color(0xFF899393),
    outlineVariant = Color(0xFF3F484A)
)

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),          // Cyan 700
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF006A62),        // Teal 700
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF70F7E9),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFF006686),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FF),
    onTertiaryContainer = Color(0xFF001E2B),
    background = Color(0xFFFBFDFA),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFFBFDFA),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDBE5E4),
    onSurfaceVariant = Color(0xFF3F484A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3F4),
    surfaceContainer = Color(0xFFEFEFF0),
    surfaceContainerHigh = Color(0xFFE9E9EA),
    surfaceContainerHighest = Color(0xFFE3E4E4),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBFC9C8)
)

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),       // MD3E 24dp for Cards & Containers
    extraLarge = RoundedCornerShape(28.dp)   // MD3E 28dp for Dialogs
)

@Composable
fun EdgeGestureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        content = content
    )
}
