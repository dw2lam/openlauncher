package com.openlauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val LocalDayMode = staticCompositionLocalOf { false }

@Composable
fun OpenLauncherTheme(
    accent: Color     = AccentOrange,
    background: Color = Black,
    textColor: Color  = Color.White,
    fontBold: Boolean = false,
    textScale: Float  = 1.0f,
    isDayMode: Boolean = false,
    useCustomBg: Boolean = false,
    content: @Composable () -> Unit
) {
    // Contrast-aware: the accent is user-chosen and can be any brightness,
    // so a fixed onPrimary (white) goes invisible on light accents
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val colorScheme = if (isDayMode) lightColorScheme(
        primary          = accent,
        onPrimary        = onAccent,
        secondary        = accent.copy(alpha = 0.7f),
        onSecondary      = onAccent,
        tertiary         = accent.copy(alpha = 0.5f),
        background       = if (useCustomBg) background else Color(0xFFEEEEEE),
        surface          = Color(0xFFFFFFFF),
        onBackground     = Color(0xFF111111),
        onSurface        = Color(0xFF111111),
        surfaceVariant   = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFF666666),
        outline          = Color(0xFFCCCCCC)
    ) else darkColorScheme(
        primary          = accent,
        onPrimary        = onAccent,
        secondary        = accent.copy(alpha = 0.7f),
        onSecondary      = onAccent,
        tertiary         = accent.copy(alpha = 0.5f),
        background       = background,
        surface          = CardSurface,
        onBackground     = textColor,
        onSurface        = textColor,
        surfaceVariant   = DimSurface,
        onSurfaceVariant = TextMuted,
        outline          = DividerGray
    )
    CompositionLocalProvider(LocalDayMode provides isDayMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = launcherTypography(fontBold, textScale),
            shapes      = ExpressiveShapes,
            content     = content
        )
    }
}
