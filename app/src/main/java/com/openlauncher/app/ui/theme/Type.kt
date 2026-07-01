package com.openlauncher.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun launcherTypography(bold: Boolean, scale: Float, fontFamily: FontFamily = FontFamily.Default): Typography {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    return Typography(
        displayLarge   = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = (64 * scale).sp),
        displayMedium  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold,   fontSize = (52 * scale).sp),
        headlineLarge  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = (36 * scale).sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = (32 * scale).sp),
        headlineSmall  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = (28 * scale).sp),
        titleLarge     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = (24 * scale).sp),
        titleMedium    = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (16 * scale).sp),
        titleSmall     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp),
        bodyLarge      = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = (16 * scale).sp),
        bodyMedium     = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = (14 * scale).sp),
        bodySmall      = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = (12 * scale).sp),
        labelLarge     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp),
        labelMedium    = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp),
        labelSmall     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (11 * scale).sp),
    )
}
