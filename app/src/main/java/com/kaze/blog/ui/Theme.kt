package com.kaze.blog.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color

@Immutable
class KazePalette(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val onBg: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val outline: Color,
    val noteColors: List<Color>,
)

val DarkPalette = KazePalette(
    dark = true,
    bg = Color(0xFF0B0C09),
    surface = Color(0xFF181A14),
    onBg = Color(0xFFEDEEE6),
    muted = Color(0xFF9A9D8F),
    accent = Color(0xFFB7C32C),
    onAccent = Color(0xFF12150A),
    outline = Color(0xFF2C2F24),
    noteColors = listOf(
        Color(0xFF181A14), Color(0xFF3A2227), Color(0xFF3B2B1E), Color(0xFF38341C),
        Color(0xFF1E3328), Color(0xFF1E2F3B), Color(0xFF2C2740),
    ),
)

val LightPalette = KazePalette(
    dark = false,
    bg = Color(0xFFF5F5EF),
    surface = Color(0xFFFFFFFF),
    onBg = Color(0xFF1B1C17),
    muted = Color(0xFF6F7266),
    accent = Color(0xFF7C8A0E),
    onAccent = Color(0xFFFFFFFF),
    outline = Color(0xFFE0E1D6),
    noteColors = listOf(
        Color(0xFFFFFFFF), Color(0xFFFBE1E4), Color(0xFFFFE6D2), Color(0xFFF7F0C6),
        Color(0xFFD9F2E1), Color(0xFFD7EAF7), Color(0xFFE6DFF8),
    ),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun KazeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            background = p.bg, onBackground = p.onBg,
            surface = p.surface, onSurface = p.onBg,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            background = p.bg, onBackground = p.onBg,
            surface = p.surface, onSurface = p.onBg,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalPalette provides p,
            LocalContentColor provides p.onBg,
            content = content,
        )
    }
}
