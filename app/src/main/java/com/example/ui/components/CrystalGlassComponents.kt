package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SyncState
import com.example.data.sync.BackgroundSyncWorkerState
import com.example.ui.theme.DocFlowTokens

const val HOTLINK_DOCFLOW_ICON_URL = ""
const val HOTLINK_PROFILE_AVATAR_URL = ""
const val HOTLINK_COUNSEL_AVATAR_URL = ""

fun Modifier.appleCrystalGlassBlur(
    tintColor: Color? = null,
    isDark: Boolean = true,
    frostAlpha: Float = 1.0f
): Modifier = this

@Composable
fun HotlinkedDocFlowIconBadge(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val shape = DocFlowTokens.smallShape
    Surface(
        modifier = modifier
            .size(size)
            .clip(shape),
        shape = shape,
        color = DocFlowTokens.accentBlue.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, DocFlowTokens.accentBlue.copy(alpha = 0.24f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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

@Composable
fun HotlinkedCrystalAvatar(
    imageUrl: String = HOTLINK_PROFILE_AVATAR_URL,
    contentDescription: String = "Profile",
    size: Dp = 34.dp,
    ringColors: List<Color> = listOf(DocFlowTokens.accentBlue, DocFlowTokens.accentBlue),
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
        color = DocFlowTokens.surfaceSubtle,
        border = BorderStroke(1.dp, DocFlowTokens.divider),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = DocFlowTokens.accentBlue,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@Composable
fun CrystalGlassBackground(
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DocFlowTokens.background,
        contentColor = DocFlowTokens.textPrimary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DocFlowTokens.background)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrystalGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = DocFlowTokens.cardRadius,
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
            containerColor = DocFlowTokens.surface,
            contentColor = DocFlowTokens.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(DocFlowTokens.dividerThickness, DocFlowTokens.divider)
    ) {
        Box(content = content)
    }
}

@Composable
fun CrystalGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = DocFlowTokens.buttonRadius,
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
        color = DocFlowTokens.surface,
        contentColor = DocFlowTokens.textPrimary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(DocFlowTokens.dividerThickness, DocFlowTokens.divider)
    ) {
        Box(content = content)
    }
}

@Composable
fun WorkManagerSupabaseSyncCard(
    workerState: BackgroundSyncWorkerState,
    pendingSyncCount: Int,
    isOfflineMode: Boolean,
    onTriggerSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when {
        isOfflineMode -> DocFlowTokens.iconOrangePpt
        pendingSyncCount > 0 || workerState.isScheduledOrRunning -> DocFlowTokens.accentBlue
        else -> DocFlowTokens.iconGreenDownload
    }

    CrystalGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("workmanager_supabase_sync_card"),
        cornerRadius = DocFlowTokens.cardRadius
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DocFlowTokens.space4, vertical = DocFlowTokens.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                DocFlowIconCircle(
                    icon = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudSync,
                    circleColor = accentColor,
                    contentDescription = "Local Storage Sync",
                    size = 36.dp,
                    iconSize = 18.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOfflineMode) {
                            "Offline Queue ($pendingSyncCount Pending)"
                        } else if (pendingSyncCount > 0) {
                            "Indexing $pendingSyncCount Change(s)"
                        } else {
                            "Stored Locally on Device"
                        },
                        style = DocFlowTokens.captionBoldStyle,
                        color = DocFlowTokens.textPrimary
                    )
                    Text(
                        text = workerState.statusLabel,
                        style = DocFlowTokens.captionStyle,
                        color = DocFlowTokens.textSecondary
                    )
                }
            }

            CrystalGlassSurface(
                cornerRadius = DocFlowTokens.smallRadius,
                onClick = onTriggerSyncNow,
                modifier = Modifier.testTag("workmanager_sync_now_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = DocFlowTokens.space3, vertical = DocFlowTokens.space2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Refresh Index",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Refresh",
                        style = DocFlowTokens.microBadgeStyle,
                        color = DocFlowTokens.textPrimary
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
        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
    ) {
        if (iconSize > 0.dp) {
            val shape = RoundedCornerShape(iconSize * 0.28f)
            Surface(
                modifier = Modifier
                    .size(iconSize)
                    .clip(shape),
                shape = shape,
                color = DocFlowTokens.accentBlue,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(w * 0.28f, h * 0.20f),
                            size = Size(w * 0.44f, h * 0.60f),
                            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                        )

                        val foldPath = Path().apply {
                            moveTo(w * 0.56f, h * 0.20f)
                            lineTo(w * 0.72f, h * 0.36f)
                            lineTo(w * 0.56f, h * 0.36f)
                            close()
                        }
                        drawPath(path = foldPath, color = DocFlowTokens.iconBlueSummarySafe)

                        val barX = w * 0.35f
                        val barH = h * 0.05f
                        drawRoundRect(
                            color = DocFlowTokens.accentBlue,
                            topLeft = Offset(barX, h * 0.42f),
                            size = Size(w * 0.22f, barH),
                            cornerRadius = CornerRadius(barH / 2, barH / 2)
                        )
                        drawRoundRect(
                            color = DocFlowTokens.accentBlue,
                            topLeft = Offset(barX, h * 0.52f),
                            size = Size(w * 0.30f, barH),
                            cornerRadius = CornerRadius(barH / 2, barH / 2)
                        )
                        drawRoundRect(
                            color = DocFlowTokens.accentBlue,
                            topLeft = Offset(barX, h * 0.62f),
                            size = Size(w * 0.24f, barH),
                            cornerRadius = CornerRadius(barH / 2, barH / 2)
                        )
                    }
                }
            }
        }

        if (showWordmark) {
            Text(
                text = "DocFlow",
                fontWeight = FontWeight.Bold,
                fontSize = wordmarkSize,
                color = DocFlowTokens.textPrimary
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
            "On Device",
            DocFlowTokens.iconGreenDownload
        )
        SyncState.LOCAL_ONLY -> Triple(
            Icons.Default.OfflinePin,
            "Local Only • Offline",
            DocFlowTokens.accentBlue
        )
        SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE -> Triple(
            Icons.Default.CloudOff,
            "Saved Locally",
            DocFlowTokens.accentBlue
        )
        SyncState.PENDING_DELETE -> Triple(
            Icons.Default.CloudSync,
            "Pending Delete",
            DocFlowTokens.iconGrayTrash
        )
        SyncState.FAILED -> Triple(
            Icons.Default.SyncProblem,
            "Local Saved",
            DocFlowTokens.iconRedPdf
        )
    }

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .testTag("sync_status_badge_${effectiveState.name.lowercase()}"),
        color = badgeColor.copy(alpha = 0.14f),
        shape = CircleShape,
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DocFlowTokens.space2, vertical = DocFlowTokens.space1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = badgeColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = DocFlowTokens.microBadgeStyle,
                color = badgeColor
            )
            if (isOfflineCached && effectiveState == SyncState.SYNCED) {
                Spacer(modifier = Modifier.width(1.dp))
                Icon(
                    imageVector = Icons.Default.OfflinePin,
                    contentDescription = "Stored locally in Room",
                    tint = badgeColor,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
