package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SyncState

/**
 * Apple Crystal Glass ("Liquid Glass") ambient mesh background.
 * Renders an iridescent radial & linear light field behind frosted translucent glass cards.
 */
@Composable
fun CrystalGlassBackground(
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF050B1A),
                Color(0xFF0A1633),
                Color(0xFF111C3D),
                Color(0xFF090F26)
            ),
            start = Offset(0f, 0f),
            end = Offset(1200f, 2400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEAF3FF),
                Color(0xFFF3F7FF),
                Color(0xFFEDE9FE),
                Color(0xFFE0F2FE)
            ),
            start = Offset(0f, 0f),
            end = Offset(1200f, 2400f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseGradient)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-right electric cyan crystal orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF00B4FF).copy(alpha = 0.24f), Color.Transparent)
                    } else {
                        listOf(Color(0xFF38BDF8).copy(alpha = 0.30f), Color.Transparent)
                    },
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.85f, h * 0.12f),
                radius = w * 0.65f
            )

            // Middle-left sapphire-violet crystal orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF6200EA).copy(alpha = 0.22f), Color.Transparent)
                    } else {
                        listOf(Color(0xFF8B5CF6).copy(alpha = 0.22f), Color.Transparent)
                    },
                    center = Offset(w * 0.15f, h * 0.48f),
                    radius = w * 0.7f
                ),
                center = Offset(w * 0.15f, h * 0.48f),
                radius = w * 0.7f
            )

            // Bottom-right aqua glow orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF0066FF).copy(alpha = 0.20f), Color.Transparent)
                    } else {
                        listOf(Color(0xFF60A5FA).copy(alpha = 0.26f), Color.Transparent)
                    },
                    center = Offset(w * 0.8f, h * 0.88f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.8f, h * 0.88f),
                radius = w * 0.6f
            )
        }
        content()
    }
}

/**
 * Apple Crystal Glass Card with translucent frosted fill, diagonal specular rim highlight,
 * and subtle luminous depth.
 */
@Composable
fun CrystalGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    isDark: Boolean = isSystemInDarkTheme(),
    tintColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)

    val fillBrush = if (isDark) {
        val baseTop = tintColor?.copy(alpha = 0.24f) ?: Color(0xFF1E293B).copy(alpha = 0.64f)
        val baseBottom = Color(0xFF0F172A).copy(alpha = 0.52f)
        Brush.linearGradient(
            colors = listOf(baseTop, baseBottom),
            start = Offset(0f, 0f),
            end = Offset(600f, 600f)
        )
    } else {
        val baseTop = tintColor?.copy(alpha = 0.22f) ?: Color.White.copy(alpha = 0.78f)
        val baseBottom = Color.White.copy(alpha = 0.56f)
        Brush.linearGradient(
            colors = listOf(baseTop, baseBottom),
            start = Offset(0f, 0f),
            end = Offset(600f, 600f)
        )
    }

    val specularBorderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                Color(0xFF38BDF8).copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.08f)
            ),
            start = Offset(0f, 0f),
            end = Offset(500f, 500f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFF93C5FD).copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.35f)
            ),
            start = Offset(0f, 0f),
            end = Offset(500f, 500f)
        )
    }

    val clickMod = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 10.dp else 6.dp,
                shape = shape,
                ambientColor = Color(0xFF0066FF).copy(alpha = 0.16f),
                spotColor = Color(0xFF0066FF).copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(brush = fillBrush, shape = shape)
            .border(width = 1.2.dp, brush = specularBorderBrush, shape = shape)
            .then(clickMod),
        content = content
    )
}

/**
 * Custom Canvas reproduction of the uploaded Glossy DocFlow Document Logo:
 * - Glossy 3D 'D' monogram (deep navy -> electric sapphire -> cyan -> violet)
 * - Crisp white folded-corner document page inside the 'D' with 3 cyan bars
 * - Flowing liquid wave ribbon at the bottom & left with 3 floating cyan data pixels
 */
