package com.apoorvdarshan.ai.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.R
import com.apoorvdarshan.ai.models.SpeechLanguage
import com.apoorvdarshan.ai.models.SpeechProvider
import com.apoorvdarshan.ai.services.speech.AudioRecorder
import com.apoorvdarshan.ai.services.speech.NativeSpeechRecognizer
import com.apoorvdarshan.ai.services.speech.SttEvent
import com.apoorvdarshan.ai.ui.theme.AppColors
import com.apoorvdarshan.ai.ui.settings.LocalGlassTheme
import com.apoorvdarshan.ai.ui.theme.glowShadow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

private enum class VoicePhase { IDLE, RECORDING, REVIEWING, TRANSCRIBING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputSheet(
    container: AppContainer,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Using Glass Theme
    val glass = LocalGlassTheme.current

    val provider by container.prefs.selectedSpeechProvider.collectAsState(initial = SpeechProvider.NATIVE)
    val speechLanguage by container.prefs.selectedSpeechLanguage(provider)
        .collectAsState(initial = SpeechLanguage.defaultFor(provider))
    val micDeniedMsg = stringResource(R.string.voice_mic_permission_denied)
    val micStartFailedMsg = stringResource(R.string.voice_mic_start_failed)
    val transcriptionFailedMsg = stringResource(R.string.voice_transcription_failed)

    var phase by remember { mutableStateOf(VoicePhase.IDLE) }
    var transcript by remember { mutableStateOf("") }
    var committed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val recorder = remember(ctx) { AudioRecorder(ctx) }
    val native = remember(ctx) { NativeSpeechRecognizer(ctx) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var nativeJob by remember { mutableStateOf<Job?>(null) }

    fun launchNativeListenerLoop() {
        nativeJob?.cancel()
        nativeJob = scope.launch {
            native.listen(locale = speechLanguage.nativeLocaleTag()).collectLatest { event ->
                when (event) {
                    is SttEvent.Partial -> {
                        transcript = (committed + " " + event.text).trim()
                    }
                    is SttEvent.Final -> {
                        committed = (committed + " " + event.text).trim()
                        transcript = committed
                        if (phase == VoicePhase.RECORDING) {
                            kotlinx.coroutines.delay(250)
                            if (phase == VoicePhase.RECORDING) launchNativeListenerLoop()
                        }
                    }
                    is SttEvent.Error -> {
                        val recoverable = event.code in setOf(6, 7, 8, 11)
                        if (recoverable && phase == VoicePhase.RECORDING) {
                            kotlinx.coroutines.delay(300)
                            if (phase == VoicePhase.RECORDING) launchNativeListenerLoop()
                        } else {
                            error = event.message
                            phase = VoicePhase.IDLE
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun startRecordingNow() {
        transcript = ""
        committed = ""
        error = null
        if (provider == SpeechProvider.NATIVE) {
            phase = VoicePhase.RECORDING
            launchNativeListenerLoop()
        } else {
            val file = recorder.start()
            if (file == null) {
                error = micStartFailedMsg
            } else {
                recordedFile = file
                phase = VoicePhase.RECORDING
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) error = micDeniedMsg
    }

    LaunchedEffect(Unit) {
        if (!native.hasMicPermission()) micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeJob?.cancel()
            recorder.cancel()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            nativeJob?.cancel()
            recorder.cancel()
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.bgTop // Glass theme background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Provider pill
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(AppColors.Calorie.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = AppColors.Calorie,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(provider.displayNameRes),
                    color = AppColors.Calorie,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))

            // Glassmorphism Transcript Box
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(glass.surface)
                    .border(
                        1.dp, 
                        Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)), 
                        RoundedCornerShape(18.dp)
                    )
                    .glowShadow(AppColors.Calorie.copy(alpha = 0.04f), radius = 24.dp, offsetY = 8.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                when {
                    phase == VoicePhase.TRANSCRIBING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = AppColors.Calorie,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                stringResource(R.string.voice_transcribing_format, stringResource(provider.displayNameRes)),
                                fontSize = 14.sp,
                                color = glass.textPrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                    transcript.isNotEmpty() -> {
                        if (phase == VoicePhase.REVIEWING) {
                            OutlinedTextField(
                                value = transcript,
                                onValueChange = { transcript = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = glass.textPrimary,
                                    unfocusedTextColor = glass.textPrimary,
                                    focusedBorderColor = AppColors.Calorie,
                                    unfocusedBorderColor = glass.border,
                                    cursorColor = AppColors.Calorie,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                            )
                        } else {
                            Text(transcript, fontSize = 16.sp, color = glass.textPrimary)
                        }
                    }
                    else -> {
                        Text(
                            if (phase == VoicePhase.RECORDING) stringResource(R.string.voice_listening) else stringResource(R.string.voice_tap_to_start),
                            fontSize = 16.sp,
                            color = glass.textPrimary.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                MicButton(
                    phase = phase,
                    onToggle = {
                        when (phase) {
                            VoicePhase.IDLE -> startRecordingNow()
                            VoicePhase.RECORDING -> {
                                if (provider == SpeechProvider.NATIVE) {
                                    nativeJob?.cancel()
                                    phase = VoicePhase.REVIEWING
                                } else {
                                    val file = recorder.stop()
                                    if (file != null) {
                                        phase = VoicePhase.TRANSCRIBING
                                        scope.launch {
                                            try {
                                                transcript = container.speechService.transcribeRemote(file)
                                                phase = VoicePhase.REVIEWING
                                            } catch (e: Throwable) {
                                                error = e.localizedMessage ?: transcriptionFailedMsg
                                                phase = VoicePhase.IDLE
                                            }
                                        }
                                    } else {
                                        phase = VoicePhase.IDLE
                                    }
                                }
                            }
                            VoicePhase.REVIEWING -> startRecordingNow()
                            VoicePhase.TRANSCRIBING -> Unit
                        }
                    }
                )
            }

            val canAnalyze = transcript.trim().isNotEmpty() && phase != VoicePhase.TRANSCRIBING
            Spacer(Modifier.height(20.dp))
            
            // Sleek Gradient Analyze Button replacing standard Material Button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (canAnalyze) Brush.linearGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd)) 
                        else Brush.linearGradient(listOf(AppColors.Calorie.copy(alpha = 0.4f), AppColors.Calorie.copy(alpha = 0.4f)))
                    )
                    .clickable(enabled = canAnalyze) {
                        if (provider == SpeechProvider.NATIVE && phase == VoicePhase.RECORDING) {
                            nativeJob?.cancel()
                            phase = VoicePhase.REVIEWING
                        }
                        if (transcript.trim().isNotEmpty()) onSubmit(transcript.trim())
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.action_analyze),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF3B30), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                nativeJob?.cancel()
                recorder.cancel()
                onDismiss()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_cancel), color = glass.textPrimary.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun MicButton(phase: VoicePhase, onToggle: () -> Unit) {
    val recording = phase == VoicePhase.RECORDING
    val infinite = rememberInfiniteTransition(label = "micPulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (recording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val scale by animateFloatAsState(
        targetValue = if (recording) pulse else 1f,
        animationSpec = tween(200),
        label = "micScale"
    )
    val bgBrush = if (recording)
        Brush.linearGradient(listOf(Color(0xFFFF3B30), Color(0xFFFF6B60)))
    else
        Brush.linearGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd))
    val interactionSource = remember { MutableInteractionSource() }
    
    // Glass halo effect around the mic
    val glass = LocalGlassTheme.current
    
    Box(
        Modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer subtle glass ring
        Box(
            Modifier
                .size((90 * scale).dp)
                .clip(CircleShape)
                .background(glass.surface.copy(alpha = 0.3f))
                .border(1.dp, glass.border.copy(alpha = 0.5f), CircleShape)
        )
        
        // Inner actual button
        Box(
            Modifier
                .size((72 * scale).dp)
                .clip(CircleShape)
                .background(bgBrush)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (recording) Icons.Filled.Mic else Icons.Filled.MicNone,
                contentDescription = if (recording) stringResource(R.string.voice_stop) else stringResource(R.string.voice_record),
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
