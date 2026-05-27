package com.openlauncher.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.openlauncher.app.R
import com.openlauncher.app.data.AppFont

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_light,   FontWeight.Light),
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold,    FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold,    FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold,    FontWeight.Bold),
)

val SourceCodePro = FontFamily(
    Font(R.font.source_code_pro_regular, FontWeight.Normal),
    Font(R.font.source_code_pro_bold,    FontWeight.Medium),
    Font(R.font.source_code_pro_bold,    FontWeight.SemiBold),
    Font(R.font.source_code_pro_bold,    FontWeight.Bold),
)

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.SYSTEM          -> FontFamily.Default
    AppFont.JETBRAINS_MONO  -> JetBrainsMono
    AppFont.SOURCE_CODE_PRO -> SourceCodePro
}

fun launcherTypography(bold: Boolean, scale: Float, fontFamily: FontFamily = FontFamily.Default): Typography {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    return Typography(
        displayLarge   = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = (57 * scale).sp),
        displayMedium  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = (45 * scale).sp),
        headlineLarge  = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = (32 * scale).sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = (28 * scale).sp),
        headlineSmall  = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = (24 * scale).sp),
        titleLarge     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (22 * scale).sp),
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
