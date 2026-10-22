package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DocFlowTokens

/**
 * Shared component library for DocFlow.
 * Reused across Recent, Tools, Manage, Document Viewer, Scanner, Search, Settings,
 * Private Safe, Recently deleted, dialogs, and bottom sheets.
 */

data class DocFlowHeaderAction(
    val icon: ImageVector,
    val contentDescription: String,
    val testTag: String,
    val tint: Color? = null,
    val isActive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * 1. ScreenHeader: large left-aligned title (40sp bold), optional subtitle,
 * right-side icon buttons (minimum 48dp touch targets).
 */
@Composable
fun DocFlowScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: List<DocFlowHeaderAction> = emptyList()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DocFlowTokens.screenSideMargin,
                end = DocFlowTokens.space2,
                top = DocFlowTokens.space3,
                bottom = DocFlowTokens.space2
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DocFlowTokens.largeTitleStyle,
                color = DocFlowTokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                Text(
                    text = subtitle,
                    style = DocFlowTokens.captionStyle,
                    color = DocFlowTokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actions.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
            ) {
                actions.forEach { action ->
                    IconButton(
                        onClick = action.onClick,
                        modifier = Modifier
                            .size(DocFlowTokens.minTouchTarget)
                            .clip(CircleShape)
                            .background(
                                if (action.isActive) DocFlowTokens.accentBlue.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .testTag(action.testTag)
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.contentDescription,
                            tint = action.tint
                                ?: if (action.isActive) DocFlowTokens.accentBlue else DocFlowTokens.textPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. Card: Flat white (#FFFFFF) / dark (#1B1D20) card with 24dp corner radius,
 * 0dp shadow elevation, 16-20dp padding.
 */
@Composable
fun DocFlowCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(
        horizontal = DocFlowTokens.cardPaddingMin,
        vertical = DocFlowTokens.cardPaddingMin
    ),
    borderColor: Color? = null,
    containerColor: Color = DocFlowTokens.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(DocFlowTokens.cardShape)
            .then(clickableModifier),
        shape = DocFlowTokens.cardShape,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = borderColor?.let { BorderStroke(DocFlowTokens.dividerThickness, it) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content
        )
    }
}

/**
 * 3. IconCircle: Colored circular icon badge used in Tools, Manage, and Viewer sheets.
 */
@Composable
fun DocFlowIconCircle(
    icon: ImageVector,
    circleColor: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    solidFill: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (solidFill) circleColor else circleColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (solidFill) Color.White else circleColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * 4. FileFormatThumbnail: Colored file icon or first-page thumbnail with 8dp corner radius.
 */
@Composable
fun DocFlowFileFormatBadge(
    fileType: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val badgeColor = DocFlowTokens.fileFormatColor(fileType)
    val badgeText = DocFlowTokens.fileFormatShortBadge(fileType)
    Box(
        modifier = modifier
            .size(size)
            .clip(DocFlowTokens.thumbnailShape)
            .background(badgeColor.copy(alpha = 0.14f))
            .border(1.dp, badgeColor.copy(alpha = 0.30f), DocFlowTokens.thumbnailShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            style = DocFlowTokens.microBadgeStyle,
            color = badgeColor
        )
    }
}

/**
 * 5. ListRow: Icon or thumbnail + title (18sp medium, max 2 lines, ellipsized) +
 * secondary line (14sp gray) + chevron or trailing content. Minimum 48dp touch target.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocFlowListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showChevron: Boolean = false,
    showBottomDivider: Boolean = false,
    selected: Boolean = false,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .background(
            if (selected) DocFlowTokens.accentBlue.copy(alpha = 0.08f)
            else Color.Transparent
        )
        .let { base ->
            if (testTag != null) base.testTag(testTag) else base
        }
        .let { base ->
            if (onClick != null || onLongClick != null) {
                base.combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = onLongClick
                )
            } else {
                base
            }
        }
        .padding(horizontal = DocFlowTokens.cardPaddingMin, vertical = DocFlowTokens.space3)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
        ) {
            if (leading != null) {
                leading()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DocFlowTokens.rowTitleStyle,
                    color = DocFlowTokens.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = DocFlowTokens.captionStyle,
                        color = DocFlowTokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DocFlowTokens.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (showBottomDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(
                    start = if (leading != null) 72.dp else DocFlowTokens.cardPaddingMin,
                    end = DocFlowTokens.cardPaddingMin
                ),
                thickness = DocFlowTokens.dividerThickness,
                color = DocFlowTokens.divider
            )
        }
    }
}

/**
 * 6. SectionLabel: 20sp regular section header with optional trailing action.
 */
@Composable
fun DocFlowSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = DocFlowTokens.screenSideMargin,
                vertical = DocFlowTokens.space2
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = DocFlowTokens.sectionLabelStyle,
            color = DocFlowTokens.textPrimary
        )
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * 7. DateGroupHeader: "17 Oct | 1 item" with collapse chevron (48dp touch target).
 */
@Composable
fun DocFlowDateGroupHeader(
    dateLabel: String,
    itemCount: Int,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countText = if (itemCount == 1) "1 item" else "$itemCount items"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DocFlowTokens.minTouchTarget)
            .clickable(role = Role.Button, onClick = onToggleCollapse)
            .padding(horizontal = DocFlowTokens.screenSideMargin, vertical = DocFlowTokens.space1),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
        ) {
            Text(
                text = dateLabel,
                style = DocFlowTokens.bodyMediumStyle,
                color = DocFlowTokens.textPrimary
            )
            Text(
                text = "|",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Text(
                text = countText,
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
        }
        Box(
            modifier = Modifier.size(DocFlowTokens.minTouchTarget),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = if (isCollapsed) "Expand $dateLabel" else "Collapse $dateLabel",
                tint = DocFlowTokens.textSecondary
            )
        }
    }
}

/**
 * 8. FilterTabs: Horizontal filter tabs with a 3dp #1F6BFF underline on the active tab.
 */
@Composable
fun <T> DocFlowFilterTabs(
    tabs: List<T>,
    selectedTab: T,
    labelProvider: (T) -> String,
    onSelectTab: (T) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "filter_tab"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = DocFlowTokens.screenSideMargin),
        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space5)
    ) {
        tabs.forEach { tab ->
            val label = labelProvider(tab)
            val isSelected = tab == selectedTab
            Column(
                modifier = Modifier
                    .heightIn(min = DocFlowTokens.minTouchTarget)
                    .clickable(role = Role.Tab) { onSelectTab(tab) }
                    .padding(top = DocFlowTokens.space2)
                    .testTag("${testTagPrefix}_${label.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = if (isSelected) {
                        DocFlowTokens.bodyMediumStyle.copy(fontWeight = FontWeight.Bold)
                    } else {
                        DocFlowTokens.bodyStyle
                    },
                    color = if (isSelected) DocFlowTokens.textPrimary else DocFlowTokens.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(DocFlowTokens.pillShape)
                        .background(if (isSelected) DocFlowTokens.accentBlue else Color.Transparent)
                )
            }
        }
    }
}

