package com.thusee.cake.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A4931),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF351005),
    secondary = Color(0xFF74604F),
    background = Color(0xFFFFF8F3),
    surface = Color(0xFFFFF8F3),
    surfaceContainer = Color(0xFFF6EDE5),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF52220F),
    primaryContainer = Color(0xFF6E3520),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFE3C1A8),
    background = Color(0xFF1A1411),
    surface = Color(0xFF1A1411),
    surfaceContainer = Color(0xFF29201B),
)

@Composable
fun CakeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, content = content
    )
}
