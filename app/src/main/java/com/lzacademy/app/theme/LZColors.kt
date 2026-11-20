package com.lzacademy.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Raw brand values from the LZ Academy brief. Prefer [LZColors] (semantic tokens) in UI code. */
object LZPalette {
    val PrimaryGreen = Color(0xFF059669)
    val DarkGreen = Color(0xFF047857)
    val Turquoise = Color(0xFF14B8A0)
    val DeepPurple = Color(0xFF342047)
    val Background = Color(0xFFF3F2F9) // lavande très claire
    val Surface = Color(0xFFFFFFFF)
    val SoftGreen = Color(0xFFECF6F1)
    val TextPrimary = Color(0xFF171B26)
    val TextSecondary = Color(0xFF667085) // gris bleuté
    val Border = Color(0xFFE9E7F0)
    val Success = Color(0xFF16A36C)
    val Warning = Color(0xFFE9A23B)
    val Error = Color(0xFFDC5252)

    // Compact gradient headers : violet foncé ↔ vert émeraude (dual brand colour)
    val HeaderViolet = Color(0xFF2E1F4D)
    val HeaderBridge = Color(0xFF1E4E48)
    val HeaderEmerald = Color(0xFF059669)

    // Light header surfaces (one brand colour, content-first look)
    val HeaderTintStudent = Color(0xFFEFF8F4)
    val HeaderTintTeacher = Color(0xFFEDF7F6)
    val HeaderTintAdmin = Color(0xFFF3F6F5)

    // Accents used by the prototypes (subjects, charts, status tiles)
    val Info = Color(0xFF3B82F6)
    val Violet = Color(0xFF7E57C2)
    val Gold = Color(0xFFF5B63A)
    val Orange = Color(0xFFF08A3C)

    // Pastel tiles used on the administration header
    val PastelMint = Color(0xFFDDF4EC)
    val PastelLavender = Color(0xFFECE4F7)
    val PastelPeach = Color(0xFFFCE9D2)
    val PastelRose = Color(0xFFF7E1EE)

    // Lavender chip (inactive category tabs, search field)
    val LavenderChip = Color(0xFFE9E6F4)
    val LavenderChipDark = Color(0xFF232A38)
}

/** Semantic colour tokens. Every component reads colours from here, never from [LZPalette] directly. */
@Immutable
data class LZColors(
    val isDark: Boolean,
    val primary: Color,
    val primaryDark: Color,
    val turquoise: Color,
    val purple: Color,
    val purpleDeep: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val successSoft: Color,
    val warningSoft: Color,
    val errorSoft: Color,
    val infoSoft: Color,
    val purpleSoft: Color,
    val turquoiseSoft: Color,
    val lavenderSoft: Color,
    val shadow: Color,
    val studentHeader: List<Color>,
    val teacherHeader: List<Color>,
    val adminHeader: List<Color>,
    val onHeader: Color,
    val heroGreen: List<Color>,
    val heroPurple: List<Color>,
) {
    val studentHeaderBrush: Brush get() = Brush.linearGradient(studentHeader)
    val teacherHeaderBrush: Brush get() = Brush.linearGradient(teacherHeader)
    val adminHeaderBrush: Brush get() = Brush.linearGradient(adminHeader)
    val heroGreenBrush: Brush get() = Brush.linearGradient(heroGreen)
    val heroPurpleBrush: Brush get() = Brush.linearGradient(heroPurple)
    val primaryBrush: Brush get() = Brush.horizontalGradient(listOf(primary, primaryDark))
}

