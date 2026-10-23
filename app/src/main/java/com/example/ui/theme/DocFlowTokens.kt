package com.example.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class DocFlowPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val pressOverlay: Color,
    val accentBlue: Color = DocFlowAccentBlue
)

/**
 * Single source of truth for the DocFlow Shared Design System.
 * Every screen (Recent, Tools, Manage, Viewer, Scanner, Search, Settings, Private Safe,
 * Recently deleted, dialogs, and sheets) reads colors, radii, spacing, typography, and motion from here.
 *
 * Strictly flat, minimal, Android-style:
 * - No glassmorphism, no blur, no heavy shadows, no gradients (except scanner camera overlay).
 */
object DocFlowTokens {

    fun palette(isDark: Boolean): DocFlowPalette {
        return if (isDark) {
            DocFlowPalette(
                isDark = true,
                background = DocFlowDarkBackground,
                surface = DocFlowDarkCard,
                surfaceSubtle = Color(0xFF23262B),
                textPrimary = DocFlowDarkTextPrimary,
                textSecondary = DocFlowDarkTextSecondary,
                divider = DocFlowDarkDivider,
                pressOverlay = Color.White.copy(alpha = 0.08f)
            )
        } else {
            DocFlowPalette(
                isDark = false,
                background = DocFlowLightBackground,
                surface = DocFlowLightCard,
                surfaceSubtle = Color(0xFFF6F7F9),
                textPrimary = DocFlowLightTextPrimary,
                textSecondary = DocFlowLightTextSecondary,
                divider = DocFlowLightDivider,
                pressOverlay = Color.Black.copy(alpha = 0.08f)
            )
        }
    }

    @Composable
    @ReadOnlyComposable
    fun currentPalette(): DocFlowPalette = palette(isDark)

