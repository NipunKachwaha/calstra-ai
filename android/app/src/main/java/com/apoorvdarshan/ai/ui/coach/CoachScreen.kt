package com.apoorvdarshan.ai.ui.coach

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.R
import com.apoorvdarshan.ai.models.ChatMessage
import com.apoorvdarshan.ai.ui.theme.AppColors
import com.apoorvdarshan.ai.ui.theme.glowShadow
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64

// ─────────────────────────────────────────────────────────────────────────────
//  Glass Theme System (Ideally move this to a shared file later)
// ─────────────────────────────────────────────────────────────────────────────

data class GlassTheme(
    val surface: Color,
    val surfaceHover: Color,
    val border: Color,
    val borderStrong: Color,
    val shimmer: Color,
    val divider: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val textPrimary: Color,
    val orbA: Color,
    val orbB: Color,
    val orbC: Color
)

val DarkGlass = GlassTheme(
    surface      = Color(0x26FFFFFF),
    surfaceHover = Color(0x33FFFFFF),
    border       = Color(0x40FFFFFF),
    borderStrong = Color(0x66FFFFFF),
    shimmer      = Color(0x0DFFFFFF),
    divider      = Color(0x18FFFFFF),
    bgTop        = Color(0xFF0D0D0F),
    bgBottom     = Color(0xFF14101A),
    textPrimary  = Color.White,
    orbA         = Color(0x40FF6B8A),
    orbB         = Color(0x28C94B7D),
    orbC         = Color(0x18FF8C5A)
)

val LightGlass = GlassTheme(
    surface      = Color(0x40FFFFFF),
    surfaceHover = Color(0x66FFFFFF),
    border       = Color(0x20000000),
    borderStrong = Color(0x40000000),
    shimmer      = Color(0x0D000000),
    divider      = Color(0x10000000),
    bgTop        = Color(0xFFF2F2F7),
    bgBottom     = Color(0xFFE5E5EA),
    textPrimary  = Color(0xFF1C1C1E),
    orbA         = Color(0x40FF6B8A),
    orbB         = Color(0x28C94B7D),
    orbC         = Color(0x18FF8C5A)
)

val LocalGlassTheme = staticCompositionLocalOf { DarkGlass }

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────────────────

private object CoachTokens {
    val RadiusXl     = 28.dp
    val RadiusBubble = 22.dp
    val RadiusCard   = 16.dp
    val RadiusChip   = 20.dp
    val ElevationCard = 20.dp
    val ElevationSend = 12.dp
    val AnimFast   = 200
    val AnimMedium = 350
    val AmbientShadow   = Color.Black.copy(alpha = 0.14f)
    val Accent get() = AppColors.Calorie
}

