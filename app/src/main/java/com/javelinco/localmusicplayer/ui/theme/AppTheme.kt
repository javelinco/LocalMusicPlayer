package com.javelinco.localmusicplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.javelinco.localmusicplayer.data.settings.ThemePreference

private val LightColors = lightColorScheme(
    primary = Color(0xFF654FA3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDCFF),
    onPrimaryContainer = Color(0xFF24163E),
    secondary = Color(0xFF4565A8),
    secondaryContainer = Color(0xFFDCE6FF),
    surface = Color(0xFFFFF9FF),
    surfaceVariant = Color(0xFFE9E4EE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD1B6FF),
    onPrimary = Color(0xFF35235C),
    primaryContainer = Color(0xFF4D3A75),
    onPrimaryContainer = Color(0xFFEBDCFF),
    secondary = Color(0xFFB3C7FF),
    secondaryContainer = Color(0xFF2D477B),
    surface = Color(0xFF17151D),
    surfaceVariant = Color(0xFF302D38),
    background = Color(0xFF17151D),
)

@Composable
fun LocalMusicPlayerTheme(theme: ThemePreference, content: @Composable () -> Unit) {
    val dark = when (theme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
