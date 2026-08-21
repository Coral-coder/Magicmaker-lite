package com.magicmaker.lite.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppColors {
    val Navy = Color(0xFF060D18)
    val Panel = Color(0xFF0C1E36)
    val PanelLine = Color(0xFF1E3A5F)
    val Cyan = Color(0xFF5CE1E6)
    val CyanDim = Color(0xFF2A8FA0)
    val Amber = Color(0xFFFFB81C)
    val TextPrimary = Color(0xFFE8F4FF)
    val TextDim = Color(0xFF7FA3C4)
}

object AppDimens {
    val pad = 12.dp
    val gap = 8.dp
    val control = 48.dp
}

val TechMono = FontFamily.Monospace

private val imagineeringScheme = darkColorScheme(
    primary = AppColors.Cyan,
    onPrimary = AppColors.Navy,
    primaryContainer = AppColors.Panel,
    onPrimaryContainer = AppColors.Cyan,
    secondary = AppColors.Amber,
    onSecondary = AppColors.Navy,
    background = AppColors.Navy,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Navy,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.Panel,
    onSurfaceVariant = AppColors.TextDim,
    outline = AppColors.PanelLine,
    error = Color(0xFFFF6B6B),
)

// Lite runs one screen instead of ten tabs, so the type is a step larger than mobile's —
// same look, easier to read and hit.
private val appTypography = Typography(
    titleMedium = Typography().titleMedium.copy(fontSize = 15.sp, fontFamily = TechMono, letterSpacing = 0.8.sp),
    titleSmall = Typography().titleSmall.copy(fontSize = 13.sp, fontFamily = TechMono, letterSpacing = 0.6.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 14.sp, fontFamily = TechMono),
    bodySmall = Typography().bodySmall.copy(fontSize = 12.sp, fontFamily = TechMono),
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp, fontFamily = TechMono, letterSpacing = 0.5.sp),
    labelMedium = Typography().labelMedium.copy(fontSize = 13.sp, fontFamily = TechMono, letterSpacing = 0.4.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 11.sp, fontFamily = TechMono, letterSpacing = 0.3.sp),
)

@Composable
fun techLabel(): TextStyle = MaterialTheme.typography.labelSmall

@Composable
fun techBody(): TextStyle = MaterialTheme.typography.labelMedium

@Composable
fun techHeader(): TextStyle = MaterialTheme.typography.labelLarge.copy(color = AppColors.Cyan)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = imagineeringScheme, typography = appTypography, content = content)
}
