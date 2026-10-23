package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.AiMicNoiseCleanController
import com.example.data.util.AiMicTelemetry
import com.example.data.util.NoiseCleanLevel
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiMicDictationBottomSheet(
    palette: DocFlowPalette,
    initialText: String = "",
    confirmButtonLabel: String = "Insert Clean Text",
    onDismiss: () -> Unit,
    onConfirmCleanText: (String) -> Unit
) {
    val context = LocalContext.current
    var transcriptText by remember { mutableStateOf(initialText) }
    var isRecording by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("en-US") }
    var selectedNoiseLevel by remember { mutableStateOf(NoiseCleanLevel.HIGH) }
    var telemetry by remember { mutableStateOf(AiMicTelemetry()) }
    var statusNotice by remember {
        mutableStateOf("Hardware DSP Noise Suppressor + Spectral Gate ready • Stored on this device")
    }

    val controller = remember(context) {
        AiMicNoiseCleanController(
            context = context,
            onPartialTranscript = { partial ->
                if (partial.isNotBlank()) {
                    transcriptText = partial
                }
            },
            onFinalTranscript = { finalClean ->
                isRecording = false
                if (finalClean.isNotBlank()) {
                    transcriptText = if (transcriptText.isBlank()) {
                        finalClean
                    } else {
                        "${transcriptText.trim()} $finalClean"
                    }
                    statusNotice = "Noise-cleaned voice converted to text (-${selectedNoiseLevel.attenuationDb} dB ambient noise removed)"
                }
            },
            onTelemetryUpdate = { updated ->
                telemetry = updated
            },
            onErrorState = { message ->
                isRecording = false
                statusNotice = message
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.stopListening()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isRecording = true
            statusNotice = "Listening with ${selectedNoiseLevel.label}..."
            controller.startListening(selectedLanguage, selectedNoiseLevel)
        } else {
            isRecording = false
            statusNotice = "Microphone permission declined. You can still type or use keyboard voice dictation below with Noise Clean."
        }
    }

    DocFlowBottomSheet(
        onDismissRequest = {
            controller.stopListening()
            onDismiss()
        },
        palette = palette
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DocFlowIconCircle(
                    icon = Icons.Filled.GraphicEq,
                    circleColor = DocFlowTokens.IconCircleTeal,
                    size = 44.dp
                )
                Column {
                    Text(
                        text = "AI Mic • Sound to Text",
                        style = DocFlowTokens.sectionLabelStyle,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "On-device Noise Clean • English & हिन्दी • Stored on this device",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // DSP Telemetry Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DocFlowTokens.buttonShape)
                .background(palette.background)
                .border(1.dp, palette.divider, DocFlowTokens.buttonShape)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRecording) "LIVE MIC • NOISE GATE ACTIVE" else "DSP AUDIO CLEANER READY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) DocFlowTokens.IconCircleGreen else DocFlowTokens.AccentBlue
                    )
                    Text(
                        text = "-${selectedNoiseLevel.attenuationDb} dB Noise Suppression • ${telemetry.voiceClarityPercent}% Clarity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary
                    )
                }
                Text(
                    text = statusNotice,
                    style = DocFlowTokens.captionStyle,
                    color = palette.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Language & Noise Clean Level Chips
        Text(
            text = "Voice Language & Noise Suppression",
            style = DocFlowTokens.captionStyle,
            fontWeight = FontWeight.SemiBold,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedLanguage == "en-US",
                onClick = { selectedLanguage = "en-US" },
                label = { Text("English (Offline)") },
                modifier = Modifier.testTag("ai_mic_lang_en")
            )
            FilterChip(
                selected = selectedLanguage == "hi-IN",
                onClick = { selectedLanguage = "hi-IN" },
                label = { Text("हिन्दी (Hindi Offline)") },
                modifier = Modifier.testTag("ai_mic_lang_hi")
            )
            NoiseCleanLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedNoiseLevel == level,
                    onClick = {
                        selectedNoiseLevel = level
                        transcriptText = AiMicNoiseCleanController.cleanTranscribedText(transcriptText, level)
                    },
                    label = { Text(level.label, fontSize = 12.sp) },
                    modifier = Modifier.testTag("ai_mic_level_${level.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Record / Stop & Noise Clean Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(DocFlowTokens.minTouchTarget)
                    .clip(DocFlowTokens.pillButtonShape)
                    .background(if (isRecording) DocFlowTokens.IconCircleRed else DocFlowTokens.AccentBlue)
                    .clickable {
                        if (isRecording) {
                            controller.stopListening()
                            isRecording = false
                            statusNotice = "Mic stopped • Noise Clean filter applied"
                        } else {
                            if (controller.hasRecordPermission()) {
                                isRecording = true
                                statusNotice = "Listening (${selectedNoiseLevel.label})..."
                                controller.startListening(selectedLanguage, selectedNoiseLevel)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                    .testTag("ai_mic_record_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop AI Mic" else "Start AI Mic",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isRecording) "Stop Listening" else "Start AI Mic (Noise Clean)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            DocFlowSecondaryButton(
                text = "Clean Noise",
                onClick = {
                    transcriptText = AiMicNoiseCleanController.cleanTranscribedText(transcriptText, selectedNoiseLevel)
                    statusNotice = "Removed acoustic fillers, background artifacts & normalized punctuation"
                },
                palette = palette,
                modifier = Modifier.testTag("ai_mic_clean_text_button")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = transcriptText,
            onValueChange = { transcriptText = it },
            label = { Text("Voice-to-Text Transcript (Editable • Noise Cleaned)") },
            placeholder = {
                Text("Speak with AI Mic or use keyboard voice input. Background noise & filler words ('um', 'uh', '[noise]') are stripped automatically...")
            },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_mic_transcript_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DocFlowSecondaryButton(
                text = "Cancel",
                onClick = {
                    controller.stopListening()
                    onDismiss()
                },
                palette = palette,
                modifier = Modifier.weight(1f)
            )
            DocFlowPrimaryButton(
                text = confirmButtonLabel,
                onClick = {
                    controller.stopListening()
                    val cleaned = AiMicNoiseCleanController.cleanTranscribedText(transcriptText, selectedNoiseLevel)
                    onConfirmCleanText(cleaned)
                },
                enabled = transcriptText.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_mic_confirm_button")
            )
        }
    }
}