/**
 * 9. BottomNav: 3 tabs (Recent, Tools, Manage), white/surface bar with a thin top divider.
 * Active tab: filled icon + dark bold label; inactive: outline icon + gray label.
 */
enum class DocFlowNavTab(
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val testTag: String
) {
    RECENT("Recent", Icons.Filled.History, Icons.Outlined.History, "bottom_nav_recent"),
    TOOLS("Tools", Icons.Filled.GridView, Icons.Outlined.GridView, "bottom_nav_tools"),
    MANAGE("Manage", Icons.Filled.Folder, Icons.Outlined.Folder, "bottom_nav_manage")
}

@Composable
fun DocFlowBottomNav(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DocFlowTokens.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = DocFlowTokens.dividerThickness,
                color = DocFlowTokens.divider
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocFlowNavTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedIndex == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = DocFlowTokens.minTouchTarget)
                            .clickable(role = Role.Tab) { onSelectIndex(index) }
                            .padding(vertical = DocFlowTokens.space2)
                            .testTag(tab.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                            contentDescription = tab.label,
                            tint = if (isSelected) DocFlowTokens.textPrimary else DocFlowTokens.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = DocFlowTokens.captionStyle.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) DocFlowTokens.textPrimary else DocFlowTokens.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 10. FAB: Accent blue (#1F6BFF) circle with white icon, flat elevation.
 */
@Composable
fun DocFlowFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String = "Primary action",
    testTag: String = "docflow_fab"
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(58.dp)
            .testTag(testTag),
        shape = CircleShape,
        containerColor = DocFlowTokens.accentBlue,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * 11. BottomSheet: Shared ModalBottomSheet with 28dp top corners and flat surface color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocFlowBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = DocFlowTokens.sheetTopShape,
        containerColor = DocFlowTokens.surface,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = DocFlowTokens.cardPaddingMax,
                    end = DocFlowTokens.cardPaddingMax,
                    bottom = DocFlowTokens.space6
                )
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = DocFlowTokens.sectionLabelStyle.copy(fontWeight = FontWeight.Bold),
                    color = DocFlowTokens.textPrimary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))
            }
            content()
        }
    }
}

