package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF032014),
    primaryContainer = Color(0xFF083D2B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = TealAccent,
    onSecondary = Color(0xFF002230),
    secondaryContainer = Color(0xFF073A4F),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = CoralHeart,
    onTertiary = Color(0xFF3B0007),
    tertiaryContainer = Color(0xFF5C0E1A),
    onTertiaryContainer = Color(0xFFFFD2D7),
    background = SlateDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = SlateDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SlateDarkCard,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = SlateDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = MintLight,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = CoralHeart,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFF9F1239),
    background = LightBg,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our consistent vibrant health palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
