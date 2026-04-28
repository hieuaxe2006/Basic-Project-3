package com.socialapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ===== Design System Colors =====
// Primary
val Primary = Color(0xFF1877F2)
val PrimaryLight = Color(0xFFE7F3FF)
val PrimaryDark = Color(0xFF0F5BD3)

// Semantic
val Secondary = Color(0xFF42B72A)
val Danger = Color(0xFFE41E3F)
val Warning = Color(0xFFF7B928)
val Success = Color(0xFF31A24C)

// Surface & Background
val Background = Color(0xFFF0F2F5)
val CardBackground = Color(0xFFFFFFFF)
val BorderColor = Color(0xFFDADDE1)

// Text
val TextPrimary = Color(0xFF050505)
val TextSecondary = Color(0xFF606770)

// Dark theme equivalents
val DarkBackground = Color(0xFF18191A)
val DarkCard = Color(0xFF242526)
val DarkBorder = Color(0xFF3E4042)
val DarkTextPrimary = Color(0xFFE4E6EB)
val DarkTextSecondary = Color(0xFFB0B3B8)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    background = Background,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = Color.White,
    outline = BorderColor,
    outlineVariant = BorderColor,
    surfaceVariant = Color(0xFFE4E6EB)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4599FF),
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = Color(0xFF5BD348),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B3A1B),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCard,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    error = Color(0xFFFF6B7A),
    onError = Color.White,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    surfaceVariant = Color(0xFF3A3B3C)
)

// ===== Design System Typography =====
val AppTypography = Typography(
    // Title / App name: 18-20px, bold
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default, // Roboto on Android
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // Message text: 14px
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // Sidebar labels: 13px
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun SocialAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) DarkCard.toArgb() else CardBackground.toArgb()
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