/**
 * 12. Dialog: Shared flat dialog with 24dp radius.
 */
@Composable
fun DocFlowDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = DocFlowTokens.cardShape,
        containerColor = DocFlowTokens.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = title,
                style = DocFlowTokens.sectionLabelStyle.copy(fontWeight = FontWeight.Bold),
                color = DocFlowTokens.textPrimary
            )
        },
        text = {
            Column(content = content)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                shape = DocFlowTokens.pillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) DocFlowTokens.iconRedPdf else DocFlowTokens.accentBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier.heightIn(min = DocFlowTokens.minTouchTarget)
            ) {
                Text(confirmText, style = DocFlowTokens.bodyMediumStyle)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.heightIn(min = DocFlowTokens.minTouchTarget)
            ) {
                Text(dismissText, color = DocFlowTokens.textSecondary, style = DocFlowTokens.bodyStyle)
            }
        }
    )
}

/**
 * 13. Toast: Flat status banner for user feedback.
 */
@Composable
fun DocFlowToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(DocFlowTokens.standardTween()),
        exit = fadeOut(DocFlowTokens.standardTween()),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DocFlowTokens.screenSideMargin, vertical = DocFlowTokens.space2),
            shape = DocFlowTokens.buttonShape,
            color = if (isError) DocFlowTokens.iconRedPdf else DocFlowTokens.textPrimary,
            tonalElevation = 0.dp
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
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) Color.White else DocFlowTokens.iconGreenDownload,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = message.orEmpty(),
                        style = DocFlowTokens.captionStyle,
                        color = if (isError) Color.White else DocFlowTokens.surface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notification",
                        tint = if (isError) Color.White else DocFlowTokens.surface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 14. EmptyState: Flat white card with colored icon circle, title, subtitle, and optional pill button.
 */
