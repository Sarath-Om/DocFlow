package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DocFlowAccentBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF17397A),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = DocFlowTealTranslate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = DocFlowGreenExcel,
    onTertiary = Color.White,
    background = DocFlowDarkBackground,
    onBackground = DocFlowDarkTextPrimary,
    surface = DocFlowDarkCard,
    onSurface = DocFlowDarkTextPrimary,
    surfaceVariant = Color(0xFF25282C),
    onSurfaceVariant = DocFlowDarkTextSecondary,
    outline = Color(0xFF3E4249),
    outlineVariant = DocFlowDarkDivider,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = DocFlowAccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0ECFF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = DocFlowTealTranslate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = DocFlowGreenExcel,
    onTertiary = Color.White,
    background = DocFlowLightBackground,
    onBackground = DocFlowLightTextPrimary,
    surface = DocFlowLightCard,
    onSurface = DocFlowLightTextPrimary,
    surfaceVariant = Color(0xFFE8EAEE),
    onSurfaceVariant = DocFlowLightTextSecondary,
    outline = Color(0xFFD1D5DB),
    outlineVariant = DocFlowLightDivider
)

@Composable
fun DocFlowTheme(
    darkTheme: Boolean = false,
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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) = DocFlowTheme(darkTheme = darkTheme, content = content)
