package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// DocFlow Shared Design System Color Tokens (Flat, Minimal, Android-style — No Glassmorphism)
val DocFlowAccentBlue = Color(0xFF1F6BFF)
val DocFlowAccentBlueDark = Color(0xFF1F6BFF)

// Light Mode Tokens
val DocFlowLightBackground = Color(0xFFF0F1F3)
val DocFlowLightCard = Color(0xFFFFFFFF)
val DocFlowLightTextPrimary = Color(0xFF111111)
val DocFlowLightTextSecondary = Color(0xFF8A8D93)
val DocFlowLightDivider = Color(0xFFE3E4E7)

// Dark Mode Tokens
val DocFlowDarkBackground = Color(0xFF0F1113)
val DocFlowDarkCard = Color(0xFF1B1D20)
val DocFlowDarkTextPrimary = Color(0xFFF2F3F5)
val DocFlowDarkTextSecondary = Color(0xFF9A9DA3)
val DocFlowDarkDivider = Color(0xFF2A2C30)

// Icon-Circle & File Format Accents
val DocFlowRedPdf = Color(0xFFE5483F)       // Red #E5483F (PDF, convert)
val DocFlowBlueIcon = Color(0xFF1F8BFF)     // Blue #1F8BFF (summary, safe, Bluetooth)
val DocFlowBlueWord = Color(0xFF1F6BFF)     // Blue #1F6BFF (Word, primary)
val DocFlowTealTranslate = Color(0xFF1FB8C9) // Teal #1FB8C9 (translate)
val DocFlowGreenDownload = Color(0xFF25C25A) // Green #25C25A (downloads, WhatsApp)
val DocFlowGreenExcel = Color(0xFF16A34A)   // Green-dark (Excel)
val DocFlowOrangePpt = Color(0xFFEA580C)    // Orange (PPT)
val DocFlowGrayTrash = Color(0xFF9A9DA3)    // Gray #9A9DA3 (trash, TXT)
val DocFlowGrayTxt = Color(0xFF9A9DA3)
val DocFlowPurpleOfd = Color(0xFF7C3AED)

// Highlight & Search Match Tokens
val DocFlowSearchMatchYellow = Color(0xFFFDE047)
val DocFlowSearchCurrentOrange = Color(0xFFF97316)
val DocFlowDangerRed = Color(0xFFE5483F)
val DocFlowWarningAmber = Color(0xFFD97706)

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
val DocBlueDarkSecondary = DocFlowTealTranslate
val DocBlueDarkSecondaryContainer = Color(0xFF115E59)
val DocEmeraldDarkAccent = DocFlowGreenDownload
val DocVioletDarkContainer = Color(0xFF5B21B6)

val DarkBackground = DocFlowDarkBackground
val DarkSurface = DocFlowDarkCard
val DarkSurfaceLowest = Color(0xFF0A0C0E)
val DarkSurfaceLow = Color(0xFF15171A)
val DarkSurfaceContainer = DocFlowDarkCard
val DarkSurfaceHigh = Color(0xFF23262A)
val DarkSurfaceVariant = Color(0xFF25282C)
val DarkOnSurface = DocFlowDarkTextPrimary
val DarkOnSurfaceVariant = DocFlowDarkTextSecondary
val DarkOutline = DocFlowDarkDivider
val DarkOutlineVariant = DocFlowDarkDivider

val LightBackground = DocFlowLightBackground
val LightSurface = DocFlowLightCard
val LightSurfaceVariant = Color(0xFFE8EAEE)
val LightOnSurface = DocFlowLightTextPrimary
val LightOnSurfaceVariant = DocFlowLightTextSecondary

val CrystalCyan = DocFlowTealTranslate
val CrystalSapphire = DocFlowAccentBlue
val CrystalViolet = DocFlowPurpleOfd
val CrystalAmber = DocFlowOrangePpt
val CrystalEmerald = DocFlowGreenExcel
val CrystalRose = DocFlowRedPdf

val FolderColors = listOf(
    "#1F6BFF", // Accent Blue
    "#E5483F", // PDF Red
    "#1FB8C9", // Translate Teal
    "#25C25A", // Download Green
    "#16A34A", // Excel Dark Green
    "#EA580C", // PPT Orange
    "#1F8BFF", // Summary/Safe Blue
    "#9A9DA3"  // Neutral Gray
)