@Composable
fun DocFlowEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconCircleColor: Color = DocFlowTokens.accentBlue,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    DocFlowCard(
        modifier = modifier.padding(horizontal = DocFlowTokens.screenSideMargin),
        padding = PaddingValues(DocFlowTokens.space6)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
        ) {
            DocFlowIconCircle(
                icon = icon,
                circleColor = iconCircleColor,
                contentDescription = null,
                size = 56.dp,
                iconSize = 28.dp
            )
            Text(
                text = title,
                style = DocFlowTokens.rowTitleStyle.copy(fontWeight = FontWeight.Bold),
                color = DocFlowTokens.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary,
                textAlign = TextAlign.Center
            )
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onActionClick,
                        shape = DocFlowTokens.pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DocFlowTokens.accentBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.heightIn(min = DocFlowTokens.minTouchTarget)
                    ) {
                        Text(actionLabel, style = DocFlowTokens.bodyMediumStyle)
                    }
                    if (!secondaryActionLabel.isNullOrBlank() && onSecondaryActionClick != null) {
                        OutlinedButton(
                            onClick = onSecondaryActionClick,
                            shape = DocFlowTokens.pillShape,
                            border = BorderStroke(1.dp, DocFlowTokens.divider),
                            modifier = Modifier.heightIn(min = DocFlowTokens.minTouchTarget)
                        ) {
                            Text(
                                text = secondaryActionLabel,
                                style = DocFlowTokens.bodyMediumStyle,
                                color = DocFlowTokens.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 15. SkeletonLoader: Flat placeholder rows for loading states.
 */
@Composable
fun DocFlowSkeletonLoader(
    rowCount: Int = 4,
    modifier: Modifier = Modifier
) {
    DocFlowCard(
        modifier = modifier.padding(horizontal = DocFlowTokens.screenSideMargin),
        padding = PaddingValues(DocFlowTokens.cardPaddingMin)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space4)) {
            repeat(rowCount) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Loading item ${index + 1}" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(DocFlowTokens.thumbnailShape)
                            .background(DocFlowTokens.divider)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .height(16.dp)
                                .clip(DocFlowTokens.smallShape)
                                .background(DocFlowTokens.divider)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(12.dp)
                                .clip(DocFlowTokens.smallShape)
                                .background(DocFlowTokens.surfaceSubtle)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 16. ErrorState: Flat error card with primary retry and optional fallback action.
 */
@Composable
fun DocFlowErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Retry",
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    DocFlowEmptyState(
        icon = Icons.Default.ErrorOutline,
        title = title,
        subtitle = message,
        iconCircleColor = DocFlowTokens.iconRedPdf,
        actionLabel = retryLabel,
        onActionClick = onRetry,
        secondaryActionLabel = secondaryLabel,
        onSecondaryActionClick = onSecondaryClick,
        modifier = modifier
    )
}

/**
 * 17. PermissionState: Permission denied card with "Open settings" or grant button + fallback.
 */
@Composable
fun DocFlowPermissionState(
    title: String,
    message: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackActionLabel: String? = null,
    onFallbackAction: (() -> Unit)? = null
) {
    DocFlowEmptyState(
        icon = Icons.Default.Lock,
        title = title,
        subtitle = message,
        iconCircleColor = DocFlowTokens.iconBlueSummarySafe,
        actionLabel = primaryActionLabel,
        onActionClick = onPrimaryAction,
        secondaryActionLabel = fallbackActionLabel,
        onSecondaryActionClick = onFallbackAction,
        modifier = modifier
    )
}

/**
 * 18. ProgressBar: Flat progress bar with optional percentage/caption.
 */
@Composable
fun DocFlowProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
    ) {
        if (!label.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = DocFlowTokens.captionStyle,
                    color = DocFlowTokens.textSecondary
                )
                if (progress != null) {
                    Text(
                        text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        style = DocFlowTokens.captionBoldStyle,
                        color = DocFlowTokens.accentBlue
                    )
                }
            }
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(DocFlowTokens.pillShape),
                color = DocFlowTokens.accentBlue,
                trackColor = DocFlowTokens.divider
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(DocFlowTokens.pillShape),
                color = DocFlowTokens.accentBlue,
                trackColor = DocFlowTokens.divider
            )
        }
    }
}

/**
 * Primary & Secondary Pill Buttons in the shared design system.
 */
@Composable
fun DocFlowPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPill: Boolean = true,
    leadingIcon: ImageVector? = null,
    testTag: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = if (isPill) DocFlowTokens.pillShape else DocFlowTokens.buttonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = DocFlowTokens.accentBlue,
            contentColor = Color.White,
            disabledContainerColor = DocFlowTokens.divider,
            disabledContentColor = DocFlowTokens.textSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        modifier = modifier
            .heightIn(min = DocFlowTokens.minTouchTarget)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(DocFlowTokens.space2))
        }
        Text(text = text, style = DocFlowTokens.bodyMediumStyle)
    }
}

@Composable
fun DocFlowSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPill: Boolean = true,
    leadingIcon: ImageVector? = null,
    testTag: String? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = if (isPill) DocFlowTokens.pillShape else DocFlowTokens.buttonShape,
        border = BorderStroke(1.dp, DocFlowTokens.divider),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = DocFlowTokens.surface,
            contentColor = DocFlowTokens.textPrimary
        ),
        modifier = modifier
            .heightIn(min = DocFlowTokens.minTouchTarget)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        if (content != null) {
            content()
        } else {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = DocFlowTokens.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(DocFlowTokens.space2))
            }
            Text(text = text, style = DocFlowTokens.bodyMediumStyle, color = DocFlowTokens.textPrimary)
        }
    }
}
