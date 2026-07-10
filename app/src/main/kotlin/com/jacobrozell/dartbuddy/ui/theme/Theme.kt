package com.jacobrozell.dartbuddy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Brand {
    val Green = Color(0xFF33AE52)
    val Red = Color(0xFFE6473D)
    val RedAccent = Color(0xFFD6332E)
    val Amber = Color(0xFFF5B31F)
    val Orange = Color(0xFFED731F)
    val ProBot = Color(0xFF9E61FA)

    val BackgroundLight = Color(0xFFF2F2F7)
    val BackgroundDark = Color(0xFF0A0A0D)
    val CardLight = Color.White
    val CardDark = Color(0xFF1C1C1E)
    val CardElevatedLight = Color(0xFFEBEBF0)
    val CardElevatedDark = Color(0xFF29292B)
    val DartBoxLight = Color(0xFFCCCCD1)
    val DartBoxDark = Color(0xFF29292B)

    val TextPrimaryLight = Color(0xFF141419)
    val TextPrimaryDark = Color.White
    val SplashCanvasLight = Color(0xFFF2F0EB)
    val SplashCanvasDark = Color(0xFF0D0F17)
    val TextSecondaryLight = Color(0xFF595961)
    val TextSecondaryDark = Color(0x8FFFFFFF)
    val InkOnBright = Color(0xFF141419)
}

private val LightColors = lightColorScheme(
    primary = Brand.Green,
    secondary = Brand.ProBot,
    tertiary = Brand.Amber,
    background = Brand.BackgroundLight,
    surface = Brand.CardLight,
    surfaceVariant = Brand.CardElevatedLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Brand.TextPrimaryLight,
    onSurface = Brand.TextPrimaryLight,
    error = Brand.RedAccent,
)

private val DarkColors = darkColorScheme(
    primary = Brand.Green,
    secondary = Brand.ProBot,
    tertiary = Brand.Amber,
    background = Brand.BackgroundDark,
    surface = Brand.CardDark,
    surfaceVariant = Brand.CardElevatedDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Brand.TextPrimaryDark,
    onSurface = Brand.TextPrimaryDark,
    error = Brand.RedAccent,
)

@Composable
fun DartBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