    // -------------------------------------------------------------------------
    // 1. Dynamic Theme-Aware Colors
    // -------------------------------------------------------------------------
    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val background: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) DocFlowDarkBackground else DocFlowLightBackground

    val surface: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) DocFlowDarkCard else DocFlowLightCard

    val surfaceSubtle: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color(0xFF23262B) else Color(0xFFF6F7F9)

    val textPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) DocFlowDarkTextPrimary else DocFlowLightTextPrimary

    val textSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) DocFlowDarkTextSecondary else DocFlowLightTextSecondary

    val divider: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) DocFlowDarkDivider else DocFlowLightDivider

    val pressOverlay: Color
        @Composable
        @ReadOnlyComposable
        get() = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f)

    // Fixed semantic & icon-circle accents
    val accentBlue: Color = DocFlowAccentBlue           // #1F6BFF
    val iconRedPdf: Color = DocFlowRedPdf               // #E5483F
    val iconBlueSummarySafe: Color = DocFlowBlueIcon    // #1F8BFF
    val iconTealTranslate: Color = DocFlowTealTranslate // #1FB8C9
    val iconGreenDownload: Color = DocFlowGreenDownload // #25C25A
    val iconGrayTrash: Color = DocFlowGrayTrash         // #9A9DA3
    val iconOrangePpt: Color = DocFlowOrangePpt         // #EA580C
    val iconGreenDarkExcel: Color = DocFlowGreenExcel   // #16A34A
    val searchMatchYellow: Color = DocFlowSearchMatchYellow
    val searchCurrentOrange: Color = DocFlowSearchCurrentOrange

    // PascalCase aliases for direct token access across screens
    @get:JvmName("getAccentBluePascal")
    val AccentBlue: Color = accentBlue
    val IconCircleRed: Color = iconRedPdf
    val IconCircleBlue: Color = iconBlueSummarySafe
    val IconCircleTeal: Color = iconTealTranslate
    val IconCircleGreen: Color = iconGreenDownload
    val IconCircleGray: Color = iconGrayTrash
    val IconCircleOrangePpt: Color = iconOrangePpt
    val IconCircleOrange: Color = iconOrangePpt
    val IconCircleGreenExcel: Color = iconGreenDarkExcel
    val IconCircleExcelGreen: Color = iconGreenDarkExcel

    // -------------------------------------------------------------------------
    // 2. Corner Radii
    // -------------------------------------------------------------------------
    val cardRadius: Dp = 24.dp
    val sheetTopRadius: Dp = 28.dp
    val buttonRadius: Dp = 16.dp
    val pillRadius: Dp = 999.dp
    val thumbnailRadius: Dp = 8.dp
    val smallRadius: Dp = 12.dp

    val cardCornerRadius: Dp = cardRadius
    val sheetTopCornerRadius: Dp = sheetTopRadius
    val thumbnailCornerRadius: Dp = thumbnailRadius

    val cardShape = RoundedCornerShape(cardRadius)
    val sheetTopShape = RoundedCornerShape(topStart = sheetTopRadius, topEnd = sheetTopRadius)
    val sheetShape = sheetTopShape
    val buttonShape = RoundedCornerShape(buttonRadius)
    val rectButtonShape = buttonShape
    val pillShape = CircleShape
    val pillButtonShape = pillShape
    val thumbnailShape = RoundedCornerShape(thumbnailRadius)
    val smallShape = RoundedCornerShape(smallRadius)

    // -------------------------------------------------------------------------
    // 3. Spacing (4.dp Grid)
    // -------------------------------------------------------------------------
    val space1: Dp = 4.dp
    val space2: Dp = 8.dp
    val space3: Dp = 12.dp
    val space4: Dp = 16.dp
    val space5: Dp = 20.dp
    val space6: Dp = 24.dp
    val space8: Dp = 32.dp

    val screenSideMargin: Dp = 16.dp
    val screenHorizontalMargin: Dp = screenSideMargin
    val cardPaddingMin: Dp = 16.dp
    val cardPaddingMax: Dp = 20.dp
    val cardPaddingHorizontal: Dp = cardPaddingMin
    val cardPaddingVertical: Dp = cardPaddingMin
    val minTouchTarget: Dp = 48.dp
    val dividerThickness: Dp = 1.dp
    val maxContentWidth: Dp = 720.dp

    fun adaptiveScreenMargin(screenWidthDp: Int): Dp {
        return when {
            screenWidthDp < 360 -> 12.dp
            screenWidthDp >= 600 -> 24.dp
            else -> 16.dp
        }
    }

    // -------------------------------------------------------------------------
    // 4. Typography (Roboto / Inter System Sans)
    // -------------------------------------------------------------------------
    val largeTitleSize: TextUnit = 40.sp
    val sectionLabelSize: TextUnit = 20.sp
    val rowTitleSize: TextUnit = 18.sp
    val bodySize: TextUnit = 16.sp
    val captionSize: TextUnit = 14.sp

    val largeTitleStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp
    )

    fun adaptiveLargeTitleStyle(screenWidthDp: Int): TextStyle {
        return when {
            screenWidthDp < 350 -> largeTitleStyle.copy(fontSize = 30.sp, lineHeight = 36.sp)
            screenWidthDp < 390 -> largeTitleStyle.copy(fontSize = 34.sp, lineHeight = 40.sp)
            else -> largeTitleStyle
        }
    }

    val sectionLabelStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )

    val rowTitleStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )

    val bodyStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    val bodyMediumStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    val captionStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )

    val captionBoldStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )

    val microBadgeStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    // -------------------------------------------------------------------------
    // 5. Motion (200-250ms ease-out; no bouncy or glass effects)
    // -------------------------------------------------------------------------
    const val MOTION_DURATION_MS: Int = 220
    val motionEasing: Easing = FastOutSlowInEasing

    fun <T> standardTween() = tween<T>(
        durationMillis = MOTION_DURATION_MS,
        easing = motionEasing
    )

    // -------------------------------------------------------------------------
    // 6. File Type Color & Label Helpers
    // -------------------------------------------------------------------------
    fun fileFormatColor(fileType: String): Color {
        return when (fileType.lowercase()) {
            "pdf", "scanned" -> iconRedPdf
            "docx", "doc", "note" -> accentBlue
            "xlsx", "xls", "csv" -> iconGreenDarkExcel
            "pptx", "ppt" -> iconOrangePpt
            "ofd" -> DocFlowPurpleOfd
            "txt", "md" -> iconGrayTrash
            else -> accentBlue
        }
    }

    fun fileFormatShortBadge(fileType: String): String {
        return when (fileType.lowercase()) {
            "pdf", "scanned" -> "PDF"
            "docx", "doc", "note" -> "W"
            "xlsx", "xls", "csv" -> "XLS"
            "pptx", "ppt" -> "PPT"
            "ofd" -> "OFD"
            "txt", "md" -> "TXT"
            "png", "jpg", "jpeg" -> "IMG"
            else -> fileType.uppercase().take(3)
        }
    }
}