// ─────────────────────────────────────────────────────────────────────────────
// CoachScreen — root composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(container: AppContainer) {
    val vm: CoachViewModel = viewModel(factory = CoachViewModel.Factory(container))
    val ui by vm.ui.collectAsState()

    var input            by remember { mutableStateOf("") }
    var attachedBytes    by remember { mutableStateOf<ByteArray?>(null) }
    var pendingCapture   by remember { mutableStateOf<File?>(null) }
    var showReset        by remember { mutableStateOf(false) }

    val listState    = rememberLazyListState()
    val ctx          = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard     = LocalSoftwareKeyboardController.current
    val haptic       = LocalHapticFeedback.current

    // Theme logic
    val appearance by container.prefs.appearanceMode.collectAsState(initial = "system")
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (appearance) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val glassTheme = remember(isDarkTheme) { if (isDarkTheme) DarkGlass else LightGlass }

    // ── helpers ──────────────────────────────────────────────────────────────

    fun hideKeyboard() {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    fun dispatchSend() {
        val image   = attachedBytes
        val trimmed = input.trim()
        if (trimmed.isEmpty() && image == null) return
        if (ui.sending) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val aiImage   = image?.let { resizedJpeg(it, 1600, 78) ?: it }
        val thumbnail = image?.let { resizedJpeg(it, 700, 68) ?: it }
        hideKeyboard()
        input = ""
        attachedBytes = null
        vm.send(trimmed, imageBytes = aiImage, thumbnailBytes = thumbnail)
    }

    // ── media launchers ──────────────────────────────────────────────────────

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val raw = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (raw != null) attachedBytes = resizedJpeg(raw, 1800, 86) ?: raw
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val file = pendingCapture.also { pendingCapture = null }
        if (saved == true && file?.exists() == true) {
            val raw = file.readBytes()
            if (raw.isNotEmpty()) attachedBytes = resizedJpeg(raw, 1800, 86) ?: raw
        }
    }

    fun launchCamera() {
        val dir  = File(ctx.cacheDir, "capture").apply { mkdirs() }
        val file = File(dir, "coach-${System.currentTimeMillis()}.jpg")
        val uri  = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        pendingCapture = file
        cameraLauncher.launch(uri)
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) launchCamera()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    // ── auto-scroll to last message ───────────────────────────────────────────

    LaunchedEffect(ui.messages.size, ui.sending) {
        if (ui.messages.isNotEmpty())
            listState.animateScrollToItem(ui.messages.size - 1)
    }

    // ── scaffold ─────────────────────────────────────────────────────────────

    CompositionLocalProvider(LocalGlassTheme provides glassTheme) {
        val glass = LocalGlassTheme.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(glass.bgTop, glass.bgBottom)))
        ) {
            AmbientOrbs(listState = listState)

            Scaffold(
                containerColor = Color.Transparent, // Transparent so orbs are visible
                topBar = {
                    CoachTopBar(
                        hasMessages = ui.messages.isNotEmpty(),
                        onReset     = { showReset = true }
                    )
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {

                    // Message list / empty state
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) { detectTapGestures(onTap = { hideKeyboard() }) }
                    ) {
                        AnimatedContent(
                            targetState = ui.messages.isEmpty(),
                            transitionSpec = {
                                fadeIn(tween(CoachTokens.AnimMedium)) togetherWith
                                fadeOut(tween(CoachTokens.AnimMedium))
                            },
                            label = "emptyVsList"
                        ) { empty ->
                            if (empty) {
                                EmptyState(Modifier.fillMaxSize())
                            } else {
                                val resolvedError = when (val err = ui.error) {
                                    is CoachError.FromResource -> stringResource(err.resId)
                                    is CoachError.Literal      -> err.message.ifBlank {
                                        err.fallbackResId?.let { stringResource(it) }
                                    }
                                    null -> null
                                }
                                MessageList(
                                    messages  = ui.messages,
                                    sending   = ui.sending,
                                    error     = resolvedError,
                                    listState = listState,
                                    modifier  = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Suggestion chips
                    val resolvedChips = ui.suggestions.map { stringResource(it) }
                    AnimatedVisibility(
                        visible = resolvedChips.isNotEmpty(),
                        enter   = fadeIn() + expandVertically(),
                        exit    = fadeOut() + shrinkVertically()
                    ) {
                        PromptChipRow(
                            chips   = resolvedChips,
                            enabled = !ui.sending,
                            onTap   = { chip ->
                                hideKeyboard()
                                input = ""
                                attachedBytes = null
                                vm.send(chip)
                            }
                        )
                    }

                    // Input bar
                    InputBar(
                        value            = input,
                        onValueChange    = { input = it },
                        attachedBytes    = attachedBytes,
                        sending          = ui.sending,
                        onPickImage      = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCaptureImage   = { openCamera() },
                        onRemoveImage    = { attachedBytes = null },
                        onSend           = { dispatchSend() }
                    )
                }
            }
        }
    }

    // ── reset confirmation dialog ─────────────────────────────────────────────

    if (showReset) {
        ResetDialog(
            onConfirm = { vm.resetConversation(); showReset = false },
            onDismiss = { showReset = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ambient Orbs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmbientOrbs(listState: LazyListState) {
    val glass = LocalGlassTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val parallax = listState.firstVisibleItemScrollOffset * 0.15f
        val orbDrift = drift * 30f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbA, Color.Transparent),
                center = Offset(size.width * 0.15f, 180f - parallax + orbDrift),
                radius = 340f
            ),
            radius = 340f,
            center = Offset(size.width * 0.15f, 180f - parallax + orbDrift)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbB, Color.Transparent),
                center = Offset(size.width * 0.82f, 320f - parallax * 0.5f - orbDrift),
                radius = 260f
            ),
            radius = 260f,
            center = Offset(size.width * 0.82f, 320f - parallax * 0.5f - orbDrift)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbC, Color.Transparent),
                center = Offset(size.width * 0.6f, size.height * 0.75f + parallax * 0.3f),
                radius = 300f
            ),
            radius = 300f,
            center = Offset(size.width * 0.6f, size.height * 0.75f + parallax * 0.3f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TopBar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachTopBar(hasMessages: Boolean, onReset: () -> Unit) {
    val glass = LocalGlassTheme.current

    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppColors.CalorieGradient)
                )
                Text(
                    text       = stringResource(R.string.coach_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 17.sp,
                    letterSpacing = 0.3.sp,
                    color      = glass.textPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        actions = {
            AnimatedVisibility(
                visible = hasMessages,
                enter   = fadeIn() + scaleIn(initialScale = 0.7f),
                exit    = fadeOut() + scaleOut(targetScale = 0.7f)
            ) {
                IconButton(
                    onClick  = onReset,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(glass.surfaceHover)
                        .border(1.dp, glass.border, CircleShape)
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.coach_reset_chat_a11y),
                        tint               = glass.textPrimary,
                        modifier           = Modifier.size(17.dp)
                    )
                }
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val glass = LocalGlassTheme.current
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.06f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier              = modifier.padding(horizontal = 36.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(112.dp)
                .scale(pulseScale)
                .glowShadow(CoachTokens.Accent.copy(alpha = 0.20f), radius = 24.dp)
                .clip(CircleShape)
                .background(glass.surface)
                .border(
                    width  = 1.dp,
                    brush  = Brush.linearGradient(listOf(glass.borderStrong, glass.border)),
                    shape  = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier           = Modifier.size(42.dp),
                tint               = CoachTokens.Accent
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text          = stringResource(R.string.coach_empty_title),
            fontSize      = 22.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = glass.textPrimary,
            letterSpacing = (-0.3).sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text       = stringResource(R.string.coach_empty_subtitle),
            fontSize   = 15.sp,
            color      = glass.textPrimary.copy(alpha = 0.55f),
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    messages  : List<ChatMessage>,
    sending   : Boolean,
    error     : String?,
    listState : LazyListState,
    modifier  : Modifier = Modifier
) {
    val glass = LocalGlassTheme.current

    LazyColumn(
        state          = listState,
        modifier       = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(msg.id) { visible = true }
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(CoachTokens.AnimMedium)) +
                          slideInVertically(tween(CoachTokens.AnimMedium)) { it / 3 }
            ) {
                MessageBubble(msg)
            }
        }

        if (sending) {
            item("typing") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistantBadge()
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(CoachTokens.RadiusBubble))
                            .glowShadow(Color.Black.copy(alpha = 0.05f), radius = 12.dp)
                            .background(glass.surface)
                            .border(
                                0.6.dp,
                                Brush.linearGradient(listOf(glass.borderStrong, glass.border)),
                                RoundedCornerShape(CoachTokens.RadiusBubble)
                            )
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        TypingIndicator()
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        if (error != null) {
            item("error") {
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn() + expandVertically()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEBEE).copy(alpha = 0.7f))
                            .border(
                                0.5.dp,
                                Color(0xFFE53935).copy(alpha = 0.30f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint     = Color(0xFFE53935),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text     = error,
                            fontSize = 13.sp,
                            color    = Color(0xFFE53935)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CoachTokens.AnimMedium.toLong())
            phase = (phase + 1) % 3
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val active = i == phase
            val scaleF  by animateFloatAsState(if (active) 1.20f else 1f, tween(CoachTokens.AnimMedium), label = "s$i")
            val alphaF  by animateFloatAsState(if (active) 1f else 0.28f, tween(CoachTokens.AnimMedium), label = "a$i")
            Box(
                Modifier
                    .size(7.dp)
                    .scale(scaleF)
                    .alpha(alphaF)
                    .clip(CircleShape)
                    .background(AppColors.CalorieGradient)
            )
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == ChatMessage.Role.USER
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            AssistantBadge()
            Spacer(Modifier.width(8.dp))
            Bubble(content = msg.content, isUser = false)
            Spacer(Modifier.width(52.dp))
        } else {
            Spacer(Modifier.width(52.dp))
            Bubble(content = msg.content, isUser = true, imageBase64 = msg.attachmentImageBase64)
        }
    }
}

