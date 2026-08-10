package com.cochat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF4338CA)
val IndigoLight = Color(0xFFE0E7FF)
val OnlineGreen = Color(0xFF22C55E)
val AwayAmber = Color(0xFFF59E0B)
val OfflineGray = Color(0xFF9CA3AF)
val DangerRed = Color(0xFFB91C1C)
val DangerBg = Color(0xFFFEF2F2)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = Indigo,
    secondary = Indigo,
    background = Color(0xFFF3F4F6),
    surface = Color.White,
    error = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF818CF8),
    background = Color(0xFF111827),
    surface = Color(0xFF1F2937),
    error = Color(0xFFF87171),
)

@Composable
fun CoChatTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
