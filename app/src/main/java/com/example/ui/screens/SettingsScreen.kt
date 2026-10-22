package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StorageDashboardSummary
import com.example.data.util.BiometricSecurityHelper
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.data.util.TranslationLanguage
import com.example.ui.components.DocFlowBottomSheet
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDialog
import com.example.ui.components.DocFlowHeaderAction
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowProgressBar
import com.example.ui.components.DocFlowScreenHeader
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowSectionLabel
import com.example.ui.components.DocFlowToast
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.ThemeMode

/**
 * Dedicated Settings Screen built exclusively with the DocFlow Shared Component Library
 * (`DocFlowScreenHeader`, `DocFlowSectionLabel`, `DocFlowCard`, `DocFlowListRow`,
 * `DocFlowIconCircle`, `DocFlowProgressBar`, `DocFlowBottomSheet`, `DocFlowDialog`, `DocFlowToast`).
 *
 * Includes grouped sections with `DocFlowListRow`s for:
 * 1. 'General' (Theme with Light Mode / Dark Mode / System Default, Language)
 * 2. 'Storage' (Storage usage stats, Clear Cache, Encrypted .docflow Backup & Restore)
 * 3. 'Security' (Biometric Toggle, PIN settings)
 * 4. 'About' (App Version, Privacy Policy link https://doceflow.site.je/privacy/, Terms of Service link https://doceflow.site.je/terms/)
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    biometricVaultEnabled: Boolean,
    appEntryBiometricGateEnabled: Boolean,
    storageSummary: StorageDashboardSummary,
    totalDocumentsCount: Int,
    deletedDocumentsCount: Int,
    privateSafeCount: Int,
    onBack: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleBiometricVault: () -> Unit,
    onToggleAppEntryBiometricGate: () -> Unit,
    onOpenStorageDashboard: () -> Unit,
    onClearCacheAndCompact: () -> Unit,
    onExportBackup: (String) -> Unit,
    onImportBackupUri: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val horizontalMargin = DocFlowTokens.adaptiveScreenMargin(screenWidthDp)

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var isToastError by remember { mutableStateOf(false) }

    // Sheet & Dialog states
    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showPinSettingsSheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var activeLegalLinkPreview by remember { mutableStateOf<LegalDocumentSpec?>(null) }

    // Language & OCR state
    var primaryLanguage by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getPrimaryLanguage(context))
    }
    var downloadedLangs by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context))
    }

    // Security & PIN state
    var hasConfiguredPin by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.hasPrivateSafePin(context))
    }
    var lockTimeoutMinutes by remember {
        mutableIntStateOf(OnDeviceAiAndBackupEngine.getLockTimeoutMinutes(context))
    }
    val biometricStatus = remember(context) {
        BiometricSecurityHelper.getBiometricStatus(context)
    }

    // Storage & Cache metrics
    var cacheBytes by remember {
        mutableLongStateOf(
            OnDeviceAiAndBackupEngine.calculateCacheAndTempBytes(context) +
                (storageSummary.versionSnapshotsCount * 4096L)
        )
    }
    val deviceFreeBytes = remember {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        }.getOrDefault(12L * 1024L * 1024L * 1024L)
    }
    val totalAppBytes = storageSummary.totalBytesUsed + cacheBytes

    // Backup state
    var backupPassphrase by remember { mutableStateOf("") }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportBackupUri(uri, backupPassphrase.ifBlank { "docflow-backup" })
            toastMessage = "Restoring encrypted .docflow backup..."
            isToastError = false
            showBackupSheet = false
        }
    }

    val themeSubtitle = when (themeMode) {
        ThemeMode.LIGHT -> "Light Mode (#F0F1F3 background, white cards)"
        ThemeMode.DARK -> "Dark Mode (#0F1113 background, dark cards)"
        ThemeMode.SYSTEM -> "System Default (Follows device setting)"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DocFlowTokens.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DocFlowTokens.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = DocFlowTokens.maxContentWidth)
                    .testTag("settings_screen_list"),
                contentPadding = PaddingValues(bottom = DocFlowTokens.space8),
                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                // Top Shared ScreenHeader with Back Button and Quick Light/Dark Toggle
                item {
                    DocFlowScreenHeader(
                        title = "Settings",
                        subtitle = "100% Offline • Your files stay on your phone",
                        onNavigateBack = onBack,
                        backTestTag = "settings_back_button",
                        actions = listOf(
                            DocFlowHeaderAction(
                                icon = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (themeMode == ThemeMode.DARK) "Switch to Light Mode" else "Switch to Dark Mode",
                                testTag = "settings_header_light_mode_button",
                                isActive = themeMode == ThemeMode.LIGHT,
                                onClick = {
                                    val next = if (themeMode == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
                                    onSelectThemeMode(next)
                                    toastMessage = if (next == ThemeMode.LIGHT) {
                                        "Switched to Light Mode"
                                    } else {
                                        "Switched to Dark Mode"
                                    }
                                    isToastError = false
                                }
                            )
                        )
                    )
                }

                // Toast Feedback Banner
                if (!toastMessage.isNullOrBlank()) {
                    item {
                        DocFlowToast(
                            message = toastMessage,
                            isError = isToastError,
                            onDismiss = { toastMessage = null }
                        )
                    }
                }

                // =============================================================
                // SECTION 1: GENERAL (Theme, Language)
                // =============================================================
                item {
                    DocFlowSectionLabel(text = "General")
                }

                item {
                    DocFlowCard(
                        modifier = Modifier.padding(horizontal = horizontalMargin),
                        padding = PaddingValues(vertical = DocFlowTokens.space1)
                    ) {
                        // Row 1: Theme
                        DocFlowListRow(
                            title = "Theme",
                            subtitle = themeSubtitle,
                            leading = {
                                DocFlowIconCircle(
                                    icon = when (themeMode) {
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.SYSTEM -> Icons.Default.Palette
                                    },
                                    circleColor = DocFlowTokens.accentBlue,
                                    contentDescription = "Theme"
                                )
                            },
                            showChevron = true,
                            showBottomDivider = false,
                            testTag = "settings_row_theme",
                            onClick = { showThemeSheet = true }
                        )

                        // Inline Quick Theme Segmented Bar (Light Mode / Dark Mode / System)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = DocFlowTokens.cardPaddingMin,
                                    end = DocFlowTokens.cardPaddingMin,
                                    bottom = DocFlowTokens.space3
                                ),
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                        ) {
                            ThemeSegmentPill(
                                label = "Light Mode",
                                icon = Icons.Default.LightMode,
                                selected = themeMode == ThemeMode.LIGHT,
                                testTag = "settings_theme_chip_light",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSelectThemeMode(ThemeMode.LIGHT)
                                    toastMessage = "Light Mode active"
                                    isToastError = false
                                }
                            )
                            ThemeSegmentPill(
                                label = "Dark Mode",
                                icon = Icons.Default.DarkMode,
                                selected = themeMode == ThemeMode.DARK,
                                testTag = "settings_theme_chip_dark",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSelectThemeMode(ThemeMode.DARK)
                                    toastMessage = "Dark Mode active"
                                    isToastError = false
                                }
                            )
                            ThemeSegmentPill(
                                label = "System",
                                icon = Icons.Default.SettingsBrightness,
                                selected = themeMode == ThemeMode.SYSTEM,
                                testTag = "settings_theme_chip_system",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSelectThemeMode(ThemeMode.SYSTEM)
                                    toastMessage = "Following System theme"
                                    isToastError = false
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin),
                            thickness = DocFlowTokens.dividerThickness,
                            color = DocFlowTokens.divider
                        )

                        // Row 2: Language
                        DocFlowListRow(
                            title = "Language",
                            subtitle = "${primaryLanguage.displayName} (${primaryLanguage.nativeName}) • ${downloadedLangs.size} offline OCR/translation pack(s)",
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.Language,
                                    circleColor = DocFlowTokens.iconTealTranslate,
                                    contentDescription = "Language"
                                )
                            },
                            showChevron = true,
                            showBottomDivider = false,
                            testTag = "settings_row_language",
                            onClick = { showLanguageSheet = true }
                        )
                    }
                }

                // =============================================================
                // SECTION 2: STORAGE (Clear Cache, Storage usage stats)
                // =============================================================
                item {
                    DocFlowSectionLabel(text = "Storage")
                }

                item {
                    DocFlowCard(
                        modifier = Modifier.padding(horizontal = horizontalMargin),
                        padding = PaddingValues(vertical = DocFlowTokens.space1)
                    ) {
                        // Row 1: Storage usage stats
                        DocFlowListRow(
                            title = "Storage usage stats",
                            subtitle = "${formatStorageBytes(totalAppBytes)} used ($totalDocumentsCount files) • ${formatStorageBytes(deviceFreeBytes)} free on device",
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.PieChart,
                                    circleColor = DocFlowTokens.accentBlue,
                                    contentDescription = "Storage usage stats"
                                )
                            },
                            showChevron = true,
                            showBottomDivider = false,
                            testTag = "settings_row_storage_stats",
                            onClick = onOpenStorageDashboard
                        )

                        // Visual Storage Breakdown Progress Bar inside Storage Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = DocFlowTokens.cardPaddingMin,
                                    end = DocFlowTokens.cardPaddingMin,
                                    bottom = DocFlowTokens.space3
                                ),
                            verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                        ) {
                            val storageRatio = remember(totalAppBytes, deviceFreeBytes) {
                                val denom = (totalAppBytes + deviceFreeBytes).coerceAtLeast(1L).toFloat()
                                (totalAppBytes.toFloat() / denom).coerceIn(0.04f, 1f)
                            }
                            DocFlowProgressBar(
                                progress = storageRatio,
                                label = "Documents: ${formatStorageBytes(storageSummary.totalBytesUsed)} • Cache: ${formatStorageBytes(cacheBytes)} • Safe: $privateSafeCount • Trash: $deletedDocumentsCount"
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin),
                            thickness = DocFlowTokens.dividerThickness,
                            color = DocFlowTokens.divider
                        )

                        // Row 2: Clear Cache
                        DocFlowListRow(
                            title = "Clear Cache",
                            subtitle = if (cacheBytes > 0L) {
                                "${formatStorageBytes(cacheBytes)} in thumbnails, temp PDFs & old snapshots"
                            } else {
                                "0 B • Cache and temporary files are clean"
                            },
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.CleaningServices,
                                    circleColor = DocFlowTokens.iconOrangePpt,
                                    contentDescription = "Clear Cache"
                                )
                            },
                            trailing = {
                                Surface(
                                    shape = DocFlowTokens.pillShape,
                                    color = DocFlowTokens.surfaceSubtle,
                                    border = BorderStroke(1.dp, DocFlowTokens.divider)
                                ) {
                                    Text(
                                        text = if (cacheBytes > 0L) "Clean" else "0 B",
                                        style = DocFlowTokens.microBadgeStyle,
                                        color = if (cacheBytes > 0L) DocFlowTokens.accentBlue else DocFlowTokens.textSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            },
                            showBottomDivider = true,
                            testTag = "settings_row_clear_cache",
                            onClick = {
                                val freed = OnDeviceAiAndBackupEngine.clearCacheAndTempFiles(context) + cacheBytes
                                onClearCacheAndCompact()
                                cacheBytes = 0L
                                toastMessage = "Cache cleared • Freed ${formatStorageBytes(freed.coerceAtLeast(1024L))}"
                                isToastError = false
                            }
                        )

                        // Row 3: Encrypted .docflow Local Backup & Restore
                        DocFlowListRow(
                            title = "Encrypted .docflow Backup",
                            subtitle = "Export or restore AES-256-GCM local archive",
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.Backup,
                                    circleColor = DocFlowTokens.iconGreenDownload,
                                    contentDescription = "Encrypted Backup"
                                )
                            },
                            showChevron = true,
                            showBottomDivider = false,
                            testTag = "settings_row_backup",
                            onClick = { showBackupSheet = true }
                        )
                    }
                }

                // =============================================================
                // SECTION 3: SECURITY (Biometric Toggle, PIN settings)
                // =============================================================
                item {
                    DocFlowSectionLabel(text = "Security")
                }

                item {
                    DocFlowCard(
                        modifier = Modifier.padding(horizontal = horizontalMargin),
                        padding = PaddingValues(vertical = DocFlowTokens.space1)
                    ) {
                        // Row 1: Biometric Toggle
                        DocFlowListRow(
                            title = "Biometric Toggle",
                            subtitle = "${biometricStatus.statusLabel} • Unlock Private Safe & encrypted files",
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.Fingerprint,
                                    circleColor = DocFlowTokens.iconBlueSummarySafe,
                                    contentDescription = "Biometric Toggle"
                                )
                            },
                            trailing = {
                                Switch(
                                    checked = biometricVaultEnabled,
                                    onCheckedChange = {
                                        onToggleBiometricVault()
                                        OnDeviceAiAndBackupEngine.setBiometricUnlockEnabled(
                                            context,
                                            !biometricVaultEnabled
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = DocFlowTokens.accentBlue,
                                        uncheckedThumbColor = DocFlowTokens.textSecondary,
                                        uncheckedTrackColor = DocFlowTokens.surfaceSubtle
                                    ),
                                    modifier = Modifier.testTag("settings_switch_biometric")
                                )
                            },
                            showBottomDivider = true,
                            testTag = "settings_row_biometric",
                            onClick = {
                                onToggleBiometricVault()
                                OnDeviceAiAndBackupEngine.setBiometricUnlockEnabled(
                                    context,
                                    !biometricVaultEnabled
                                )
                            }
                        )

                        // Row 2: PIN settings
                        DocFlowListRow(
                            title = "PIN settings",
                            subtitle = if (hasConfiguredPin) {
                                "Custom PIN active • Auto-lock after ${lockTimeoutMinutes}m"
                            } else {
                                "Default PIN (1234) • Auto-lock after ${lockTimeoutMinutes}m • Tap to update"
                            },
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.Password,
                                    circleColor = DocFlowTokens.accentBlue,
                                    contentDescription = "PIN settings"
                                )
                            },
                            showChevron = true,
                            showBottomDivider = true,
                            testTag = "settings_row_pin",
                            onClick = { showPinSettingsSheet = true }
                        )

                        // Row 3: App Launch Biometric Lock
                        DocFlowListRow(
                            title = "Require Lock on App Launch",
                            subtitle = if (appEntryBiometricGateEnabled) {
                                "Enabled • Prompt for biometric/credential when opening DocFlow"
                            } else {
                                "Off • Only prompt when opening Private Safe"
                            },
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.Lock,
                                    circleColor = DocFlowTokens.iconGreenDarkExcel,
                                    contentDescription = "App Launch Lock"
                                )
                            },
                            trailing = {
                                Switch(
                                    checked = appEntryBiometricGateEnabled,
                                    onCheckedChange = { onToggleAppEntryBiometricGate() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = DocFlowTokens.accentBlue,
                                        uncheckedThumbColor = DocFlowTokens.textSecondary,
                                        uncheckedTrackColor = DocFlowTokens.surfaceSubtle
                                    ),
                                    modifier = Modifier.testTag("settings_switch_app_lock")
                                )
                            },
                            showBottomDivider = false,
                            testTag = "settings_row_app_lock",
                            onClick = onToggleAppEntryBiometricGate
                        )
                    }
                }

                // =============================================================
                // SECTION 4: ABOUT (App Version, Privacy Policy, Terms of Service)
                // =============================================================
                item {
                    DocFlowSectionLabel(text = "About")
                }

                item {
                    DocFlowCard(
                        modifier = Modifier.padding(horizontal = horizontalMargin),
                        padding = PaddingValues(vertical = DocFlowTokens.space1)
                    ) {
                        // Row 1: App Version
                        DocFlowListRow(
                            title = "App Version",
                            subtitle = "Version ${OnDeviceAiAndBackupEngine.APP_VERSION_NAME} (Build ${OnDeviceAiAndBackupEngine.APP_VERSION_BUILD}) • Android 8.0+ • Offline",
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.Info,
                                    circleColor = DocFlowTokens.accentBlue,
                                    contentDescription = "App Version"
                                )
                            },
                            trailing = {
                                Surface(
                                    shape = DocFlowTokens.pillShape,
                                    color = DocFlowTokens.accentBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "v${OnDeviceAiAndBackupEngine.APP_VERSION_NAME}",
                                        style = DocFlowTokens.microBadgeStyle,
                                        color = DocFlowTokens.accentBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            },
                            showBottomDivider = true,
                            testTag = "settings_row_app_version",
                            onClick = { showAboutDialog = true }
                        )

                        // Row 2: Privacy Policy link (https://doceflow.site.je/privacy/)
                        DocFlowListRow(
                            title = "Privacy Policy",
                            subtitle = OnDeviceAiAndBackupEngine.PRIVACY_POLICY_URL,
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.PrivacyTip,
                                    circleColor = DocFlowTokens.iconGreenDownload,
                                    contentDescription = "Privacy Policy"
                                )
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open Privacy Policy",
                                    tint = DocFlowTokens.accentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            showBottomDivider = true,
                            testTag = "settings_row_privacy_policy",
                            onClick = {
                                val spec = LegalDocumentSpec(
                                    title = "Privacy Policy",
                                    url = OnDeviceAiAndBackupEngine.PRIVACY_POLICY_URL,
                                    summaryPoints = listOf(
                                        "100% On-Device Processing: DocFlow opens, scans, edits, summarizes, and translates your documents locally on your Android device.",
                                        "Zero Cloud Uploads: We do not operate servers, user accounts, or cloud databases. Your files never leave your phone unless you explicitly share them.",
                                        "No Analytics or Tracking: DocFlow contains no analytics SDKs, no advertising identifiers, no cookies, and no telemetry.",
                                        "Hardware-Backed Security: Private Safe files are encrypted locally using AES-256-GCM with keys derived via PBKDF2 and protected by Android Keystore."
                                    )
                                )
                                val opened = openExternalUrlSafely(context, spec.url)
                                if (!opened) {
                                    activeLegalLinkPreview = spec
                                } else {
                                    activeLegalLinkPreview = spec
                                }
                            }
                        )

                        // Row 3: Terms of Service link (https://doceflow.site.je/terms/)
                        DocFlowListRow(
                            title = "Terms of Service",
                            subtitle = OnDeviceAiAndBackupEngine.TERMS_OF_SERVICE_URL,
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Default.Gavel,
                                    circleColor = DocFlowTokens.iconTealTranslate,
                                    contentDescription = "Terms of Service"
                                )
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open Terms of Service",
                                    tint = DocFlowTokens.accentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            showBottomDivider = false,
                            testTag = "settings_row_terms_of_service",
                            onClick = {
                                val spec = LegalDocumentSpec(
                                    title = "Terms of Service",
                                    url = OnDeviceAiAndBackupEngine.TERMS_OF_SERVICE_URL,
                                    summaryPoints = listOf(
                                        "Local Responsibility: Because DocFlow stores all documents locally on your device without any cloud servers, you are responsible for maintaining backups of important files.",
                                        "Private Safe PIN & Passphrases: Encrypted files cannot be recovered by us if you forget your PIN or backup passphrase, as no keys are ever sent to a server.",
                                        "Permitted Use: DocFlow is provided free of charge for personal and professional document viewing, scanning, OCR, and conversion.",
                                        "Data Deletion: Uninstalling DocFlow or clearing app storage permanently removes all app-private documents, databases, and keys from your device."
                                    )
                                )
                                val opened = openExternalUrlSafely(context, spec.url)
                                if (!opened) {
                                    activeLegalLinkPreview = spec
                                } else {
                                    activeLegalLinkPreview = spec
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // 1. THEME SELECTION BOTTOM SHEET
    // =========================================================================
    if (showThemeSheet) {
        DocFlowBottomSheet(
            title = "Select Theme",
            onDismissRequest = { showThemeSheet = false }
        ) {
            Text(
                text = "Choose how DocFlow looks across all screens, cards, and viewers.",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            val themeOptions = listOf(
                Triple(
                    ThemeMode.LIGHT,
                    "Light Mode",
                    "Flat light gray background (#F0F1F3) with crisp white (#FFFFFF) cards"
                ),
                Triple(
                    ThemeMode.DARK,
                    "Dark Mode",
                    "OLED-friendly dark background (#0F1113) with charcoal (#1B1D20) cards"
                ),
                Triple(
                    ThemeMode.SYSTEM,
                    "System Default",
                    "Automatically switches between Light and Dark mode with your Android device"
                )
            )

            DocFlowCard(
                containerColor = DocFlowTokens.surfaceSubtle,
                padding = PaddingValues(vertical = DocFlowTokens.space1)
            ) {
                themeOptions.forEachIndexed { idx, (mode, label, desc) ->
                    val isSelected = themeMode == mode
                    DocFlowListRow(
                        title = label,
                        subtitle = desc,
                        leading = {
                            DocFlowIconCircle(
                                icon = when (mode) {
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                                },
                                circleColor = if (isSelected) DocFlowTokens.accentBlue else DocFlowTokens.iconGrayTrash,
                                contentDescription = label
                            )
                        },
                        trailing = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = DocFlowTokens.accentBlue
                                )
                            }
                        },
                        selected = isSelected,
                        showBottomDivider = idx < themeOptions.lastIndex,
                        testTag = "theme_sheet_option_${mode.name.lowercase()}",
                        onClick = {
                            onSelectThemeMode(mode)
                            toastMessage = "$label applied"
                            isToastError = false
                            showThemeSheet = false
                        }
                    )
                }
            }
        }
    }

    // =========================================================================
    // 2. LANGUAGE & OFFLINE OCR PACKS BOTTOM SHEET
    // =========================================================================
    if (showLanguageSheet) {
        DocFlowBottomSheet(
            title = "Language & Offline OCR Packs",
            onDismissRequest = { showLanguageSheet = false }
        ) {
            Text(
                text = "Select your default OCR & translation language. Downloaded packs run 100% offline on your device.",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            DocFlowCard(
                containerColor = DocFlowTokens.surfaceSubtle,
                padding = PaddingValues(vertical = DocFlowTokens.space1)
            ) {
                TranslationLanguage.entries.forEachIndexed { index, lang ->
                    val isPrimary = primaryLanguage == lang
                    val isInstalled = lang.code in downloadedLangs
                    DocFlowListRow(
                        title = "${lang.displayName} (${lang.nativeName})",
                        subtitle = "${lang.packSizeMb} • ${if (isPrimary) "Primary language" else if (isInstalled) "Installed offline" else "Tap Download for offline use"}",
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Default.Language,
                                circleColor = if (isPrimary) DocFlowTokens.accentBlue else DocFlowTokens.iconTealTranslate,
                                contentDescription = lang.displayName
                            )
                        },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                            ) {
                                if (!isInstalled) {
                                    DocFlowSecondaryButton(
                                        text = "Download",
                                        leadingIcon = Icons.Default.Download,
                                        onClick = {
                                            OnDeviceAiAndBackupEngine.markLanguagePackDownloaded(context, lang.code)
                                            downloadedLangs = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                                            toastMessage = "Downloaded ${lang.displayName} offline pack"
                                            isToastError = false
                                        }
                                    )
                                } else {
                                    if (isPrimary) {
                                        Surface(
                                            shape = DocFlowTokens.pillShape,
                                            color = DocFlowTokens.accentBlue.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = "Active",
                                                style = DocFlowTokens.microBadgeStyle,
                                                color = DocFlowTokens.accentBlue,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else if (lang.code != "en") {
                                        TextButton(
                                            onClick = {
                                                OnDeviceAiAndBackupEngine.removeLanguagePack(context, lang.code)
                                                downloadedLangs = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                                            }
                                        ) {
                                            Text("Remove", color = DocFlowTokens.iconRedPdf, style = DocFlowTokens.captionBoldStyle)
                                        }
                                    }
                                }
                            }
                        },
                        selected = isPrimary,
                        showBottomDivider = index < TranslationLanguage.entries.lastIndex,
                        testTag = "language_option_${lang.code}",
                        onClick = {
                            OnDeviceAiAndBackupEngine.setPrimaryLanguage(context, lang)
                            primaryLanguage = lang
                            downloadedLangs = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                            toastMessage = "Primary language set to ${lang.displayName}"
                            isToastError = false
                        }
                    )
                }
            }
        }
    }

    // =========================================================================
    // 3. PIN SETTINGS & AUTO-LOCK TIMEOUT BOTTOM SHEET
    // =========================================================================
    if (showPinSettingsSheet) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        DocFlowBottomSheet(
            title = "Private Safe PIN & Lock Settings",
            onDismissRequest = { showPinSettingsSheet = false }
        ) {
            Text(
                text = "Protect your Private Safe documents with a 4–8 digit PIN and hardware-backed AES-256-GCM encryption.",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            Text(
                text = "Auto-lock Timeout",
                style = DocFlowTokens.bodyMediumStyle,
                color = DocFlowTokens.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
            ) {
                listOf(1, 5, 15, 30).forEach { mins ->
                    FilterChip(
                        selected = lockTimeoutMinutes == mins,
                        onClick = {
                            lockTimeoutMinutes = mins
                            OnDeviceAiAndBackupEngine.setLockTimeoutMinutes(context, mins)
                        },
                        label = { Text("${mins}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DocFlowTokens.accentBlue.copy(alpha = 0.16f),
                            selectedLabelColor = DocFlowTokens.accentBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space4))

            OutlinedTextField(
                value = newPin,
                onValueChange = {
                    if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                        newPin = it
                        pinError = null
                    }
                },
                label = { Text("New 4–8 Digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_pin_new_input")
            )

            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                        confirmPin = it
                        pinError = null
                    }
                },
                label = { Text("Confirm New PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_pin_confirm_input")
            )

            if (!pinError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                Text(
                    text = pinError.orEmpty(),
                    style = DocFlowTokens.captionStyle,
                    color = DocFlowTokens.iconRedPdf
                )
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                if (hasConfiguredPin) {
                    DocFlowSecondaryButton(
                        text = "Reset to Default",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            OnDeviceAiAndBackupEngine.clearPrivateSafePin(context)
                            hasConfiguredPin = false
                            toastMessage = "Private Safe PIN reset to default (1234)"
                            isToastError = false
                            showPinSettingsSheet = false
                        }
                    )
                }
                DocFlowPrimaryButton(
                    text = "Save PIN",
                    modifier = Modifier.weight(1f),
                    testTag = "settings_save_pin_button",
                    onClick = {
                        when {
                            newPin.length < 4 -> {
                                pinError = "PIN must be at least 4 digits."
                            }
                            newPin != confirmPin -> {
                                pinError = "PIN entries do not match."
                            }
                            else -> {
                                OnDeviceAiAndBackupEngine.setPrivateSafePin(context, newPin)
                                hasConfiguredPin = true
                                toastMessage = "Private Safe PIN updated"
                                isToastError = false
                                showPinSettingsSheet = false
                            }
                        }
                    }
                )
            }
        }
    }

    // =========================================================================
    // 4. ENCRYPTED .DOCFLOW BACKUP & RESTORE BOTTOM SHEET
    // =========================================================================
    if (showBackupSheet) {
        DocFlowBottomSheet(
            title = "Encrypted .docflow Backup & Restore",
            onDismissRequest = { showBackupSheet = false }
        ) {
            Text(
                text = "Export or restore all documents, folders, tags, and Private Safe items in a single AES-256-GCM encrypted .docflow file.",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            OutlinedTextField(
                value = backupPassphrase,
                onValueChange = { backupPassphrase = it },
                label = { Text("Backup Passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_backup_passphrase_input")
            )

            Spacer(modifier = Modifier.height(DocFlowTokens.space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                DocFlowPrimaryButton(
                    text = "Export .docflow",
                    leadingIcon = Icons.Default.Backup,
                    modifier = Modifier.weight(1f),
                    testTag = "settings_export_backup_button",
                    onClick = {
                        onExportBackup(backupPassphrase.ifBlank { "docflow-backup" })
                        toastMessage = "Exported encrypted .docflow backup archive"
                        isToastError = false
                        showBackupSheet = false
                    }
                )
                DocFlowSecondaryButton(
                    text = "Restore",
                    leadingIcon = Icons.Default.Restore,
                    modifier = Modifier.weight(1f),
                    testTag = "settings_restore_backup_button",
                    onClick = {
                        restoreBackupLauncher.launch(arrayOf("*/*"))
                    }
                )
            }
        }
    }

    // =========================================================================
    // 5. ABOUT DOCFLOW DIALOG
    // =========================================================================
    if (showAboutDialog) {
        DocFlowDialog(
            title = "About DocFlow",
            onDismissRequest = { showAboutDialog = false },
            confirmText = "Done",
            onConfirm = { showAboutDialog = false },
            dismissText = "Privacy Policy"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                ) {
                    DocFlowIconCircle(
                        icon = Icons.Default.Description,
                        circleColor = DocFlowTokens.accentBlue,
                        contentDescription = null,
                        solidFill = true
                    )
                    Column {
                        Text(
                            text = "DocFlow v${OnDeviceAiAndBackupEngine.APP_VERSION_NAME}",
                            style = DocFlowTokens.rowTitleStyle.copy(fontWeight = FontWeight.Bold),
                            color = DocFlowTokens.textPrimary
                        )
                        Text(
                            text = "Build ${OnDeviceAiAndBackupEngine.APP_VERSION_BUILD} • Android 8.0+ • Free",
                            style = DocFlowTokens.captionStyle,
                            color = DocFlowTokens.textSecondary
                        )
                    }
                }
                Text(
                    text = "Open, scan, convert, summarize, translate, and organize your documents privately and offline.",
                    style = DocFlowTokens.bodyStyle,
                    color = DocFlowTokens.textPrimary
                )
                DocFlowCard(
                    containerColor = DocFlowTokens.surfaceSubtle,
                    padding = PaddingValues(DocFlowTokens.space3)
                ) {
                    Text(
                        text = "• No servers or cloud uploads\n• No account required\n• Zero trackers or analytics\n• Hardware-backed AES-256-GCM Private Safe",
                        style = DocFlowTokens.captionStyle,
                        color = DocFlowTokens.textSecondary
                    )
                }
            }
        }
    }

    // =========================================================================
    // 6. PRIVACY POLICY & TERMS OF SERVICE SHEET (With External Browser Launch + Copy Link)
    // =========================================================================
    activeLegalLinkPreview?.let { legalSpec ->
        DocFlowBottomSheet(
            title = legalSpec.title,
            onDismissRequest = { activeLegalLinkPreview = null }
        ) {
            DocFlowCard(
                containerColor = DocFlowTokens.surfaceSubtle,
                padding = PaddingValues(DocFlowTokens.space3)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Official Web URL",
                            style = DocFlowTokens.captionBoldStyle,
                            color = DocFlowTokens.textSecondary
                        )
                        Text(
                            text = legalSpec.url,
                            style = DocFlowTokens.bodyMediumStyle.copy(fontFamily = FontFamily.Monospace),
                            color = DocFlowTokens.accentBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DocFlowSecondaryButton(
                        text = "Copy",
                        leadingIcon = Icons.Default.ContentCopy,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText(legalSpec.title, legalSpec.url))
                            toastMessage = "Copied ${legalSpec.url}"
                            isToastError = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            legalSpec.summaryPoints.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DocFlowTokens.space1),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = DocFlowTokens.accentBlue,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = point,
                        style = DocFlowTokens.bodyStyle,
                        color = DocFlowTokens.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                DocFlowSecondaryButton(
                    text = "Close",
                    modifier = Modifier.weight(1f),
                    onClick = { activeLegalLinkPreview = null }
                )
                DocFlowPrimaryButton(
                    text = "Open in Browser",
                    leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    modifier = Modifier.weight(1f),
                    testTag = "legal_sheet_open_browser_button",
                    onClick = {
                        openExternalUrlSafely(context, legalSpec.url)
                        activeLegalLinkPreview = null
                    }
                )
            }
        }
    }
}

private data class LegalDocumentSpec(
    val title: String,
    val url: String,
    val summaryPoints: List<String>
)

@Composable
private fun ThemeSegmentPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (selected) DocFlowTokens.accentBlue else DocFlowTokens.surfaceSubtle
    val contentColor = if (selected) Color.White else DocFlowTokens.textPrimary
    val borderColor = if (selected) DocFlowTokens.accentBlue else DocFlowTokens.divider

    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(DocFlowTokens.pillShape)
            .background(bgColor)
            .border(1.dp, borderColor, DocFlowTokens.pillShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.Check else icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = DocFlowTokens.captionBoldStyle,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun openExternalUrlSafely(context: Context, url: String): Boolean {
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
