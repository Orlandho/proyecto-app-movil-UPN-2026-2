package com.example.foodjeetapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = FoodJetPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = FoodJetPrimaryLight,
    onPrimaryContainer = FoodJetDark,
    secondary = FoodJetPrimary,
    onSecondary = Color.Black,
    secondaryContainer = FoodJetPrimaryLight,
    onSecondaryContainer = FoodJetDark,
    background = FoodJetLightGray,
    onBackground = FoodJetDark,
    surface = Color.White,
    onSurface = FoodJetDark,
    surfaceVariant = FoodJetLightGray,
    onSurfaceVariant = FoodJetGray,
    error = FoodJetDanger,
    onError = Color.White,
    outline = FoodJetBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = FoodJetPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A2818),
    onPrimaryContainer = FoodJetPrimaryLight,
    secondary = FoodJetPrimaryDark,
    onSecondary = Color.White,
    background = Color(0xFF141414),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF202020),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = FoodJetDanger,
    onError = Color.White,
    outline = Color(0xFF404040)
)

@Composable
fun FoodJetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
