package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.DocFlowLegalConstants
import com.example.data.util.NoteLinkDecodeResult
import com.example.data.util.NoteLinkExpiryOption
import com.example.data.util.NoteLinkGenerationResult
import com.example.data.util.NoteShareLinkEngine
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.data.util.SharedNoteLinkRecord
import com.example.data.util.SharedNotePayload
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Share Note with Link Options & Result Bottom Sheet (Section 4):
 * - Link format: https://docflow.free.je/n/#v1.<payload>
 * - Options: Access ("Anyone with the link can read", view-only), Password (AES-256-GCM + PBKDF2),
 *   Expiry (1 day / 7 days / 30 days / Never)
 * - Size limit: if > 6,000 chars, warns user and offers Share as PDF/file instead
 * - Result sheet: Link, Copy, Share (system share sheet), QR code, and warning banner
 */
@Composable
fun ShareNoteWithLinkBottomSheet(
    noteTitle: String,
    noteBody: String,
    noteCreatedAtMillis: Long = System.currentTimeMillis(),
    palette: DocFlowPalette,
    onDismiss: () -> Unit,
    onShareAsPdfFallback: () -> Unit = {},
    onLinkSavedToHistory: (SharedNoteLinkRecord) -> Unit = {}
) {
    val context = LocalContext.current
    var expiryOption by remember { mutableStateOf(NoteLinkExpiryOption.NEVER) }
    var usePassword by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var generatedResult by remember { mutableStateOf<NoteLinkGenerationResult?>(null) }
    var copiedFeedback by remember { mutableStateOf(false) }

    DocFlowBottomSheet(
        onDismissRequest = onDismiss,
        palette = palette
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("share_note_with_link_sheet")
        ) {
            val currentRes = generatedResult
            if (currentRes !is NoteLinkGenerationResult.Success) {
                Text(
                    text = "Share note with link",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "The note is compressed into the link fragment (#v1) and never uploaded to a server.",
                    fontSize = 13.sp,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Access row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.background)
                        .border(1.dp, palette.divider, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = DocFlowTokens.AccentBlue
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Anyone with the link can read",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "View-only • Processed in browser or DocFlow app",
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Expiry selector (1 day / 7 days / 30 days / Never)
                Text(
                    text = "Link expiry",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoteLinkExpiryOption.entries.forEach { option ->
                        FilterChip(
                            selected = expiryOption == option,
                            onClick = { expiryOption = option },
                            label = { Text(option.label, fontSize = 13.sp) },
                            modifier = Modifier.testTag("share_link_expiry_${option.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Password (AES-256-GCM with PBKDF2-derived key)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Protect with password (AES-256-GCM)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Password is never put in the link; derived with PBKDF2",
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                    Switch(
                        checked = usePassword,
                        onCheckedChange = { usePassword = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DocFlowTokens.AccentBlue
                        ),
                        modifier = Modifier.testTag("share_link_password_switch")
                    )
                }

                if (usePassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Encryption password", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_link_password_input")
                    )
                }

                if (currentRes is NoteLinkGenerationResult.TooLarge) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DocFlowTokens.IconCircleRed.copy(alpha = 0.1f))
                            .border(1.dp, DocFlowTokens.IconCircleRed, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                            .testTag("share_link_too_large_notice"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = DocFlowTokens.IconCircleRed
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Note exceeds 6,000-character link limit (${currentRes.charCount} chars)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DocFlowTokens.IconCircleRed
                            )
                            Text(
                                text = "Links are never truncated. Share this note as a PDF or file instead.",
                                fontSize = 12.sp,
                                color = palette.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    DocFlowPrimaryButton(
                        text = "Share as PDF / File Instead",
                        onClick = {
                            onDismiss()
                            onShareAsPdfFallback()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_link_fallback_pdf_button")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DocFlowSecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    DocFlowPrimaryButton(
                        text = "Create Link",
                        enabled = !usePassword || passwordInput.isNotBlank(),
                        onClick = {
                            val res = NoteShareLinkEngine.generateShareLink(
                                title = noteTitle,
                                body = noteBody,
                                createdAtMillis = noteCreatedAtMillis,
                                expiryOption = expiryOption,
                                password = if (usePassword) passwordInput else ""
                            )
                            generatedResult = res
                            if (res is NoteLinkGenerationResult.Success) {
                                OnDeviceAiAndBackupEngine.saveSharedLinkRecord(context, res.record)
                                onLinkSavedToHistory(res.record)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_link_generate_button")
                    )
                }
            } else {
                // Result sheet: Link, Copy, Share, QR code, and non-revocable warning
                val qrBitmap = remember(currentRes.url) {
                    NoteShareLinkEngine.generateQrCodeBitmap(currentRes.url, 320)
                }

                Text(
                    text = "Shareable Note Link Ready",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Warning box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DocFlowTokens.IconCircleOrange.copy(alpha = 0.12f))
                        .border(1.dp, DocFlowTokens.IconCircleOrange, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                        .testTag("share_link_warning_banner"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = DocFlowTokens.IconCircleOrange
                    )
                    Text(
                        text = "Anyone who has this link can read this note. A link cannot be revoked once it is shared.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Link box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.background)
                        .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = currentRes.url,
                        fontSize = 12.sp,
                        color = DocFlowTokens.AccentBlue,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("share_link_result_url_text")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // QR Code Preview
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, palette.divider, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code for shared note link",
                            modifier = Modifier
                                .size(148.dp)
                                .testTag("share_link_qr_code")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Scan to open in browser or DocFlow",
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DocFlowSecondaryButton(
                        text = if (copiedFeedback) "Copied!" else "Copy",
                        onClick = {
                            copyLinkToClipboard(context, currentRes.url)
                            copiedFeedback = true
                        },
                        palette = palette,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_link_copy_button")
                    )
                    DocFlowPrimaryButton(
                        text = "Share",
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, noteTitle)
                                putExtra(Intent.EXTRA_TEXT, currentRes.url)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Share note link").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_link_system_share_button")
                    )
                }
            }
        }
    }
}

/**
 * Local-only "My shared links" sheet & in-app #v1.<payload> link decoder/importer.
 */
@Composable
fun MySharedLinksBottomSheet(
    palette: DocFlowPalette,
    onDismiss: () -> Unit,
    onImportSharedNote: (SharedNotePayload) -> Unit = {}
) {
    val context = LocalContext.current
    var links by remember { mutableStateOf(OnDeviceAiAndBackupEngine.getSavedSharedLinks(context)) }
    var pasteLinkInput by remember { mutableStateOf("") }
    var passwordPromptInput by remember { mutableStateOf("") }
    var decodeResult by remember { mutableStateOf<NoteLinkDecodeResult?>(null) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    DocFlowBottomSheet(
        onDismissRequest = onDismiss,
        palette = palette
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("my_shared_links_sheet")
        ) {
            Text(
                text = "My shared links (Stored on this device)",
                style = DocFlowTokens.sectionLabelStyle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "Removing a link from this list only clears your local history; it does not disable or revoke the link.",
                fontSize = 12.sp,
                color = palette.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Open / Verify / Import a shared note link (#v1.<payload>)
            OutlinedTextField(
                value = pasteLinkInput,
                onValueChange = {
                    pasteLinkInput = it
                    decodeResult = null
                },
                label = { Text("Open or verify a ${DocFlowLegalConstants.NOTE_SHARE_BASE_URL}... link", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shared_link_verify_input")
            )

            if (decodeResult is NoteLinkDecodeResult.PasswordRequired || decodeResult is NoteLinkDecodeResult.WrongPassword) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passwordPromptInput,
                    onValueChange = { passwordPromptInput = it },
                    label = {
                        Text(
                            if (decodeResult is NoteLinkDecodeResult.WrongPassword)
                                "Incorrect password — try again"
                            else "Enter note password",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shared_link_decrypt_password_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            DocFlowSecondaryButton(
                text = "Decode & Preview Link",
                onClick = {
                    decodeResult = NoteShareLinkEngine.decodeShareLink(pasteLinkInput, passwordPromptInput)
                },
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shared_link_decode_button")
            )

            when (val res = decodeResult) {
                is NoteLinkDecodeResult.Success -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    DocFlowCard(palette = palette) {
                        Text(
                            text = res.payload.title,
                            style = DocFlowTokens.rowTitleStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = res.payload.body,
                            style = DocFlowTokens.bodyStyle,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DocFlowPrimaryButton(
                            text = "Save as note",
                            onClick = {
                                onImportSharedNote(res.payload)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shared_link_save_as_note_button")
                        )
                    }
                }
                is NoteLinkDecodeResult.Invalid -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This link is invalid: ${res.reason}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DocFlowTokens.IconCircleRed,
                        modifier = Modifier.testTag("shared_link_invalid_error")
                    )
                }
                is NoteLinkDecodeResult.Expired -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This shared note link has expired.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DocFlowTokens.IconCircleRed
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (links.isEmpty()) {
                DocFlowEmptyState(
                    icon = Icons.Filled.Link,
                    title = "No shared links yet",
                    subtitle = "Use 'Share with link' on any note to create a client-side link.",
                    palette = palette
                )
            } else {
                links.forEachIndexed { idx, rec ->
                    val createdStr = dateFormatter.format(Date(rec.createdAtMillis))
                    val expiryStr = rec.expiresAtMillis?.let { "Expires ${dateFormatter.format(Date(it))}" } ?: "Never expires"
                    DocFlowListRow(
                        title = rec.noteTitle,
                        subtitle = "Created $createdStr • $expiryStr${if (rec.hasPassword) " • Password" else ""}",
                        palette = palette,
                        modifier = Modifier.testTag("my_shared_link_row_$idx"),
                        onClick = {
                            pasteLinkInput = rec.shareUrl
                            decodeResult = NoteShareLinkEngine.decodeShareLink(rec.shareUrl)
                        },
                        leading = {
                            DocFlowIconCircle(
                                icon = if (rec.hasPassword) Icons.Filled.Lock else Icons.Filled.Link,
                                circleColor = DocFlowTokens.AccentBlue
                            )
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { copyLinkToClipboard(context, rec.shareUrl) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy link",
                                        tint = DocFlowTokens.AccentBlue
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        OnDeviceAiAndBackupEngine.removeSharedLinkRecord(context, rec.id)
                                        links = OnDeviceAiAndBackupEngine.getSavedSharedLinks(context)
                                    },
                                    modifier = Modifier.testTag("remove_shared_link_$idx")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove from list",
                                        tint = DocFlowTokens.IconCircleRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Remove", fontSize = 12.sp, color = DocFlowTokens.IconCircleRed)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun copyLinkToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("DocFlow Shared Note Link", text))
}
