package com.apoorvdarshan.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 1. Data Class define kiya jisme Glass Theme ki sabhi properties hain
data class GlassColors(
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceHover: Color,
    val textPrimary: Color,
    val border: Color,
    val borderStrong: Color,
    val shimmer: Color
)

// 2. Dark Mode ke Glass Colors (Solid Background)
val DarkGlassColors = GlassColors(
    bgTop = Color(0xFF1C1C1E), // Solid Dark Gray (ab transparent nahi hai)
    bgBottom = Color(0xFF000000), // Solid Black
    surface = Color(0x26FFFFFF), // Cards ka glassy effect rahega (15% White)
    surfaceHover = Color(0x40FFFFFF), // 25% White
    textPrimary = Color.White,
    border = Color(0x33FFFFFF), // 20% White
    borderStrong = Color(0x66FFFFFF),
    shimmer = Color(0x1AFFFFFF)
)

// 3. Light Mode ke Glass Colors (Solid Background)
val LightGlassColors = GlassColors(
    bgTop = Color(0xFFFFFFFF), // Solid White (ab transparent nahi hai)
    bgBottom = Color(0xFFF5F5F5), // Solid Light Gray
    surface = Color(0x0D000000), // Cards ka glassy effect rahega (5% Black)
    surfaceHover = Color(0x1A000000), // 10% Black
    textPrimary = Color.Black,
    border = Color(0x1A000000), // 10% Black
    borderStrong = Color(0x40000000),
    shimmer = Color(0x0D000000)
)

// 4. CompositionLocal banaya taaki poori app me ise access kar sakein
val LocalGlassTheme = staticCompositionLocalOf { LightGlassColors }

private fun lightColors(themeColor: AppThemeColor) = lightColorScheme(
    primary = themeColor.start,
    onPrimary = AppColors.OnDark,
    secondary = themeColor.start,
    onSecondary = AppColors.OnDark,
    tertiary = themeColor.start,
    onTertiary = AppColors.OnDark,
    background = AppColors.AppBackgroundLight,
    onBackground = AppColors.OnLight,
    surface = AppColors.AppCardLight,
    onSurface = AppColors.OnLight,
    surfaceVariant = AppColors.AppCardLight,
    onSurfaceVariant = AppColors.MutedLight,
    outline = AppColors.DividerLight
)

private fun darkColors(themeColor: AppThemeColor) = darkColorScheme(
    primary = themeColor.start,
    onPrimary = AppColors.OnDark,
    secondary = themeColor.start,
    onSecondary = AppColors.OnDark,
    tertiary = themeColor.start,
    onTertiary = AppColors.OnDark,
    background = AppColors.AppBackgroundDark,
    onBackground = AppColors.OnDark,
    surface = AppColors.AppCardDark,
    onSurface = AppColors.OnDark,
    surfaceVariant = AppColors.AppCardDark,
    onSurfaceVariant = AppColors.MutedDark,
    outline = AppColors.DividerDark
)

@Composable
fun FudAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: AppThemeColor = AppThemeColor.FUD_PINK,
    content: @Composable () -> Unit
) {
    AppColors.setThemeColor(themeColor)
    
    // Standard Material Theme decide ho raha hai
    val colorScheme = if (darkTheme) darkColors(themeColor) else lightColors(themeColor)
    
    // YAHAN FIX HAI: Light / Dark Mode ke hisaab se Glass Theme decide ho raha hai
    val glassColors = if (darkTheme) DarkGlassColors else LightGlassColors

    // CompositionLocalProvider ko add kiya jismein humne MaterialTheme ko wrap kiya hai
    CompositionLocalProvider(LocalGlassTheme provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}