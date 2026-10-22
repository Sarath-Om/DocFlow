package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DocBlueDarkPrimary,
    onPrimary = Color(0xFF003064),
    primaryContainer = DocBlueDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFF002957),
    secondary = DocBlueDarkSecondary,
    onSecondary = Color(0xFF003739),
    secondaryContainer = DocBlueDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFF006C71),
    tertiary = DocEmeraldDarkAccent,
    onTertiary = Color(0xFF3C0090),
    tertiaryContainer = DocVioletDarkContainer,
    onTertiaryContainer = Color(0xFF34007F),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = DocBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF006C71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF63F7FF),
    onSecondaryContainer = Color(0xFF002021),
    tertiary = Color(0xFF5700C9),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun DocFlowTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) = DocFlowTheme(darkTheme = darkTheme, content = content)
