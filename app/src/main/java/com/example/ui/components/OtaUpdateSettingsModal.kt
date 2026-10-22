package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.OtaUpdateRecordEntity
import com.example.data.util.OtaInstallPhase
import com.example.data.util.OtaReleaseChannel
import com.example.data.util.OtaReleaseManifest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OtaUpdateAndSettingsModal(
    currentVersionName: String,
    currentVersionCode: Int,
    selectedChannel: OtaReleaseChannel,
    installPhase: OtaInstallPhase,
    downloadProgress: Float,
    availableManifest: OtaReleaseManifest,
    autoOtaEnabled: Boolean,
    otaHistory: List<OtaUpdateRecordEntity>,
    biometricVaultEnabled: Boolean,
    appEntryBiometricGateEnabled: Boolean,
    isOfflineMode: Boolean,
    userEmail: String,
    onSelectChannel: (OtaReleaseChannel) -> Unit,
    onCheckForUpdates: () -> Unit,
    onInstallOtaUpdate: () -> Unit,
    onToggleAutoOta: () -> Unit,
    onToggleBiometricVault: () -> Unit,
    onToggleAppEntryBiometricGate: () -> Unit,
    onToggleOfflineMode: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val cyanAccent = Color(0xFF00F4FE)
    val blueAccent = Color(0xFF3E90FF)
    val emeraldAccent = Color(0xFF34D399)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10131F),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE1E1F3),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cyanAccent.copy(alpha = 0.16f))
                        .border(1.dp, cyanAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "OTA App Updates",
                        tint = cyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "OTA App Updates & Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Installed: v$currentVersionName (Build #$currentVersionCode) • $userEmail",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyanAccent
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Release Channel Selector
                Text(
                    text = "OTA Release Channel:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFC0C6D6)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OtaReleaseChannel.entries.forEach { channel ->
                        val isSelected = selectedChannel == channel
                        val chColor = Color(channel.badgeColorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectChannel(channel) },
                            label = {
                                Text(
                                    text = channel.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("ota_channel_${channel.name.lowercase()}")
                        )
                    }
                }

                // 2. Active OTA Release Manifest Card
                CrystalGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ota_manifest_card"),
                    cornerRadius = 18.dp,
                    tintColor = cyanAccent
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = cyanAccent.copy(alpha = 0.16f),
                                shape = CircleShape,
                                modifier = Modifier.border(1.dp, cyanAccent.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = cyanAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = installPhase.statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cyanAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${availableManifest.patchSizeBytes / 1024} KB Delta",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC0C6D6)
                            )
                        }

                        Text(
                            text = "v${availableManifest.versionName} — ${availableManifest.releaseTitle}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "SHA-256: ${availableManifest.bundleSha256.take(24)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B91A0)
                        )

                        availableManifest.highlights.forEach { bullet ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(cyanAccent)
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE1E1F3)
                                )
                            }
                        }

                        if (installPhase == OtaInstallPhase.DOWNLOADING_DELTA ||
                            installPhase == OtaInstallPhase.VERIFYING_SHA256 ||
                            installPhase == OtaInstallPhase.APPLYING_PATCH
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape),
                                color = cyanAccent,
                                trackColor = Color.White.copy(alpha = 0.12f)
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}% • ${installPhase.statusText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cyanAccent
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCheckForUpdates,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("check_ota_updates_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check OTA", style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = onInstallOtaUpdate,
                                enabled = installPhase != OtaInstallPhase.DOWNLOADING_DELTA &&
                                    installPhase != OtaInstallPhase.VERIFYING_SHA256 &&
                                    installPhase != OtaInstallPhase.APPLYING_PATCH,
                                modifier = Modifier
                                    .weight(1.25f)
                                    .testTag("install_ota_update_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = blueAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (installPhase == OtaInstallPhase.INSTALLED) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.CloudDownload
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (installPhase == OtaInstallPhase.INSTALLED) {
                                        "Re-Verify Patch"
                                    } else {
                                        "Install OTA Now"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Auto-OTA & Security Toggles
                CrystalGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Install OTA Delta Updates",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Automatically verify SHA-256 & apply live patches over Wi-Fi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC0C6D6)
                                )
                            }
                            Switch(
                                checked = autoOtaEnabled,
                                onCheckedChange = { onToggleAutoOta() },
                                modifier = Modifier.testTag("auto_ota_switch")
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = emeraldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Biometric Vault & App Entry Lock",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Require Fingerprint / Face ID (androidx.biometric) for sensitive docs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC0C6D6)
                                )
                            }
                            Switch(
                                checked = biometricVaultEnabled,
                                onCheckedChange = { onToggleBiometricVault() }
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = cyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Lock App Entry Now (Test Biometric Gate)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Immediately lock application screen with AndroidX BiometricPrompt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC0C6D6)
                                )
                            }
                            OutlinedButton(
                                onClick = onToggleAppEntryBiometricGate,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("lock_app_entry_now_button")
                            ) {
                                Text("Lock App", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = if (isOfflineMode) Color(0xFFFBBF24) else cyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Offline-First Room FTS4 Mode",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = if (isOfflineMode) "Operating offline; edits queue in Room" else "Real-time cloud sync active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC0C6D6)
                                )
                            }
                            Switch(
                                checked = isOfflineMode,
                                onCheckedChange = { onToggleOfflineMode() }
                            )
                        }
                    }
                }

                // 4. Installed OTA Update History from Room DB
                if (otaHistory.isNotEmpty()) {
                    Text(
                        text = "Installed OTA Patch History (Room DB):",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC0C6D6)
                    )
                    otaHistory.take(4).forEach { record ->
                        val dateStr = remember(record.installedAtMillis) {
                            SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US).format(Date(record.installedAtMillis))
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "v${record.versionName} (${record.channel})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = emeraldAccent
                                    )
                                    Text(
                                        text = record.releaseTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "SHA-256: ${record.bundleSha256.take(16)}... • $dateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8B91A0)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Installed",
                                    tint = emeraldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = cyanAccent, contentColor = Color.Black)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onSignOut()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFFFFB4AB),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sign Out", color = Color(0xFFFFB4AB))
            }
        }
    )
}
