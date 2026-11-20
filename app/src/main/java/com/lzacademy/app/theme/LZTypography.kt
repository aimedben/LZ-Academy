package com.lzacademy.app.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/** Inter bundled in res/font (regular / medium / semibold / bold). */
val LZFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)
val LZSerifFamily: FontFamily = FontFamily.Serif

/** Typographic scale of LZ Academy. Colours are applied by components, not by the styles. */
@Immutable
data class LZTypography(
    val display: TextStyle,
    val pageTitle: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val secondary: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,
    val stat: TextStyle,
    val statLarge: TextStyle,
    val button: TextStyle,
    val brand: TextStyle,
)

val DefaultLZTypography = LZTypography(
    display = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    pageTitle = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp),
    sectionTitle = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp),
    cardTitle = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = (-0.1).sp),
    body = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyStrong = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    secondary = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    label = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
    labelSmall = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    stat = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    statLarge = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.8).sp),
    button = TextStyle(fontFamily = LZFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    brand = TextStyle(fontFamily = LZSerifFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
)

val LocalLZTypography = staticCompositionLocalOf { DefaultLZTypography }

internal fun LZTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = display,
    headlineMedium = pageTitle,
    titleLarge = sectionTitle,
    titleMedium = cardTitle,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = secondary,
    labelLarge = button,
    labelMedium = label,
    labelSmall = labelSmall,
)