@Composable
fun DocFlowBrandLogo(
    iconSize: Dp = 42.dp,
    showWordmark: Boolean = false,
    wordmarkSize: TextUnit = 20.sp,
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(iconSize * 0.28f),
                    spotColor = Color(0xFF007BFF).copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(iconSize * 0.28f))
                .background(
                    Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF14244C), Color(0xFF0C1736))
                        } else {
                            listOf(Color(0xFFFFFFFF), Color(0xFFEFF6FF))
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color(0xFF00B4FF).copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(iconSize * 0.28f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 1. Outer Glossy 'D' Monogram Shape
                val dPath = Path().apply {
                    moveTo(w * 0.28f, h * 0.16f)
                    lineTo(w * 0.55f, h * 0.16f)
                    cubicTo(
                        w * 0.78f, h * 0.16f,
                        w * 0.88f, h * 0.32f,
                        w * 0.88f, h * 0.50f
                    )
                    cubicTo(
                        w * 0.88f, h * 0.68f,
                        w * 0.76f, h * 0.84f,
                        w * 0.54f, h * 0.84f
                    )
                    lineTo(w * 0.28f, h * 0.84f)
                    lineTo(w * 0.28f, h * 0.57f)
                    lineTo(w * 0.38f, h * 0.57f)
                    lineTo(w * 0.38f, h * 0.38f)
                    lineTo(w * 0.28f, h * 0.38f)
                    close()
                }
                drawPath(
                    path = dPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF041B70),
                            Color(0xFF0066FF),
                            Color(0xFF00B8FF),
                            Color(0xFF6200EA)
                        ),
                        start = Offset(w * 0.25f, h * 0.15f),
                        end = Offset(w * 0.88f, h * 0.85f)
                    )
                )

                // 2. White Document Page Inside the 'D'
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.39f, h * 0.27f),
                    size = Size(w * 0.33f, h * 0.43f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )

                // Folded Top-Right Document Corner
                val foldPath = Path().apply {
                    moveTo(w * 0.61f, h * 0.27f)
                    lineTo(w * 0.72f, h * 0.38f)
                    lineTo(w * 0.61f, h * 0.38f)
                    close()
                }
                drawPath(path = foldPath, color = Color(0xFF00B4FF))

                // 3. Cyan Document Text Bars
                val barX = w * 0.44f
                val barH = h * 0.042f
                drawRoundRect(
                    color = Color(0xFF0088FF),
                    topLeft = Offset(barX, h * 0.41f),
                    size = Size(w * 0.15f, barH),
                    cornerRadius = CornerRadius(barH / 2, barH / 2)
                )
                drawRoundRect(
                    color = Color(0xFF00A6FF),
                    topLeft = Offset(barX, h * 0.485f),
                    size = Size(w * 0.21f, barH),
                    cornerRadius = CornerRadius(barH / 2, barH / 2)
                )
                drawRoundRect(
                    color = Color(0xFF00B8FF),
                    topLeft = Offset(barX, h * 0.56f),
                    size = Size(w * 0.17f, barH),
                    cornerRadius = CornerRadius(barH / 2, barH / 2)
                )

                // 4. Flowing Cyan & Violet Wave Ribbon across bottom
                val wavePath = Path().apply {
                    moveTo(w * 0.22f, h * 0.46f)
                    cubicTo(
                        w * 0.38f, h * 0.46f,
                        w * 0.44f, h * 0.68f,
                        w * 0.58f, h * 0.67f
                    )
                    cubicTo(
                        w * 0.68f, h * 0.66f,
                        w * 0.77f, h * 0.58f,
                        w * 0.85f, h * 0.52f
                    )
                    cubicTo(
                        w * 0.80f, h * 0.74f,
                        w * 0.64f, h * 0.84f,
                        w * 0.45f, h * 0.84f
                    )
                    lineTo(w * 0.28f, h * 0.84f)
                    lineTo(w * 0.28f, h * 0.54f)
                    lineTo(w * 0.22f, h * 0.54f)
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00C2FF),
                            Color(0xFF0077FF),
                            Color(0xFF6200EA)
                        ),
                        start = Offset(w * 0.22f, h * 0.46f),
                        end = Offset(w * 0.85f, h * 0.84f)
                    )
                )

                // 5. Three Floating Cyan Stream Pixels on the Left
                val pixelSize = Size(w * 0.075f, h * 0.055f)
                val pixelCorner = CornerRadius(w * 0.02f, w * 0.02f)
                drawRoundRect(
                    color = Color(0xFF00B8FF),
                    topLeft = Offset(w * 0.17f, h * 0.38f),
                    size = pixelSize,
                    cornerRadius = pixelCorner
                )
                drawRoundRect(
                    color = Color(0xFF00A2FF),
                    topLeft = Offset(w * 0.11f, h * 0.46f),
                    size = pixelSize,
                    cornerRadius = pixelCorner
                )
                drawRoundRect(
                    color = Color(0xFF0088FF),
                    topLeft = Offset(w * 0.17f, h * 0.54f),
                    size = pixelSize,
                    cornerRadius = pixelCorner
                )
            }
        }

        if (showWordmark) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF04153B),
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("Doc")
                    }
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0066FF),
                                    Color(0xFF00B4FF),
                                    Color(0xFF6200EA)
                                )
                            ),
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("Flow")
                    }
                },
                fontSize = wordmarkSize
            )
        }
    }
}