@Composable
private fun AssistantBadge() {
    val glass = LocalGlassTheme.current
    Box(
        Modifier
            .padding(top = 4.dp)
            .size(28.dp)
            .glowShadow(CoachTokens.Accent.copy(alpha = 0.15f), radius = 12.dp)
            .clip(CircleShape)
            .background(glass.surface)
            .background(CoachTokens.Accent.copy(alpha = 0.18f))
            .border(0.6.dp, glass.borderStrong, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier           = Modifier.size(13.dp),
            tint               = CoachTokens.Accent
        )
    }
}

@Composable
private fun Bubble(content: String, isUser: Boolean, imageBase64: String? = null) {
    val glass = LocalGlassTheme.current
    val shape = RoundedCornerShape(CoachTokens.RadiusBubble)
    val shadowColor = if (isUser) CoachTokens.Accent.copy(alpha = 0.25f)
                      else Color.Black.copy(alpha = 0.08f)
    val elevation   = if (isUser) 12.dp else 4.dp

    Box(
        Modifier
            .widthIn(max = 300.dp)
            .shadow(elevation, shape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(shape)
            .then(
                if (isUser)
                    Modifier.background(AppColors.CalorieGradient)
                else
                    Modifier
                        .background(glass.surface)
                        .background(CoachTokens.Accent.copy(alpha = 0.030f))
            )
            .border(
                0.7.dp,
                if (isUser) Brush.linearGradient(listOf(Color.White.copy(0.45f), Color.White.copy(0.05f)))
                else Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)),
                shape
            )
    ) {
        if (isUser) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(0.32f), Color.Transparent)
                        )
                    )
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            imageBase64?.let { encoded ->
                val bitmap = remember(encoded) {
                    runCatching {
                        Base64.getDecoder().decode(encoded).let { b ->
                            BitmapFactory.decodeByteArray(b, 0, b.size)
                        }
                    }.getOrNull()
                }
                bitmap?.let {
                    Image(
                        bitmap             = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Text(
                text       = content,
                fontSize   = 16.sp,
                color      = if (isUser) Color.White else glass.textPrimary,
                lineHeight = 23.sp,
                style      = TextStyle(fontWeight = FontWeight.Normal)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Prompt chips
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PromptChipRow(chips: List<String>, enabled: Boolean, onTap: (String) -> Unit) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip -> PromptChip(chip, enabled, onTap) }
    }
}

@Composable
private fun PromptChip(text: String, enabled: Boolean, onTap: (String) -> Unit) {
    val glass = LocalGlassTheme.current
    val shape = RoundedCornerShape(CoachTokens.RadiusChip)
    val interSource = remember { MutableInteractionSource() }
    val pressed    by interSource.collectIsPressedAsState()
    val scaleAnim  by animateFloatAsState(
        targetValue  = if (pressed) 0.94f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label        = "chipScale"
    )
    val haptic = LocalHapticFeedback.current

    Box(
        Modifier
            .scale(scaleAnim)
            .clip(shape)
            .glowShadow(CoachTokens.Accent.copy(alpha = 0.10f), radius = 8.dp)
            .background(glass.surface)
            .background(CoachTokens.Accent.copy(alpha = 0.08f))
            .border(
                0.7.dp,
                Brush.linearGradient(
                    listOf(CoachTokens.Accent.copy(0.35f), glass.border)
                ),
                shape
            )
            .clickable(
                interactionSource = interSource,
                indication        = null,
                enabled           = enabled
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onTap(text)
            }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text       = text,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = CoachTokens.Accent
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InputBar(
    value           : String,
    onValueChange   : (String) -> Unit,
    attachedBytes   : ByteArray?,
    sending         : Boolean,
    onPickImage     : () -> Unit,
    onCaptureImage  : () -> Unit,
    onRemoveImage   : () -> Unit,
    onSend          : () -> Unit
) {
    val glass = LocalGlassTheme.current
    val canSend = !sending && (value.trim().isNotEmpty() || attachedBytes != null)
    val capsule = RoundedCornerShape(CoachTokens.RadiusXl)

    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 12.dp)
            .fillMaxWidth()
            .glowShadow(Color.Black.copy(alpha = 0.10f), radius = 24.dp, offsetY = 8.dp)
            .clip(capsule)
            .background(glass.surfaceHover)
            .border(
                1.dp,
                Brush.linearGradient(listOf(glass.borderStrong, glass.border)),
                capsule
            )
            .padding(start = 4.dp, end = 5.dp, top = 4.dp, bottom = 4.dp)
    ) {
        AnimatedVisibility(
            visible = attachedBytes != null,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            attachedBytes?.let { bytes ->
                val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                bitmap?.let {
                    Box(
                        Modifier
                            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp)
                            .size(width = 90.dp, height = 72.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            bitmap             = it.asImageBitmap(),
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.60f))
                                .clickable { onRemoveImage() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove image",
                                tint               = Color.White,
                                modifier           = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MediaIconButton(
                icon    = Icons.Rounded.PhotoLibrary,
                enabled = !sending,
                onClick = onPickImage
            )
            MediaIconButton(
                icon    = Icons.Rounded.CameraAlt,
                enabled = !sending,
                onClick = onCaptureImage
            )

            Box(Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 10.dp)) {
                if (value.isEmpty()) {
                    Text(
                        text     = stringResource(R.string.coach_input_placeholder),
                        fontSize = 16.sp,
                        color    = glass.textPrimary.copy(alpha = 0.42f)
                    )
                }
                BasicTextField(
                    value          = value,
                    onValueChange  = onValueChange,
                    textStyle      = TextStyle(
                        color      = glass.textPrimary,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush    = SolidColor(CoachTokens.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    maxLines       = 5,
                    modifier       = Modifier.fillMaxWidth()
                )
            }

            SendButton(canSend = canSend, onClick = onSend)
        }
    }
}

@Composable
private fun MediaIconButton(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    enabled : Boolean,
    onClick : () -> Unit
) {
    val glass = LocalGlassTheme.current
    val interSource = remember { MutableInteractionSource() }
    val pressed     by interSource.collectIsPressedAsState()
    val scale       by animateFloatAsState(
        targetValue   = if (pressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "mediaBtn"
    )
    IconButton(
        onClick           = onClick,
        enabled           = enabled,
        interactionSource = interSource,
        modifier          = Modifier.size(40.dp).scale(scale)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = glass.textPrimary.copy(alpha = if (enabled) 0.7f else 0.30f),
            modifier           = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SendButton(canSend: Boolean, onClick: () -> Unit) {
    val interSource = remember { MutableInteractionSource() }
    val pressed     by interSource.collectIsPressedAsState()
    val scale       by animateFloatAsState(
        targetValue   = when {
            pressed  -> 0.86f
            canSend  -> 1f
            else     -> 0.92f
        },
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "sendScale"
    )
    val haptic = LocalHapticFeedback.current

    Box(
        Modifier
            .size(36.dp)
            .scale(scale)
            .then(
                if (canSend) Modifier.glowShadow(CoachTokens.Accent.copy(alpha = 0.38f), radius = 12.dp)
                else Modifier
            )
            .clip(CircleShape)
            .background(
                if (canSend) AppColors.CalorieGradient
                else         Brush.linearGradient(listOf(Color.Gray.copy(0.30f), Color.Gray.copy(0.30f)))
            )
            .border(0.6.dp, Color.White.copy(if (canSend) 0.28f else 0.10f), CircleShape)
            .clickable(
                interactionSource = interSource,
                indication        = null,
                enabled           = canSend
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Rounded.ArrowUpward,
            contentDescription = stringResource(R.string.coach_send_a11y),
            tint               = Color.White,
            modifier           = Modifier.size(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reset confirmation dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val glass = LocalGlassTheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon             = {
            Icon(
                Icons.Rounded.DeleteSweep,
                contentDescription = null,
                tint               = Color(0xFFD32F2F),
                modifier           = Modifier.size(28.dp)
            )
        },
        title   = { Text(stringResource(R.string.coach_reset_dialog_title), fontWeight = FontWeight.SemiBold, color = glass.textPrimary) },
        text    = { Text(stringResource(R.string.coach_reset_dialog_message), lineHeight = 20.sp, color = glass.textPrimary.copy(alpha = 0.8f)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text       = stringResource(R.string.coach_reset_confirm),
                    color      = Color(0xFFD32F2F),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = glass.textPrimary.copy(alpha = 0.6f))
            }
        },
        shape             = RoundedCornerShape(CoachTokens.RadiusCard),
        containerColor    = glass.bgBottom, // Fallback for dialog
        modifier          = Modifier.border(1.dp, glass.border, RoundedCornerShape(CoachTokens.RadiusCard))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Image utility
// ─────────────────────────────────────────────────────────────────────────────

private fun resizedJpeg(bytes: ByteArray, maxDimension: Int, quality: Int): ByteArray? {
    val src    = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val longest = maxOf(src.width, src.height)
    val target  = if (longest > maxDimension) {
        val ratio = maxDimension.toFloat() / longest
        Bitmap.createScaledBitmap(
            src,
            (src.width  * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            /* filter= */ true
        )
    } else src

    return ByteArrayOutputStream().use { out ->
        target.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
        out.toByteArray()
    }
}