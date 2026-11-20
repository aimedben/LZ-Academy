package com.lzacademy.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** Accessor for the LZ design tokens: `LZ.colors.primary`, `LZ.type.body`. */
object LZ {
    val colors: LZColors
        @Composable @ReadOnlyComposable get() = LocalLZColors.current

    val type: LZTypography
        @Composable @ReadOnlyComposable get() = LocalLZTypography.current
}

enum class ThemeMode(val label: String) {
    System("Système"),
    Light("Clair"),
    Dark("Sombre"),
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun LZTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkLZColors else LightLZColors
    CompositionLocalProvider(
        LocalLZColors provides colors,
        LocalLZTypography provides DefaultLZTypography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = DefaultLZTypography.toMaterialTypography(),
            shapes = LZMaterialShapes,
            content = content,
        )
    }
}
