package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// DocFlow Flat Minimal Design System Palette
val DocFlowAccentBlue = Color(0xFF1F6BFF)
val DocFlowAccentBlueDark = Color(0xFF4785FF)

// Light Mode Tokens
val DocFlowLightBackground = Color(0xFFF0F1F3)
val DocFlowLightCard = Color(0xFFFFFFFF)
val DocFlowLightTextPrimary = Color(0xFF111111)
val DocFlowLightTextSecondary = Color(0xFF8A8D93)
val DocFlowLightDivider = Color(0xFFE5E7EB)

// Dark Mode Tokens
val DocFlowDarkBackground = Color(0xFF0F1113)
val DocFlowDarkCard = Color(0xFF1B1D20)
val DocFlowDarkTextPrimary = Color(0xFFF2F3F5)
val DocFlowDarkTextSecondary = Color(0xFF9A9DA3)
val DocFlowDarkDivider = Color(0xFF2A2D32)

// Icon Circle & File Format Accents
val DocFlowRedPdf = Color(0xFFE53935)
val DocFlowBlueWord = Color(0xFF1F6BFF)
val DocFlowTealTranslate = Color(0xFF0D9488)
val DocFlowGreenExcel = Color(0xFF16A34A)
val DocFlowOrangePpt = Color(0xFFEA580C)
val DocFlowGrayTxt = Color(0xFF6B7280)
val DocFlowPurpleOfd = Color(0xFF7C3AED)

val BadgePdfRed = DocFlowRedPdf
val BadgeDocBlue = DocFlowBlueWord
val BadgeXlsGreen = DocFlowGreenExcel
val BadgePptOrange = DocFlowOrangePpt
val BadgeOfdPurple = DocFlowPurpleOfd
val BadgeTxtSlate = DocFlowGrayTxt

// Legacy compatibility aliases used by sub-components
val DocBluePrimary = DocFlowAccentBlue
val DocBlueSecondary = DocFlowTealTranslate
val DocEmeraldAccent = DocFlowGreenExcel

val DocBlueDarkPrimary = DocFlowAccentBlueDark
val DocBlueDarkPrimaryContainer = Color(0xFF17397A)
val DocBlueDarkSecondary = Color(0xFF2DD4BF)
val DocBlueDarkSecondaryContainer = Color(0xFF115E59)
val DocEmeraldDarkAccent = Color(0xFF4ADE80)
val DocVioletDarkContainer = Color(0xFF5B21B6)

val DarkBackground = DocFlowDarkBackground
val DarkSurface = DocFlowDarkCard
val DarkSurfaceLowest = Color(0xFF0A0C0E)
val DarkSurfaceLow = Color(0xFF15171A)
val DarkSurfaceContainer = DocFlowDarkCard
val DarkSurfaceHigh = Color(0xFF23262A)
val DarkSurfaceVariant = Color(0xFF282B30)
val DarkOnSurface = DocFlowDarkTextPrimary
val DarkOnSurfaceVariant = DocFlowDarkTextSecondary
val DarkOutline = Color(0xFF3E4249)
val DarkOutlineVariant = DocFlowDarkDivider

val LightBackground = DocFlowLightBackground
val LightSurface = DocFlowLightCard
val LightSurfaceVariant = Color(0xFFE6E8EC)
val LightOnSurface = DocFlowLightTextPrimary
val LightOnSurfaceVariant = DocFlowLightTextSecondary

val CrystalCyan = DocFlowTealTranslate
val CrystalSapphire = DocFlowAccentBlue
val CrystalViolet = DocFlowPurpleOfd
val CrystalAmber = DocFlowOrangePpt
val CrystalEmerald = DocFlowGreenExcel
val CrystalRose = DocFlowRedPdf

val FolderColors = listOf(
    "#1F6BFF", // DocFlow Blue
    "#E53935", // PDF Red
    "#0D9488", // Translate Teal
    "#16A34A", // Green
    "#EA580C", // Orange
    "#7C3AED", // Purple
    "#D97706", // Amber
    "#6B7280"  // Slate Gray
)
