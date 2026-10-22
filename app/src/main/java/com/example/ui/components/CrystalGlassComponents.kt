package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.local.SyncState
import com.example.data.sync.BackgroundSyncWorkerState

// Hotlinked URLs from the Crystal Glass HTML reference
const val HOTLINK_DOCFLOW_ICON_URL =
    "https://lh3.googleusercontent.com/aida/AEtjO1W709mb5kCX1Pw2kcKKPhYTfRfhsQnIKZR_hDA93telAuO1m3D6xZwMpO9jTnQVJ5Gx9yj1KmqlPr3og9X2_6Au9GFxp8Ulq9cW_s7cGGTwnJLX34nQh85dvfNmFdUlmZUTMRZaf1hD6UGffo9nreIkD-AcN1JK_4jDb667eCCWRWp3_DQiyISb-6lTIFUhtD6IRoj5DnH6n52haALzJ_eZqU2K2WBWPRUfzEoNuX9fVYe-C-33xx7of2A"

const val HOTLINK_PROFILE_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCd1U5UunoeoxX1QDERbUL9epJXEhvOkhe0XhvF5kypTko3J0C4iKcNewhczN22QvipcnIFdvSPvhPyQodPMWN5-uX5Gh-vUSMnuSaRy82YzsdhHUtIB78_wwoPKIYmTcpGv7Gzy2Qoh4KJcJOv6QuLRTiKVOVvk6_yOowlwHnwFjIHUI5NqAe2zsxRjWrjHz5c1rqGEokyRVFyB7KPHB2ONEVmDQDs7frdFhAapJaJXY5xPWw6lW3V"

const val HOTLINK_COUNSEL_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuAFwXC5iO3aFTf8g1_Wkoihs8y51qOMu1koFGGXZB1_7jWnLcdw3C-_0ZBZU4CrF624-WKrvXIE5Pjdri4bysN9wUShkkaHeYH9SKAjne3KJIDI8Q09ZEWlHreAKFah2aCknbNYo3c15PrD7Msn_HjTZmCj3pL2pJDuqQs2SoqyPxL8Or1ronaUW3znG9bdAcLHz6SzJ_nsYKqS85b6g0aT2CdaleJrJIDi26UJSQ0KJ3DWVviM-mEy"

/**
 * Custom modifier achieving the Apple Crystal Glass aesthetic:
 * Combines translucent frosted glass fill, diagonal optical refraction sheen,
 * and a 1.5dp top-left specular rim highlight over transparent Material3 Surfaces and Cards.
 */
fun Modifier.appleCrystalGlassBlur(
    tintColor: Color? = null,
    isDark: Boolean = true,
    frostAlpha: Float = 1.0f
): Modifier = this

/**
 * Flat minimal DocFlow App Icon tile.
 */
@Composable
fun HotlinkedDocFlowIconBadge(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .size(size)
            .clip(shape),
        shape = shape,
        color = Color(0xFF1F6BFF).copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color(0xFF1F6BFF).copy(alpha = 0.24f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            DocFlowBrandLogo(iconSize = size - 8.dp, showWordmark = false)
        }
    }
}

/**
 * Flat Profile / Author Avatar badge.
 */
@Composable
fun HotlinkedCrystalAvatar(
    imageUrl: String = HOTLINK_PROFILE_AVATAR_URL,
    contentDescription: String = "Profile",
    size: Dp = 34.dp,
    ringColors: List<Color> = listOf(Color(0xFF1F6BFF), Color(0xFF1F6BFF)),
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickMod = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(clickMod),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = Color(0xFF1F6BFF),
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

/**
 * Flat minimal page background (#F0F1F3 in light mode, #0F1113 in dark mode).
 * No glassmorphism, no blur.
 */
@Composable
fun CrystalGlassBackground(
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}

/**
 * Flat minimal Card (#FFFFFF in light mode, #1B1D20 in dark mode) with ~24dp corner radius,
 * no heavy shadows, no blur.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrystalGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    isDark: Boolean = true,
    tintColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val clickMod = when {
        onClick != null && onLongClick != null -> Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
        onLongClick != null -> Modifier.combinedClickable(
            onClick = {},
            onLongClick = onLongClick
        )
        onClick != null -> Modifier.clickable { onClick() }
        else -> Modifier
    }

    Card(
        modifier = modifier
            .clip(shape)
            .then(clickMod),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(content = content)
    }
}

/**
 * Reusable flat Material3 `Surface` with 16dp-24dp corner radius and thin divider border.
 */
@Composable
fun CrystalGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    tintColor: Color? = null,
    frostAlpha: Float = 1.0f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickMod = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickMod),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(content = content)
    }
}

/**
 * Dedicated Apple Crystal Glass Material3 Card displaying WorkManager + Supabase background
 * synchronization status, pending offline item queue, and a one-tap retry/sync button.
 */
