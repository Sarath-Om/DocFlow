package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    frostAlpha: Float = 0.72f
): Modifier = this.drawWithContent {
    val w = size.width
    val h = size.height

    val baseTop = when {
        tintColor != null -> tintColor.copy(alpha = if (isDark) 0.20f else 0.16f)
        isDark -> Color(0xFF1E243B).copy(alpha = frostAlpha)
        else -> Color(0xFF1A2035).copy(alpha = frostAlpha)
    }
    val baseMid = if (isDark) {
        Color(0xFF131728).copy(alpha = frostAlpha * 0.92f)
    } else {
        Color(0xFF141829).copy(alpha = frostAlpha * 0.90f)
    }
    val baseBottom = if (isDark) {
        Color(0xFF0D101D).copy(alpha = frostAlpha * 0.96f)
    } else {
        Color(0xFF101424).copy(alpha = frostAlpha * 0.94f)
    }

    // 1. Translucent frosted glass body
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(baseTop, baseMid, baseBottom),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
    )

    // 2. Diagonal Apple Crystal Glass optical caustic reflection
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.11f),
                (tintColor ?: Color(0xFF00F4FE)).copy(alpha = 0.05f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(w * 0.65f, h * 0.55f)
        )
    )

    // 3. Render child composable content
    drawContent()

    // 4. Specular top-edge crystal rim highlight
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                (tintColor ?: Color(0xFF00F4FE)).copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.06f)
            )
        ),
        start = Offset(w * 0.05f, 1f),
        end = Offset(w * 0.95f, 1f),
        strokeWidth = 1.5f
    )
}

/**
 * Hotlinked DocFlow App Icon in a frosted Crystal Glass Material3 Surface tile, with Canvas fallback.
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
            .clip(shape)
            .appleCrystalGlassBlur(tintColor = Color(0xFF00F4FE), frostAlpha = 0.45f),
        shape = shape,
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = HOTLINK_DOCFLOW_ICON_URL,
                contentDescription = "DocFlow App Icon",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
                loading = {
                    DocFlowBrandLogo(iconSize = size - 8.dp, showWordmark = false)
                },
                error = {
                    DocFlowBrandLogo(iconSize = size - 8.dp, showWordmark = false)
                }
            )
        }
    }
}

/**
 * Hotlinked Profile / Collaborator Avatar with luminous cyan-to-indigo gradient ring.
 */
@Composable
fun HotlinkedCrystalAvatar(
    imageUrl: String = HOTLINK_PROFILE_AVATAR_URL,
    contentDescription: String = "Profile",
    size: Dp = 34.dp,
    ringColors: List<Color> = listOf(Color(0xFF00F4FE), Color(0xFF3E90FF), Color(0xFFA178FF)),
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
        color = Color.Transparent,
        border = BorderStroke(2.dp, Brush.linearGradient(ringColors))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF10131F)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF191B28)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = contentDescription,
                            tint = Color(0xFF00F4FE),
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF191B28)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = contentDescription,
                            tint = Color(0xFF00F4FE),
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                }
            )
        }
    }
}

/**
 * Apple Crystal Glass ambient mesh background built on Material3 `Surface` with
 * hardware Gaussian `Modifier.blur` applied to the chromatic light orbs (`#00F4FE` cyan,
 * `#3E90FF` electric sapphire, `#7000FF` spectral violet, and `#10B981` emerald).
 */
@Composable
fun CrystalGlassBackground(
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF070913),
                Color(0xFF0B0E1C),
                Color(0xFF101426),
                Color(0xFF070913)
            ),
            start = Offset(0f, 0f),
            end = Offset(1200f, 2400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0B0D1A),
                Color(0xFF101425),
                Color(0xFF161B30),
                Color(0xFF0B0D1A)
            ),
            start = Offset(0f, 0f),
            end = Offset(1200f, 2400f)
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(baseGradient)
        ) {
            // Blurred ambient light mesh layer for authentic Apple Crystal Glass depth
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 64.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                val w = size.width
                val h = size.height

                // Top-left cyan glow orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00F4FE).copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(w * 0.12f, h * 0.08f),
                        radius = w * 0.65f
                    ),
                    center = Offset(w * 0.12f, h * 0.08f),
                    radius = w * 0.65f
                )

                // Top-right electric indigo/sapphire orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3E90FF).copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(w * 0.92f, h * 0.26f),
                        radius = w * 0.72f
                    ),
                    center = Offset(w * 0.92f, h * 0.26f),
                    radius = w * 0.72f
                )

                // Lower-left spectral purple orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF7000FF).copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(w * 0.10f, h * 0.68f),
                        radius = w * 0.70f
                    ),
                    center = Offset(w * 0.10f, h * 0.68f),
                    radius = w * 0.70f
                )

                // Bottom-right cyan/emerald ambient orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w * 0.82f, h * 0.90f),
                        radius = w * 0.60f
                    ),
                    center = Offset(w * 0.82f, h * 0.90f),
                    radius = w * 0.60f
                )
            }
            content()
        }
    }
}

/**
 * Apple Crystal Glass Card built using Material3's `Card` and `Surface` with
 * `Modifier.appleCrystalGlassBlur`, transparent container colors, directional 1dp
 * specular rim border, and colored ambient glow.
 */
@Composable
fun CrystalGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    isDark: Boolean = true,
    tintColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)

    val specularBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.34f),
            (tintColor ?: Color(0xFF00F4FE)).copy(alpha = 0.24f),
            Color.White.copy(alpha = 0.08f)
        ),
        start = Offset(0f, 0f),
        end = Offset(520f, 520f)
    )

    val clickMod = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = (tintColor ?: Color(0xFF00F4FE)).copy(alpha = 0.20f),
                spotColor = (tintColor ?: Color(0xFF3E90FF)).copy(alpha = 0.24f)
            )
            .clip(shape)
            .then(clickMod),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Surface(
            modifier = Modifier
                .appleCrystalGlassBlur(
                    tintColor = tintColor,
                    isDark = isDark,
                    frostAlpha = 0.72f
                ),
            shape = shape,
            color = Color.White.copy(alpha = 0.03f),
            contentColor = Color.White,
            tonalElevation = 4.dp,
            border = BorderStroke(width = 1.dp, brush = specularBorderBrush)
        ) {
            Box(content = content)
        }
    }
}

/**
 * Reusable Material3 `Surface` with Apple Crystal Glass blur/refraction and specular rim.
 */
@Composable
fun CrystalGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    tintColor: Color? = null,
    frostAlpha: Float = 0.62f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.30f),
            (tintColor ?: Color(0xFF00F4FE)).copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.06f)
        )
    )
    val clickMod = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Surface(
        modifier = modifier
            .clip(shape)
            .appleCrystalGlassBlur(tintColor = tintColor, frostAlpha = frostAlpha)
            .then(clickMod),
        shape = shape,
        color = Color.White.copy(alpha = 0.04f),
        contentColor = Color.White,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, borderBrush)
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