val LightLZColors = LZColors(
    isDark = false,
    primary = LZPalette.PrimaryGreen,
    primaryDark = LZPalette.DarkGreen,
    turquoise = LZPalette.Turquoise,
    purple = LZPalette.DeepPurple,
    purpleDeep = LZPalette.DeepPurple,
    onPrimary = Color.White,
    background = LZPalette.Background,
    surface = LZPalette.Surface,
    surfaceSoft = LZPalette.SoftGreen,
    textPrimary = LZPalette.TextPrimary,
    textSecondary = LZPalette.TextSecondary,
    border = LZPalette.Border,
    success = LZPalette.Success,
    warning = LZPalette.Warning,
    error = LZPalette.Error,
    info = LZPalette.Info,
    successSoft = Color(0xFFE3F5EC),
    warningSoft = Color(0xFFFDF1DD),
    errorSoft = Color(0xFFFBE7E7),
    infoSoft = Color(0xFFE6EEFD),
    purpleSoft = Color(0xFFEFE8F7),
    turquoiseSoft = Color(0xFFDFF5F1),
    lavenderSoft = LZPalette.LavenderChip,
    shadow = Color(0x14171B26),
    // Élève / enseignant : émeraude dominant → violet ; admin : violet dominant → émeraude
    studentHeader = listOf(LZPalette.HeaderEmerald, Color(0xFF146C5C), LZPalette.HeaderViolet),
    teacherHeader = listOf(LZPalette.HeaderEmerald, Color(0xFF175E63), LZPalette.HeaderViolet),
    adminHeader = listOf(LZPalette.HeaderViolet, LZPalette.HeaderBridge, LZPalette.HeaderEmerald),
    onHeader = Color.White,
    heroGreen = listOf(Color(0xFF0AA67D), LZPalette.DarkGreen),
    heroPurple = listOf(LZPalette.Turquoise, LZPalette.DarkGreen),
)

val DarkLZColors = LZColors(
    isDark = true,
    primary = Color(0xFF22B88C),
    primaryDark = Color(0xFF0B7F61),
    turquoise = Color(0xFF2CCDB4),
    purple = Color(0xFFC4A8E3),
    purpleDeep = Color(0xFF342047),
    onPrimary = Color(0xFF04261D),
    background = Color(0xFF0C1310),
    surface = Color(0xFF151F1B),
    surfaceSoft = Color(0xFF1C2B25),
    textPrimary = Color(0xFFE7EFEB),
    textSecondary = Color(0xFF93A39C),
    border = Color(0xFF263530),
    success = Color(0xFF2FC083),
    warning = Color(0xFFF0B050),
    error = Color(0xFFEA7070),
    info = Color(0xFF6FA3F8),
    successSoft = Color(0xFF16352A),
    warningSoft = Color(0xFF3A2D14),
    errorSoft = Color(0xFF3B1E1F),
    infoSoft = Color(0xFF1B2A44),
    purpleSoft = Color(0xFF2A2037),
    turquoiseSoft = Color(0xFF15332F),
    lavenderSoft = LZPalette.LavenderChipDark,
    shadow = Color(0x66000000),
    studentHeader = listOf(Color(0xFF046D4F), Color(0xFF12453F), Color(0xFF241636)),
    teacherHeader = listOf(Color(0xFF046D4F), Color(0xFF14424A), Color(0xFF241636)),
    adminHeader = listOf(Color(0xFF241636), Color(0xFF123E39), Color(0xFF045B45)),
    onHeader = Color(0xFFE7EFEB),
    heroGreen = listOf(Color(0xFF0A8A68), Color(0xFF05503F)),
    heroPurple = listOf(Color(0xFF0E8F7C), Color(0xFF05503F)),
)

val LocalLZColors = staticCompositionLocalOf { LightLZColors }

internal fun LZColors.toMaterialColorScheme(): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = surfaceSoft,
            onPrimaryContainer = primary,
            secondary = turquoise,
            onSecondary = onPrimary,
            tertiary = purple,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceSoft,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border,
            error = error,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = surfaceSoft,
            onPrimaryContainer = primaryDark,
            secondary = turquoise,
            onSecondary = onPrimary,
            tertiary = purple,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceSoft,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border,
            error = error,
            onError = Color.White,
        )
    }