/**
 * Prominent visual indicator displaying the synchronization & storage status of each document:
 * - Cloud icon for synced documents (SYNCED)
 * - Offline/Local icon for local-only or offline-queued documents (LOCAL_ONLY, PENDING_CREATE, PENDING_UPDATE)
 * - Sync error icon if synchronization failed (FAILED)
 */
@Composable
fun SyncStatusVisualIndicator(
    syncStateName: String,
    isOfflineMode: Boolean,
    isOfflineCached: Boolean,
    modifier: Modifier = Modifier
) {
    val parsedState = runCatching { SyncState.valueOf(syncStateName) }.getOrDefault(SyncState.SYNCED)
    val effectiveState = if (isOfflineMode && parsedState == SyncState.SYNCED) {
        SyncState.LOCAL_ONLY
    } else {
        parsedState
    }

    val (icon, label, badgeColor) = when (effectiveState) {
        SyncState.SYNCED -> Triple(
            Icons.Default.CloudDone,
            "Cloud Synced",
            Color(0xFF10B981) // Emerald Crystal
        )
        SyncState.LOCAL_ONLY -> Triple(
            Icons.Default.OfflinePin,
            "Local Only • Offline",
            Color(0xFFF59E0B) // Amber Crystal
        )
        SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE -> Triple(
            Icons.Default.CloudOff,
            "Local Queue • Pending Sync",
            Color(0xFFD97706) // Warm Gold Crystal
        )
        SyncState.PENDING_DELETE -> Triple(
            Icons.Default.CloudSync,
            "Pending Delete",
            Color(0xFF64748B)
        )
        SyncState.FAILED -> Triple(
            Icons.Default.SyncProblem,
            "Sync Error • Local Saved",
            Color(0xFFEF4444)
        )
    }

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = badgeColor.copy(alpha = 0.48f),
                shape = CircleShape
            )
            .testTag("sync_status_badge_${effectiveState.name.lowercase()}"),
        color = badgeColor.copy(alpha = 0.15f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
            if (isOfflineCached && effectiveState == SyncState.SYNCED) {
                Spacer(modifier = Modifier.width(1.dp))
                Icon(
                    imageVector = Icons.Default.OfflinePin,
                    contentDescription = "Cached locally in Room",
                    tint = badgeColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
