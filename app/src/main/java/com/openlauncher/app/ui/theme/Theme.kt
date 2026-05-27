package com.openlauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.openlauncher.app.data.AppFont

@Composable
fun OpenLauncherTheme(
    accent: Color     = AccentWhite,
    background: Color = Black,
    fontBold: Boolean = false,
    textScale: Float  = 1.0f,
    appFont: AppFont  = AppFont.JETBRAINS_MONO,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary          = accent,
        onPrimary        = background,
        secondary        = accent.copy(alpha = 0.7f),
        onSecondary      = background,
        tertiary         = accent.copy(alpha = 0.5f),
        background       = background,
        surface          = CardSurface,
        onBackground     = White,
        onSurface        = White,
        surfaceVariant   = DimSurface,
        onSurfaceVariant = TextMuted,
        outline          = DividerGray
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = launcherTypography(fontBold, textScale, appFont.toFontFamily()),
        content     = content
    )
}