@Composable
fun WorkManagerSupabaseSyncCard(
    workerState: BackgroundSyncWorkerState,
    pendingSyncCount: Int,
    isOfflineMode: Boolean,
    onTriggerSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when {
        isOfflineMode -> Color(0xFFF59E0B)
        pendingSyncCount > 0 || workerState.isScheduledOrRunning -> Color(0xFF00F4FE)
        else -> Color(0xFF10B981)
    }

    CrystalGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("workmanager_supabase_sync_card"),
        cornerRadius = 18.dp,
        tintColor = accentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
                ) {
                    Icon(
                        imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudSync,
                        contentDescription = "WorkManager Supabase Sync",
                        tint = accentColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOfflineMode) {
                            "WorkManager Queued ($pendingSyncCount Pending Offline)"
                        } else if (pendingSyncCount > 0) {
                            "Syncing $pendingSyncCount Offline Change(s) to Supabase"
                        } else {
                            "WorkManager • Supabase Cloud Consistent"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = workerState.statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            CrystalGlassSurface(
                cornerRadius = 12.dp,
                tintColor = accentColor,
                onClick = onTriggerSyncNow,
                modifier = Modifier.testTag("workmanager_sync_now_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Now",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isOfflineMode) "Queue Retry" else "Sync Now",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DocFlowBrandLogo(
    iconSize: Dp = 42.dp,
    showWordmark: Boolean = false,
    wordmarkSize: TextUnit = 20.sp,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (iconSize > 0.dp) {
            val shape = RoundedCornerShape(iconSize * 0.28f)
            Surface(
                modifier = Modifier
                    .size(iconSize)
                    .shadow(
                        elevation = 6.dp,
                        shape = shape,
                        spotColor = Color(0xFF00F4FE).copy(alpha = 0.35f)
                    )
                    .clip(shape),
                shape = shape,
                color = Color.Transparent,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color(0xFF00F4FE).copy(alpha = 0.45f)
                        )
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF14244C), Color(0xFF0C1736))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

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
                                    Color(0xFF0A84FF),
                                    Color(0xFF00F4FE),
                                    Color(0xFF7000FF)
                                ),
                                start = Offset(w * 0.25f, h * 0.15f),
                                end = Offset(w * 0.88f, h * 0.85f)
                            )
                        )

                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(w * 0.39f, h * 0.27f),
                            size = Size(w * 0.33f, h * 0.43f),
                            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                        )

                        val foldPath = Path().apply {
                            moveTo(w * 0.61f, h * 0.27f)
                            lineTo(w * 0.72f, h * 0.38f)
                            lineTo(w * 0.61f, h * 0.38f)
                            close()
                        }
                        drawPath(path = foldPath, color = Color(0xFF00F4FE))

                        val barX = w * 0.44f
                        val barH = h * 0.042f
                        drawRoundRect(
                            color = Color(0xFF0A84FF),
                            topLeft = Offset(barX, h * 0.41f),
                            size = Size(w * 0.15f, barH),
                            cornerRadius = CornerRadius(barH / 2, barH / 2)
                        )
                        drawRoundRect(
                            color = Color(0xFF00B4FF),
                            topLeft = Offset(barX, h * 0.485f),
                            size = Size(w * 0.21f, barH),
                            cornerRadius = CornerRadius(barH / 2, barH / 2)
                        )
                        drawRoundRect(
                            color = Color(0xFF00F4FE),
                            topLeft = Offset(barX, h * 0.56f),
                            size = Size(w * 0.17f, barH),
                            cornerRadius = CornerRadius(barH / 2, barH / 2)
                        )
                    }
                }
            }
        }

        if (showWordmark) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("Doc")
                    }
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00F4FE),
                                    Color(0xFF3E90FF),
                                    Color(0xFFA178FF)
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
            Color(0xFF34D399)
        )
        SyncState.LOCAL_ONLY -> Triple(
            Icons.Default.OfflinePin,
            "Local Only • Offline",
            Color(0xFFFBBF24)
        )
        SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE -> Triple(
            Icons.Default.CloudOff,
            "Local Queue • Pending Sync",
            Color(0xFFF59E0B)
        )
        SyncState.PENDING_DELETE -> Triple(
            Icons.Default.CloudSync,
            "Pending Delete",
            Color(0xFF8B91A0)
        )
        SyncState.FAILED -> Triple(
            Icons.Default.SyncProblem,
            "Sync Error • Local Saved",
            Color(0xFFF43F5E)
        )
    }

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .appleCrystalGlassBlur(tintColor = badgeColor, frostAlpha = 0.35f)
            .testTag("sync_status_badge_${effectiveState.name.lowercase()}"),
        color = badgeColor.copy(alpha = 0.14f),
        shape = CircleShape,
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.42f))
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
                modifier = Modifier.size(13.dp)
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
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
