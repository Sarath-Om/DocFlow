package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedDocumentEntity
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.data.util.TranslationLanguage
import com.example.ui.components.DocFlowBottomSheet
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDialog
import com.example.ui.components.DocFlowDivider
import com.example.ui.components.DocFlowEmptyState
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowProgressBar
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowSectionLabel
import com.example.ui.components.DocFlowToast
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocFlowViewModel
import com.example.ui.viewmodel.ThemeMode

private data class StorageCategorySlice(
    val key: String,
    val label: String,
    val bytes: Long,
    val color: Color,
    val files: List<CachedDocumentEntity>
)

@Composable
fun SettingsScreen(
    viewModel: DocFlowViewModel,
    palette: DocFlowPalette,
    onBack: () -> Unit,
    onOpenStorageDashboard: () -> Unit
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val cachedMap by viewModel.cachedDocumentsMap.collectAsState()
    val deletedDocs by viewModel.deletedDocuments.collectAsState()
    val privateSafeDocs by viewModel.privateSafeDocuments.collectAsState()

    var primaryLanguage by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getPrimaryLanguage(context))
    }
    var downloadedLangs by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context))
    }
    var hasPin by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.hasPrivateSafePin(context))
    }
    var biometricEnabled by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.isBiometricUnlockEnabled(context))
    }
    var appLockEnabled by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.isAppLockEnabled(context))
    }
    var lockTimeoutMinutes by remember {
        mutableIntStateOf(OnDeviceAiAndBackupEngine.getLockTimeoutMinutes(context))
    }
    var cacheBytes by remember {
        mutableLongStateOf(OnDeviceAiAndBackupEngine.calculateCacheAndTempBytes(context))
    }

    // Interactive Modals & Bottom Sheets
    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showLockTimeoutSheet by remember { mutableStateOf(false) }
    var showLicensesSheet by remember { mutableStateOf(false) }
    var showPrivacyOfflineFallbackSheet by remember { mutableStateOf(false) }
    var showTermsOfflineFallbackSheet by remember { mutableStateOf(false) }
    var selectedCategoryForDrilldown by remember { mutableStateOf<StorageCategorySlice?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    var pinStep by remember { mutableStateOf("ENTER_CURRENT") } // ENTER_CURRENT, SET_NEW, CONFIRM_NEW
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf<String?>(null) }

    // On-device storage breakdown calculation (Strictly local — Stored on this device)
    val allDocs = cachedMap.values.toList()
    val activeNormalDocs = allDocs.filter { !it.isDeleted && !it.isPrivateSafe }

    val pdfDocs = activeNormalDocs.filter {
        it.fileType.equals("pdf", true) || it.fileType.equals("ofd", true)
    }
    val wordDocs = activeNormalDocs.filter {
        val ft = it.fileType.lowercase()
        ft in setOf("docx", "doc", "xlsx", "xls", "csv", "pptx", "ppt", "txt", "md", "note")
    }
    val imageScanDocs = activeNormalDocs.filter {
        val ft = it.fileType.lowercase()
        ft in setOf("pic", "scanned", "png", "jpg", "jpeg", "webp", "heic")
    }

    val pdfBytes = pdfDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
    val wordBytes = wordDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
    val imageBytes = imageScanDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
    val safeBytes = privateSafeDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
    val deletedBytes = deletedDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
    val totalUsedByDocFlow = pdfBytes + wordBytes + imageBytes + safeBytes + deletedBytes + cacheBytes
    val deviceFreeBytes = remember(totalUsedByDocFlow) {
        OnDeviceAiAndBackupEngine.getDeviceFreeSpaceBytes(context)
    }

    val storageCategories = listOf(
        StorageCategorySlice(
            key = "pdf",
            label = "PDF documents",
            bytes = pdfBytes,
            color = DocFlowTokens.IconCircleRed,
            files = pdfDocs.sortedByDescending { it.fileSize }
        ),
        StorageCategorySlice(
            key = "word",
            label = "Word & Office documents",
            bytes = wordBytes,
            color = DocFlowTokens.AccentBlue,
            files = wordDocs.sortedByDescending { it.fileSize }
        ),
        StorageCategorySlice(
            key = "images",
            label = "Images / Scans",
            bytes = imageBytes,
            color = DocFlowTokens.IconCircleTeal,
            files = imageScanDocs.sortedByDescending { it.fileSize }
        ),
        StorageCategorySlice(
            key = "safe",
            label = "Private Safe",
            bytes = safeBytes,
            color = DocFlowTokens.IconCircleBlue,
            files = privateSafeDocs.sortedByDescending { it.fileSize }
        ),
        StorageCategorySlice(
            key = "deleted",
            label = "Recently deleted",
            bytes = deletedBytes,
            color = DocFlowTokens.IconCircleGray,
            files = deletedDocs.sortedByDescending { it.fileSize }
        )
    )

    fun openExternalUrl(url: String, onFallback: () -> Unit) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            onFallback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("settings_screen_root")
    ) {
        // Top Header with Back Button + 40sp Bold Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = DocFlowTokens.space2,
                    end = DocFlowTokens.screenHorizontalMargin,
                    top = DocFlowTokens.space3,
                    bottom = DocFlowTokens.space2
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(DocFlowTokens.minTouchTarget)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .testTag("settings_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(DocFlowTokens.space1))
            Column {
                Text(
                    text = "Settings",
                    style = DocFlowTokens.largeTitleStyle,
                    color = palette.textPrimary
                )
                Text(
                    text = "Stored on this device • Offline workspace",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
            }
        }

        if (toastMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space1)
            ) {
                DocFlowToast(message = toastMessage!!)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_lazy_column"),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // =================================================================
            // SECTION 1: GENERAL (Theme with Light Mode default, Language)
            // =================================================================
            item {
                DocFlowSectionLabel(text = "General", palette = palette)
                DocFlowCard(palette = palette, contentPadding = PaddingValues(vertical = DocFlowTokens.space1)) {
                    // Row 1: Theme (Light / Dark / System)
                    DocFlowListRow(
                        title = "Theme",
                        subtitle = when (themeMode) {
                            ThemeMode.LIGHT -> "Light Mode (Active • #F0F1F3 surface)"
                            ThemeMode.DARK -> "Dark Mode (Active • #0F1113 surface)"
                            ThemeMode.SYSTEM -> "Follow System Theme"
                        },
                        palette = palette,
                        onClick = { showThemeSheet = true },
                        modifier = Modifier.testTag("settings_row_theme"),
                        leading = {
                            DocFlowIconCircle(
                                icon = if (themeMode == ThemeMode.DARK) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                circleColor = DocFlowTokens.AccentBlue
                            )
                        },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                            ) {
                                ThemeQuickChip(
                                    label = "Light",
                                    selected = themeMode == ThemeMode.LIGHT,
                                    palette = palette,
                                    testTag = "theme_chip_light",
                                    onClick = {
                                        viewModel.setThemeMode(ThemeMode.LIGHT)
                                        toastMessage = "Switched to Light Mode"
                                    }
                                )
                                ThemeQuickChip(
                                    label = "Dark",
                                    selected = themeMode == ThemeMode.DARK,
                                    palette = palette,
                                    testTag = "theme_chip_dark",
                                    onClick = {
                                        viewModel.setThemeMode(ThemeMode.DARK)
                                        toastMessage = "Switched to Dark Mode"
                                    }
                                )
                                ThemeQuickChip(
                                    label = "System",
                                    selected = themeMode == ThemeMode.SYSTEM,
                                    palette = palette,
                                    testTag = "theme_chip_system",
                                    onClick = {
                                        viewModel.setThemeMode(ThemeMode.SYSTEM)
                                        toastMessage = "Following System Theme"
                                    }
                                )
                            }
                        }
                    )

                    DocFlowDivider(
                        palette = palette,
                        modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                    )

                    // Row 2: Language (App UI, OCR & Offline Translation Language Packs)
                    DocFlowListRow(
                        title = "Language",
                        subtitle = "${primaryLanguage.displayName} (${primaryLanguage.nativeName}) • ${downloadedLangs.size} offline pack(s) on this device",
                        palette = palette,
                        onClick = { showLanguageSheet = true },
                        showChevron = true,
                        modifier = Modifier.testTag("settings_row_language"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Language,
                                circleColor = DocFlowTokens.IconCircleTeal
                            )
                        }
                    )
                }
            }

            // =================================================================
            // SECTION 2: STORAGE ("On this device" breakdown, largest files, Clear cache)
            // =================================================================
            item {
                Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                DocFlowSectionLabel(text = "Storage • On this device", palette = palette)
                DocFlowCard(palette = palette, contentPadding = PaddingValues(vertical = DocFlowTokens.space2)) {
                    // Top Summary & Multi-segment Breakdown Bar ("On this device")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DocFlowTokens.cardPaddingMin, vertical = DocFlowTokens.space1)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "On this device",
                                    style = DocFlowTokens.rowTitleStyle,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Stored on this device • Free space: ${formatStorageBytes(deviceFreeBytes)}",
                                    style = DocFlowTokens.captionStyle,
                                    color = palette.textSecondary
                                )
                            }
                            Text(
                                text = formatStorageBytes(totalUsedByDocFlow),
                                style = DocFlowTokens.rowTitleStyle,
                                fontWeight = FontWeight.Bold,
                                color = DocFlowTokens.AccentBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                        // Breakdown Horizontal Bar
                        val barTotal = totalUsedByDocFlow.coerceAtLeast(1L).toFloat()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(DocFlowTokens.pillButtonShape)
                                .background(palette.divider)
                                .testTag("settings_storage_breakdown_bar")
                        ) {
                            storageCategories.forEach { cat ->
                                val weightVal = (cat.bytes.toFloat() / barTotal).coerceAtLeast(if (cat.bytes > 0L) 0.04f else 0f)
                                if (weightVal > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(weightVal)
                                            .height(10.dp)
                                            .background(cat.color)
                                    )
                                }
                            }
                            val cacheWeight = (cacheBytes.toFloat() / barTotal).coerceAtLeast(if (cacheBytes > 0L) 0.04f else 0f)
                            if (cacheWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(cacheWeight)
                                        .height(10.dp)
                                        .background(DocFlowTokens.IconCircleGreen)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                    DocFlowDivider(
                        palette = palette,
                        modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                    )

                    // Full Storage Dashboard row
                    DocFlowListRow(
                        title = "Storage usage stats (On this device)",
                        subtitle = "${formatStorageBytes(totalUsedByDocFlow)} used across ${allDocs.size} local document(s) • Stored on this device",
                        palette = palette,
                        onClick = onOpenStorageDashboard,
                        showChevron = true,
                        modifier = Modifier.testTag("settings_row_storage_stats"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Storage,
                                circleColor = DocFlowTokens.AccentBlue
                            )
                        }
                    )

                    // Category rows: tap any row to inspect largest files in that category with a delete option
                    storageCategories.forEach { slice ->
                        DocFlowDivider(
                            palette = palette,
                            modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                        )
                        DocFlowListRow(
                            title = slice.label,
                            subtitle = "${formatStorageBytes(slice.bytes)} • ${slice.files.size} file(s) • Tap to view largest files",
                            palette = palette,
                            onClick = { selectedCategoryForDrilldown = slice },
                            showChevron = true,
                            modifier = Modifier.testTag("settings_storage_cat_${slice.key}"),
                            leading = {
                                DocFlowIconCircle(
                                    icon = when (slice.key) {
                                        "pdf" -> Icons.Filled.PictureAsPdf
                                        "word" -> Icons.Filled.Description
                                        "images" -> Icons.Filled.Image
                                        "safe" -> Icons.Filled.Lock
                                        else -> Icons.Filled.DeleteOutline
                                    },
                                    circleColor = slice.color
                                )
                            }
                        )
                    }

                    DocFlowDivider(
                        palette = palette,
                        modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                    )

                    // Clear Cache (never deletes user documents)
                    DocFlowListRow(
                        title = "Clear cache",
                        subtitle = "${formatStorageBytes(cacheBytes)} temporary thumbnails • Never deletes user documents",
                        palette = palette,
                        onClick = { showClearCacheConfirm = true },
                        modifier = Modifier.testTag("settings_row_clear_cache"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.CleaningServices,
                                circleColor = DocFlowTokens.IconCircleGreen
                            )
                        },
                        trailing = {
                            Text(
                                text = "Clear",
                                style = DocFlowTokens.captionStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = DocFlowTokens.AccentBlue
                            )
                        }
                    )
                }
            }

            // =================================================================
            // SECTION 3: DEVICE MANAGEMENT (Scoped Accounting to Device)
            // =================================================================
            item {
                Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                DocFlowSectionLabel(text = "Device", palette = palette)
                DocFlowCard(palette = palette, contentPadding = PaddingValues(vertical = DocFlowTokens.space1)) {
                    DocFlowListRow(
                        title = "Device Storage & File Accounting",
                        subtitle = "${allDocs.size} total file(s) • ${formatStorageBytes(deviceFreeBytes)} free on this device • Stored on this device",
                        palette = palette,
                        onClick = onOpenStorageDashboard,
                        showChevron = true,
                        modifier = Modifier.testTag("settings_row_device_accounting"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Devices,
                                circleColor = DocFlowTokens.IconCircleBlue
                            )
                        }
                    )
                }
            }

            // =================================================================
            // SECTION 4: SECURITY (Biometric Toggle, PIN Settings, App Lock)
            // =================================================================
            item {
                Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                DocFlowSectionLabel(text = "Security", palette = palette)
                DocFlowCard(palette = palette, contentPadding = PaddingValues(vertical = DocFlowTokens.space1)) {
                    // Row 1: Biometric Toggle
                    DocFlowListRow(
                        title = "Biometric Toggle",
                        subtitle = if (biometricEnabled) {
                            "Enabled • Fingerprint / Face unlock for Private Safe"
                        } else {
                            "Disabled • Require PIN only"
                        },
                        palette = palette,
                        onClick = {
                            val next = !biometricEnabled
                            biometricEnabled = next
                            OnDeviceAiAndBackupEngine.setBiometricUnlockEnabled(context, next)
                            viewModel.toggleBiometricVault()
                            toastMessage = if (next) "Biometric unlock enabled" else "Biometric unlock disabled"
                        },
                        modifier = Modifier.testTag("settings_row_biometric"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Fingerprint,
                                circleColor = DocFlowTokens.IconCircleTeal
                            )
                        },
                        trailing = {
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { next ->
                                    biometricEnabled = next
                                    OnDeviceAiAndBackupEngine.setBiometricUnlockEnabled(context, next)
                                    viewModel.toggleBiometricVault()
                                    toastMessage = if (next) "Biometric unlock enabled" else "Biometric unlock disabled"
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DocFlowTokens.AccentBlue
                                ),
                                modifier = Modifier.testTag("settings_switch_biometric")
                            )
                        }
                    )

                    DocFlowDivider(
                        palette = palette,
                        modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                    )

                    // Row 2: PIN Settings
                    DocFlowListRow(
                        title = "PIN settings",
                        subtitle = if (hasPin) {
                            "Custom AES-256 Private Safe PIN configured • Tap to change or reset"
                        } else {
                            "Default PIN (1234) active • Tap to set custom 4–8 digit PIN"
                        },
                        palette = palette,
                        onClick = {
                            currentPinInput = ""
                            newPinInput = ""
                            confirmPinInput = ""
                            pinErrorText = null
                            pinStep = if (hasPin) "ENTER_CURRENT" else "SET_NEW"
                            showPinDialog = true
                        },
                        showChevron = true,
                        modifier = Modifier.testTag("settings_row_pin"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Password,
                                circleColor = DocFlowTokens.AccentBlue
                            )
                        }
                    )

                    DocFlowDivider(
                        palette = palette,
                        modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                    )

                    // Row 3: App Lock on Launch & Timeout
                    DocFlowListRow(
                        title = "Auto-lock Private Safe",
                        subtitle = if (appLockEnabled) {
                            "Require unlock after $lockTimeoutMinutes min in background"
                        } else {
                            "Lock immediately when leaving Private Safe"
                        },
                        palette = palette,
                        onClick = { showLockTimeoutSheet = true },
                        showChevron = true,
                        modifier = Modifier.testTag("settings_row_auto_lock"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Timer,
                                circleColor = DocFlowTokens.IconCircleGray
                            )
                        }
                    )
                }
            }

            // =================================================================
            // SECTION 5: ABOUT (App Version, Privacy Policy, Terms of Service)
            // =================================================================
            item {
                Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                DocFlowSectionLabel(text = "About", palette = palette)
                DocFlowCard(palette = palette, contentPadding = PaddingValues(vertical = DocFlowTokens.space1)) {
                    // Row 1: App Version
                    DocFlowListRow(
                        title = "App Version",
                        subtitle = "DocFlow v${OnDeviceAiAndBackupEngine.APP_VERSION_NAME} (Build ${OnDeviceAiAndBackupEngine.APP_VERSION_BUILD}) • Stored on this device",
                        palette = palette,
                        onClick = { showLicensesSheet = true },
                        modifier = Modifier.testTag("settings_row_version"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Info,
                                circleColor = DocFlowTokens.AccentBlue
                            )
                        },
                        trailing = {
                            Text(
                                text = "v${OnDeviceAiAndBackupEngine.APP_VERSION_NAME}",
                                style = DocFlowTokens.captionStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textSecondary
                            )
                        }
                    )

                    DocFlowDivider(
                        palette = palette,
                        modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                    )

                    // Row 2: Privacy Policy Link (https://doceflow.site.je/privacy/)
                    DocFlowListRow(
                        title = "Privacy Policy",
                        subtitle = OnDeviceAiAndBackupEngine.PRIVACY_POLICY_URL,
                        palette = palette,
                        onClick = {
                            openExternalUrl(
                                url = OnDeviceAiAndBackupEngine.PRIVACY_POLICY_URL,
                                onFallback = { showPrivacyOfflineFallbackSheet = true }
                            )
                        },
                        modifier = Modifier.testTag("settings_row_privacy_policy"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Policy,
                                circleColor = DocFlowTokens.IconCircleGreen
                            )
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = "Open Privacy Policy",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    DocFlowDivider(
                        palette = palette,
                        modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                    )

                    // Row 3: Terms of Service Link (https://doceflow.site.je/terms/)
                    DocFlowListRow(
                        title = "Terms of Service",
                        subtitle = OnDeviceAiAndBackupEngine.TERMS_OF_SERVICE_URL,
                        palette = palette,
                        onClick = {
                            openExternalUrl(
                                url = OnDeviceAiAndBackupEngine.TERMS_OF_SERVICE_URL,
                                onFallback = { showTermsOfflineFallbackSheet = true }
                            )
                        },
                        modifier = Modifier.testTag("settings_row_terms_of_service"),
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Gavel,
                                circleColor = DocFlowTokens.IconCircleTeal
                            )
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = "Open Terms of Service",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }
    }

    // =========================================================================
    // SHEET: CATEGORY LARGEST FILES DRILL-DOWN WITH DELETE OPTION
    // =========================================================================
    selectedCategoryForDrilldown?.let { slice ->
        DocFlowBottomSheet(
            onDismissRequest = { selectedCategoryForDrilldown = null },
            palette = palette
        ) {
            Text(
                text = "${slice.label} • On this device",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space1))
            Text(
                text = "${formatStorageBytes(slice.bytes)} across ${slice.files.size} file(s) • Largest files first",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            if (slice.files.isEmpty()) {
                DocFlowEmptyState(
                    icon = Icons.Filled.Storage,
                    title = "No files in ${slice.label}",
                    subtitle = "Files stored on this device in this category will appear here.",
                    palette = palette
                )
            } else {
                slice.files.take(8).forEach { doc ->
                    DocFlowListRow(
                        title = doc.title,
                        subtitle = "${doc.fileType.uppercase()} • ${formatStorageBytes(doc.fileSize)} • Stored on this device",
                        palette = palette,
                        onClick = {},
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Description,
                                circleColor = slice.color
                            )
                        },
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .size(DocFlowTokens.minTouchTarget)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (doc.isDeleted) {
                                            viewModel.permanentlyDeleteDocument(doc.id)
                                        } else {
                                            viewModel.softDeleteDocument(doc.id)
                                        }
                                        toastMessage = "Deleted '${doc.title}'"
                                        selectedCategoryForDrilldown = null
                                    }
                                    .testTag("settings_delete_cat_file_${doc.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete ${doc.title}",
                                    tint = DocFlowTokens.IconCircleRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    )
                    DocFlowDivider(palette = palette)
                }
            }
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
            DocFlowSecondaryButton(
                text = "Close",
                onClick = { selectedCategoryForDrilldown = null },
                palette = palette,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // =========================================================================
    // SHEET 1: THEME SELECTOR (Light / Dark / System)
    // =========================================================================
    if (showThemeSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            palette = palette
        ) {
            Text(
                text = "Choose Theme",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space1))
            Text(
                text = "Select Light Mode (#F0F1F3 flat canvas), Dark Mode (#0F1113), or follow your device setting.",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            val options = listOf(
                Triple(ThemeMode.LIGHT, "Light Mode", "Flat #F0F1F3 light gray background with #FFFFFF cards"),
                Triple(ThemeMode.DARK, "Dark Mode", "Flat #0F1113 dark background with #1B1D20 surfaces"),
                Triple(ThemeMode.SYSTEM, "System Default", "Automatically match Android system appearance")
            )
            options.forEach { (mode, label, desc) ->
                DocFlowListRow(
                    title = label,
                    subtitle = desc,
                    palette = palette,
                    onClick = {
                        viewModel.setThemeMode(mode)
                        showThemeSheet = false
                        toastMessage = "Theme set to $label"
                    },
                    modifier = Modifier.testTag("theme_sheet_option_${mode.name.lowercase()}"),
                    leading = {
                        DocFlowIconCircle(
                            icon = if (mode == ThemeMode.DARK) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            circleColor = if (themeMode == mode) DocFlowTokens.AccentBlue else DocFlowTokens.IconCircleGray
                        )
                    },
                    trailing = {
                        if (themeMode == mode) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = DocFlowTokens.AccentBlue
                            )
                        }
                    }
                )
            }
        }
    }

    // =========================================================================
    // SHEET 2: LANGUAGE & OFFLINE PACK MANAGER
    // =========================================================================
    if (showLanguageSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            palette = palette
        ) {
            Text(
                text = "Language & Offline Packs",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space1))
            Text(
                text = "Select your primary OCR & translation language. Downloaded packs are stored on this device.",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            TranslationLanguage.entries.forEach { lang ->
                val isInstalled = downloadedLangs.contains(lang.code)
                val isPrimary = primaryLanguage == lang
                DocFlowListRow(
                    title = "${lang.displayName} (${lang.nativeName})",
                    subtitle = if (isInstalled) {
                        "Installed on this device • ${lang.packSizeMb}"
                    } else {
                        "Offline pack • ${lang.packSizeMb} (Tap to download & activate)"
                    },
                    palette = palette,
                    onClick = {
                        OnDeviceAiAndBackupEngine.setPrimaryLanguage(context, lang)
                        primaryLanguage = lang
                        downloadedLangs = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                        toastMessage = "Primary language set to ${lang.displayName}"
                    },
                    modifier = Modifier.testTag("lang_option_${lang.code}"),
                    leading = {
                        DocFlowIconCircle(
                            icon = if (isInstalled) Icons.Filled.Language else Icons.Filled.Download,
                            circleColor = if (isPrimary) DocFlowTokens.AccentBlue else DocFlowTokens.IconCircleTeal
                        )
                    },
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                        ) {
                            if (isInstalled && lang.code != "en") {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            OnDeviceAiAndBackupEngine.removeLanguagePack(context, lang.code)
                                            downloadedLangs = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                                            toastMessage = "Removed ${lang.displayName} offline pack"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove pack",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (isPrimary) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Active language",
                                    tint = DocFlowTokens.AccentBlue
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    // =========================================================================
    // DIALOG 3: CLEAR CACHE CONFIRMATION
    // =========================================================================
    if (showClearCacheConfirm) {
        DocFlowDialog(
            title = "Clear local cache?",
            message = "This frees ${formatStorageBytes(cacheBytes)} of rendered page thumbnails and temporary files. Your documents, folders, and Private Safe files stored on this device will not be touched.",
            confirmText = "Clear Cache",
            dismissText = "Cancel",
            palette = palette,
            onConfirm = {
                val freed = OnDeviceAiAndBackupEngine.clearCacheAndTempFiles(context)
                viewModel.clearThumbnailAndTempCache(context)
                cacheBytes = OnDeviceAiAndBackupEngine.calculateCacheAndTempBytes(context)
                showClearCacheConfirm = false
                toastMessage = "Cleared ${formatStorageBytes(freed)} of local cache"
            },
            onDismiss = { showClearCacheConfirm = false }
        )
    }

    // =========================================================================
    // SHEET 4: PIN SETTINGS (Set / Change / Reset Private Safe PIN)
    // =========================================================================
    if (showPinDialog) {
        DocFlowBottomSheet(
            onDismissRequest = { showPinDialog = false },
            palette = palette
        ) {
            Text(
                text = when (pinStep) {
                    "ENTER_CURRENT" -> "Verify Current PIN"
                    "SET_NEW" -> "Set New Private Safe PIN"
                    else -> "Confirm New PIN"
                },
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space1))
            Text(
                text = "Used for AES-256-GCM PBKDF2 key derivation in Private Safe (Stored on this device).",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            val activeValue = when (pinStep) {
                "ENTER_CURRENT" -> currentPinInput
                "SET_NEW" -> newPinInput
                else -> confirmPinInput
            }
            OutlinedTextField(
                value = activeValue,
                onValueChange = { value ->
                    val digitsOnly = value.filter { it.isDigit() }.take(8)
                    pinErrorText = null
                    when (pinStep) {
                        "ENTER_CURRENT" -> currentPinInput = digitsOnly
                        "SET_NEW" -> newPinInput = digitsOnly
                        else -> confirmPinInput = digitsOnly
                    }
                },
                label = {
                    Text(
                        when (pinStep) {
                            "ENTER_CURRENT" -> "Current PIN"
                            "SET_NEW" -> "New 4–8 digit PIN"
                            else -> "Re-enter new PIN"
                        }
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_pin_input")
            )

            if (pinErrorText != null) {
                Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                Text(
                    text = pinErrorText!!,
                    color = DocFlowTokens.IconCircleRed,
                    style = DocFlowTokens.captionStyle
                )
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                if (hasPin && pinStep == "ENTER_CURRENT") {
                    DocFlowSecondaryButton(
                        text = "Reset to Default",
                        onClick = {
                            if (OnDeviceAiAndBackupEngine.verifyPrivateSafePin(context, currentPinInput)) {
                                OnDeviceAiAndBackupEngine.clearPrivateSafePin(context)
                                hasPin = false
                                showPinDialog = false
                                toastMessage = "Private Safe PIN reset to default (1234)"
                            } else {
                                pinErrorText = "Enter your current PIN first to reset"
                            }
                        },
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    DocFlowSecondaryButton(
                        text = "Cancel",
                        onClick = { showPinDialog = false },
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }

                DocFlowPrimaryButton(
                    text = when (pinStep) {
                        "ENTER_CURRENT" -> "Next"
                        "SET_NEW" -> "Continue"
                        else -> "Save PIN"
                    },
                    onClick = {
                        when (pinStep) {
                            "ENTER_CURRENT" -> {
                                if (OnDeviceAiAndBackupEngine.verifyPrivateSafePin(context, currentPinInput)) {
                                    pinStep = "SET_NEW"
                                } else {
                                    pinErrorText = "Incorrect current PIN"
                                }
                            }
                            "SET_NEW" -> {
                                if (newPinInput.length < 4) {
                                    pinErrorText = "PIN must be at least 4 digits"
                                } else {
                                    pinStep = "CONFIRM_NEW"
                                }
                            }
                            else -> {
                                if (confirmPinInput != newPinInput) {
                                    pinErrorText = "PINs do not match"
                                } else {
                                    OnDeviceAiAndBackupEngine.setPrivateSafePin(context, newPinInput)
                                    hasPin = true
                                    showPinDialog = false
                                    toastMessage = "Private Safe PIN updated"
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_pin_confirm_button")
                )
            }
        }
    }

    // =========================================================================
    // SHEET 5: AUTO-LOCK TIMEOUT SELECTOR
    // =========================================================================
    if (showLockTimeoutSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showLockTimeoutSheet = false },
            palette = palette
        ) {
            Text(
                text = "Auto-lock Timeout",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            val timeouts = listOf(
                0 to "Immediately when leaving Private Safe",
                1 to "After 1 minute in background",
                5 to "After 5 minutes in background",
                15 to "After 15 minutes in background"
            )
            timeouts.forEach { (mins, desc) ->
                val isSelected = (mins == 0 && !appLockEnabled) || (mins > 0 && appLockEnabled && lockTimeoutMinutes == mins)
                DocFlowListRow(
                    title = desc,
                    palette = palette,
                    onClick = {
                        if (mins == 0) {
                            appLockEnabled = false
                            OnDeviceAiAndBackupEngine.setAppLockEnabled(context, false)
                        } else {
                            appLockEnabled = true
                            lockTimeoutMinutes = mins
                            OnDeviceAiAndBackupEngine.setAppLockEnabled(context, true)
                            OnDeviceAiAndBackupEngine.setLockTimeoutMinutes(context, mins)
                        }
                        showLockTimeoutSheet = false
                        toastMessage = "Auto-lock updated: $desc"
                    },
                    trailing = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = DocFlowTokens.AccentBlue
                            )
                        }
                    }
                )
            }
        }
    }

    // =========================================================================
    // SHEET 6: PRIVACY POLICY & TERMS OF SERVICE OFFLINE READERS
    // =========================================================================
    if (showPrivacyOfflineFallbackSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showPrivacyOfflineFallbackSheet = false },
            palette = palette
        ) {
            Text(
                text = "Privacy Policy",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space1))
            Text(
                text = OnDeviceAiAndBackupEngine.PRIVACY_POLICY_URL,
                color = DocFlowTokens.AccentBlue,
                style = DocFlowTokens.captionStyle,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
            Text(
                text = "DocFlow processes and stores all documents, scans, OCR text, summaries, translations, and AES-256-GCM Private Safe vaults strictly on your device. No personal files or analytics are uploaded to external servers.",
                style = DocFlowTokens.bodyStyle,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            DocFlowPrimaryButton(
                text = "Close",
                onClick = { showPrivacyOfflineFallbackSheet = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showTermsOfflineFallbackSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showTermsOfflineFallbackSheet = false },
            palette = palette
        ) {
            Text(
                text = "Terms of Service",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space1))
            Text(
                text = OnDeviceAiAndBackupEngine.TERMS_OF_SERVICE_URL,
                color = DocFlowTokens.AccentBlue,
                style = DocFlowTokens.captionStyle,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
            Text(
                text = "By using DocFlow, you retain 100% ownership and custody of your local documents stored on this device. Remember your Private Safe PIN, as AES-256-GCM encryption keys are derived locally on your device.",
                style = DocFlowTokens.bodyStyle,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            DocFlowPrimaryButton(
                text = "Close",
                onClick = { showTermsOfflineFallbackSheet = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showLicensesSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showLicensesSheet = false },
            palette = palette
        ) {
            Text(
                text = "DocFlow v${OnDeviceAiAndBackupEngine.APP_VERSION_NAME} (Offline Architecture)",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            Text(
                text = "• Room SQLite + FTS4 Full-Text Index (Stored on this device)\n• Android PdfRenderer & PdfDocument Engine\n• Google ML Kit On-Device Text Recognition (English + Devanagari Hindi)\n• Hardware DSP NoiseSuppressor + Offline SpeechRecognizer\n• WebCrypto / javax.crypto AES-256-GCM + PBKDF2-HMAC-SHA256",
                style = DocFlowTokens.bodyStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            DocFlowPrimaryButton(
                text = "Done",
                onClick = { showLicensesSheet = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThemeQuickChip(
    label: String,
    selected: Boolean,
    palette: DocFlowPalette,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(DocFlowTokens.pillButtonShape)
            .background(if (selected) DocFlowTokens.AccentBlue else palette.background)
            .border(
                width = 1.dp,
                color = if (selected) DocFlowTokens.AccentBlue else palette.divider,
                shape = DocFlowTokens.pillButtonShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format("%.2f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
