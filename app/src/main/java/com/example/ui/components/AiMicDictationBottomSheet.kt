package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.util.AiMicNoiseCleanController
import com.example.data.util.AiMicTelemetry
import com.example.data.util.NoiseCleanLevel
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import kotlinx.coroutines.delay

enum class AiMicUiState {
    IDLE,
    ASKING_PERMISSION,
    LISTENING,
    PROCESSING,
    PERMISSION_DENIED,
    ERROR
}

@Composable
fun AiMicDictationBottomSheet(
    palette: DocFlowPalette,
    initialText: String = "",
    confirmButtonLabel: String = "Insert Text",
    onDismiss: () -> Unit,
    onConfirmCleanText: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var transcriptText by remember { mutableStateOf(initialText) }
    var livePartialText by remember { mutableStateOf("") }
    val dictationHistory = remember { mutableStateListOf<String>() }

    var micState by remember { mutableStateOf(AiMicUiState.IDLE) }
    var selectedLanguage by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getAiMicLanguageCode(context))
    }
    var noiseCleanEnabled by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.isAiMicNoiseSuppressionEnabled(context))
    }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var telemetry by remember { mutableStateOf(AiMicTelemetry()) }
    var statusNotice by remember {
        mutableStateOf("Ready to dictate on this device (English, Hindi, Hinglish)")
    }

    val activeNoiseLevel = if (noiseCleanEnabled) NoiseCleanLevel.HIGH else NoiseCleanLevel.STANDARD

    val controller = remember(context) {
        AiMicNoiseCleanController(
            context = context,
            onPartialTranscript = { partial ->
                if (partial.isNotBlank()) {
                    livePartialText = partial
                }
            },
            onFinalTranscript = { finalClean ->
                livePartialText = ""
                micState = AiMicUiState.IDLE
                if (finalClean.isNotBlank()) {
                    val cleanedChunk = if (noiseCleanEnabled) {
                        AiMicNoiseCleanController.cleanTranscribedText(finalClean, NoiseCleanLevel.HIGH)
                    } else {
                        finalClean.trim()
                    }
                    dictationHistory.add(transcriptText)
                    transcriptText = if (transcriptText.isBlank()) {
                        cleanedChunk
                    } else {
                        "${transcriptText.trim()} $cleanedChunk"
                    }
                    statusNotice = "Dictation added • Tap Undo to remove last phrase"
                } else {
                    statusNotice = "No speech detected. Tap the microphone to try again."
                }
            },
            onTelemetryUpdate = { updated ->
                telemetry = updated
            },
            onErrorState = { message ->
                livePartialText = ""
                micState = AiMicUiState.ERROR
                statusNotice = message
            }
        )
    }

    // Timer while listening
    LaunchedEffect(micState) {
        if (micState == AiMicUiState.LISTENING) {
            elapsedSeconds = 0
            while (micState == AiMicUiState.LISTENING) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Stop listening immediately when screen locks or app is backgrounded
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (micState == AiMicUiState.LISTENING) {
                    controller.stopListening()
                    livePartialText = ""
                    micState = AiMicUiState.IDLE
                    statusNotice = "Microphone stopped because the screen locked or app moved to background."
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.stopListening()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micState = AiMicUiState.LISTENING
            statusNotice = "Listening..."
            val recognizerLocale = if (selectedLanguage == "hinglish") "en-IN" else selectedLanguage
            controller.startListening(recognizerLocale, activeNoiseLevel)
        } else {
            micState = AiMicUiState.PERMISSION_DENIED
            statusNotice = "Microphone permission is required for voice dictation."
        }
    }

    DocFlowBottomSheet(
        onDismissRequest = {
            controller.stopListening()
            onDismiss()
        },
        palette = palette
    ) {
        // Header Row (Microphone indicator shown ONLY while listening)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (micState == AiMicUiState.LISTENING) DocFlowTokens.IconCircleRed.copy(alpha = 0.15f)
                            else DocFlowTokens.SelectedRowTintLight
                        )
                        .border(
                            width = if (micState == AiMicUiState.LISTENING) 2.dp else 1.dp,
                            color = if (micState == AiMicUiState.LISTENING) DocFlowTokens.IconCircleRed else DocFlowTokens.AccentBlue,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (micState == AiMicUiState.LISTENING) Icons.Filled.GraphicEq else Icons.Filled.Mic,
                        contentDescription = "AI Mic",
                        tint = if (micState == AiMicUiState.LISTENING) DocFlowTokens.IconCircleRed else DocFlowTokens.AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "AI Mic",
                        style = DocFlowTokens.sectionLabelStyle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (micState == AiMicUiState.LISTENING) {
                            val mins = elapsedSeconds / 60
                            val secs = elapsedSeconds % 60
                            "Listening • %02d:%02d".format(mins, secs)
                        } else {
                            "On-device voice dictation • Stored on this device"
                        },
                        fontSize = 13.sp,
                        color = if (micState == AiMicUiState.LISTENING) DocFlowTokens.IconCircleRed else palette.textSecondary
                    )
                }
            }

            if (dictationHistory.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val previous = dictationHistory.removeAt(dictationHistory.lastIndex)
                        transcriptText = previous
                        statusNotice = "Undid last dictation phrase"
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_mic_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo last dictation",
                        tint = DocFlowTokens.AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Waveform & Status Banner when listening or error
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DocFlowTokens.buttonShape)
                .background(palette.background)
                .border(1.dp, palette.divider, DocFlowTokens.buttonShape)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (micState == AiMicUiState.LISTENING) {
                    // Live waveform indicator bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val heights = listOf(10.dp, 18.dp, 24.dp, 14.dp, 22.dp, 12.dp, 20.dp, 16.dp)
                            heights.forEachIndexed { idx, baseH ->
                                val activeH = if ((elapsedSeconds + idx) % 2 == 0) baseH else (baseH / 2).coerceAtLeast(6.dp)
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(activeH)
                                        .clip(CircleShape)
                                        .background(DocFlowTokens.AccentBlue)
                                )
                            }
                        }
                        Text(
                            text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DocFlowTokens.IconCircleRed
                        )
                    }
                }
                Text(
                    text = statusNotice,
                    fontSize = 13.sp,
                    color = if (micState == AiMicUiState.ERROR || micState == AiMicUiState.PERMISSION_DENIED)
                        DocFlowTokens.IconCircleRed
                    else palette.textPrimary
                )
                if (livePartialText.isNotBlank()) {
                    Text(
                        text = livePartialText,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = palette.textSecondary,
                        modifier = Modifier.testTag("ai_mic_live_partial_text")
                    )
                }
            }
        }

        // Permission Denied Banner with "Open settings" button
        if (micState == AiMicUiState.PERMISSION_DENIED) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DocFlowTokens.IconCircleRed.copy(alpha = 0.1f))
                    .border(1.dp, DocFlowTokens.IconCircleRed, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Microphone access is turned off",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Enable Microphone permission in Android Settings to use voice dictation.",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
                TextButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.testTag("ai_mic_open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = DocFlowTokens.AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open settings", fontSize = 13.sp, color = DocFlowTokens.AccentBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Languages: English, Hindi, Hinglish (single-line chips, remembered locally)
        Text(
            text = "Dictation language",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val languages = listOf(
                Triple("en-US", "English", "ai_mic_lang_en"),
                Triple("hi-IN", "Hindi (हिन्दी)", "ai_mic_lang_hi"),
                Triple("hinglish", "Hinglish", "ai_mic_lang_hinglish")
            )
            languages.forEach { (code, label, tag) ->
                FilterChip(
                    selected = selectedLanguage == code,
                    onClick = {
                        selectedLanguage = code
                        OnDeviceAiAndBackupEngine.saveAiMicLanguageCode(context, code)
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag(tag)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Noise Clean switch inside the mic sheet settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Noise clean",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = "Hardware DSP NoiseSuppressor + acoustic filler removal",
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
            }
            Switch(
                checked = noiseCleanEnabled,
                onCheckedChange = { enabled ->
                    noiseCleanEnabled = enabled
                    OnDeviceAiAndBackupEngine.setAiMicNoiseSuppressionEnabled(context, enabled)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = DocFlowTokens.AccentBlue
                ),
                modifier = Modifier.testTag("ai_mic_noise_clean_switch")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Start / Stop Listening Button (48dp height, 24dp icon)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(DocFlowTokens.pillButtonShape)
                .background(
                    if (micState == AiMicUiState.LISTENING) DocFlowTokens.IconCircleRed
                    else DocFlowTokens.AccentBlue
                )
                .clickable {
                    if (micState == AiMicUiState.LISTENING) {
                        controller.stopListening()
                        livePartialText = ""
                        micState = AiMicUiState.IDLE
                        statusNotice = "Stopped listening"
                    } else {
                        if (controller.hasRecordPermission()) {
                            micState = AiMicUiState.LISTENING
                            statusNotice = "Listening..."
                            val recognizerLocale = if (selectedLanguage == "hinglish") "en-IN" else selectedLanguage
                            controller.startListening(recognizerLocale, activeNoiseLevel)
                        } else {
                            micState = AiMicUiState.ASKING_PERMISSION
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
                    imageVector = if (micState == AiMicUiState.LISTENING) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (micState == AiMicUiState.LISTENING) "Stop" else "Start listening",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (micState == AiMicUiState.LISTENING) "Stop" else "Start Listening",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = transcriptText,
            onValueChange = { transcriptText = it },
            label = { Text("Dictated text", fontSize = 13.sp) },
            placeholder = {
                Text(
                    "Speak or type here...",
                    fontSize = 14.sp,
                    color = palette.textSecondary
                )
            },
            minLines = 3,
            maxLines = 6,
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
                    val output = if (noiseCleanEnabled) {
                        AiMicNoiseCleanController.cleanTranscribedText(transcriptText, NoiseCleanLevel.HIGH)
                    } else {
                        transcriptText.trim()
                    }
                    onConfirmCleanText(output)
                },
                enabled = transcriptText.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_mic_confirm_button")
            )
        }
    }
}
