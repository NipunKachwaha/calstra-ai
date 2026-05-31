package com.apoorvdarshan.ai.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.R
import com.apoorvdarshan.ai.services.update.AndroidUpdateChecker
import com.apoorvdarshan.ai.services.update.AndroidUpdateState
import com.apoorvdarshan.ai.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.text.font.FontStyle
import com.apoorvdarshan.ai.ui.theme.glowShadow

// ─────────────────────────────────────────────────────────────────────────────
//  Dynamic Glass Theme System
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
//  MVI State
// ─────────────────────────────────────────────────────────────────────────────

@Stable
data class AboutUiState(
    val updateState: AndroidUpdateState = AndroidUpdateState.Idle,
    val currentVersion: String = "",
    val isRefreshing: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class AboutViewModel(appContext: Context) : ViewModel() {

    private val appCtx: Context = appContext.applicationContext

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        val version = AndroidUpdateChecker.currentVersion(appCtx)
        _uiState.update { it.copy(currentVersion = version) }
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateState = AndroidUpdateState.Checking, isRefreshing = true) }
            delay(300)
            val result = AndroidUpdateChecker.check(_uiState.value.currentVersion)
            _uiState.update { it.copy(updateState = result, isRefreshing = false) }
        }
    }

    fun openUrl(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openEmail(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:apoorv@fud-ai.app"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun share(context: Context, text: String, chooserTitle: String) {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                chooserTitle
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openPlayStore(context: Context) {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(AndroidUpdateChecker.PLAY_STORE_MARKET_URL)
        ).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        runCatching { context.startActivity(marketIntent) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(AndroidUpdateChecker.PLAY_STORE_WEB_URL))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Data Model
// ─────────────────────────────────────────────────────────────────────────────

@Stable
data class AboutItem(
    val icon: ImageVector,
    val labelRes: Int,
    val action: () -> Unit,
    val isSpecial: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
//  Root Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AboutScreen(
    container: AppContainer // FIX: Yahan se isDarkTheme parameter hata diya hai kyunki ab hum isko andar read karenge
) {
    val ctx          = LocalContext.current
    val shareText    = stringResource(R.string.about_share_message)
    val shareChooser = stringResource(R.string.about_share_chooser)

    val vm      = remember { AboutViewModel(ctx.applicationContext) }
    val uiState by vm.uiState.collectAsState()

    // 1. App ki Settings se current appearance mode ko dynamically read karein
    val appearance by container.prefs.appearanceMode.collectAsState(initial = "system")
    val systemDark = isSystemInDarkTheme()
    
    // 2. Theme decide karein (User setting vs System default)
    val isDarkTheme = when (appearance) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    // 3. Theme switch hone par turant Glass Palette update hoga
    val glassTheme = remember(isDarkTheme) { if (isDarkTheme) DarkGlass else LightGlass }

    val actionItems = remember {
        listOf(
            AboutItem(icon = Icons.Filled.Star, labelRes = R.string.about_rate, action = { vm.openPlayStore(ctx) }, isSpecial = true),
            AboutItem(icon = Icons.Filled.Share, labelRes = R.string.about_share, action = { vm.share(ctx, shareText, shareChooser) }),
            AboutItem(icon = Icons.Filled.Code, labelRes = R.string.about_open_source, action = { vm.openUrl(ctx, "https://github.com/apoorvdarshan/fud-ai") }),
            AboutItem(icon = Icons.Filled.StarRate, labelRes = R.string.about_star_github, action = { vm.openUrl(ctx, "https://github.com/apoorvdarshan/fud-ai") }),
            AboutItem(icon = Icons.Filled.ThumbUp, labelRes = R.string.about_vote_ph, action = { vm.openUrl(ctx, "https://www.producthunt.com/products/fud-ai-calorie-tracker") }),
            AboutItem(icon = Icons.Filled.Favorite, labelRes = R.string.about_support, action = { vm.openUrl(ctx, "https://ko-fi.com/apoorvdarshan") }, isSpecial = true),
            AboutItem(icon = Icons.Filled.BugReport, labelRes = R.string.about_report_issue, action = { vm.openUrl(ctx, "https://github.com/apoorvdarshan/fud-ai/issues/new?labels=bug&title=Bug:%20") }),
            AboutItem(icon = Icons.Filled.Lightbulb, labelRes = R.string.about_request_feature, action = { vm.openUrl(ctx, "https://github.com/apoorvdarshan/fud-ai/issues/new?labels=enhancement&title=Feature:%20") }),
            AboutItem(icon = Icons.Filled.Email, labelRes = R.string.about_contact, action = { vm.openEmail(ctx) }),
            AboutItem(icon = Icons.Filled.AlternateEmail, labelRes = R.string.about_follow_x, action = { vm.openUrl(ctx, "https://x.com/apoorvdarshan") }),
            AboutItem(icon = Icons.Filled.PhotoCamera, labelRes = R.string.about_follow_instagram, action = { vm.openUrl(ctx, "https://www.instagram.com/fudai.app/") }),
            AboutItem(icon = Icons.Filled.Work, labelRes = R.string.about_follow_linkedin, action = { vm.openUrl(ctx, "https://www.linkedin.com/company/fud-ai-app") })
        )
    }

    val legalItems = remember {
        listOf(
            AboutItem(icon = Icons.Filled.Lock, labelRes = R.string.about_privacy, action = { vm.openUrl(ctx, "https://fud-ai.app/privacy.html") }),
            AboutItem(icon = Icons.Filled.Description, labelRes = R.string.about_terms, action = { vm.openUrl(ctx, "https://fud-ai.app/terms.html") })
        )
    }

    val listState = rememberLazyListState()

    CompositionLocalProvider(LocalGlassTheme provides glassTheme) {
        val glass = LocalGlassTheme.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(glass.bgTop, glass.bgBottom)))
        ) {
            AmbientOrbs(listState = listState)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { GlassHeroHeader() }

                item {
                    AnimatedContent(
                        targetState = uiState.updateState,
                        transitionSpec = {
                            fadeIn(tween(300)) + slideInVertically { -20 } togetherWith fadeOut(tween(200))
                        },
                        label = "update_state"
                    ) { state ->
                        GlassUpdateCard(
                            state          = state,
                            currentVersion = uiState.currentVersion,
                            onRefresh      = { vm.checkForUpdates() },
                            onOpenStore    = { vm.openPlayStore(ctx) }
                        )
                    }
                }

                item { SectionLabel("Community & Support") }

                item {
                    GlassCard {
                        actionItems.forEachIndexed { index, item ->
                            GlassRow(item = item, animDelay = index * 40)
                            if (index < actionItems.lastIndex) GlassDivider()
                        }
                    }
                }

                item { SectionLabel("Legal") }

                item {
                    GlassCard {
                        legalItems.forEachIndexed { index, item ->
                            GlassRow(item = item, animDelay = index * 60)
                            if (index < legalItems.lastIndex) GlassDivider()
                        }
                    }
                }

                item { GlassFooter() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Ambient Orbs
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
//  Hero Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassHeroHeader() {
    val glass = LocalGlassTheme.current
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(600, easing = EaseOutCubic)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha.value)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(AppColors.Calorie.copy(alpha = 0.3f), Color.Transparent))
                    )
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(glass.surface)
                    .border(1.dp, Brush.linearGradient(listOf(glass.borderStrong, Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, null, tint = AppColors.Calorie, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Calstra·AI", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = glass.textPrimary, letterSpacing = (-0.5).sp)
        Text("Calorie Tracker", fontSize = 13.sp, color = glass.textPrimary.copy(alpha = 0.45f), letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Update Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassUpdateCard(
    state: AndroidUpdateState,
    currentVersion: String,
    onRefresh: () -> Unit,
    onOpenStore: () -> Unit
) {
    val glass = LocalGlassTheme.current
    val isAvailable  = state is AndroidUpdateState.Available
    val accentAlpha by animateFloatAsState(if (isAvailable) 1f else 0.6f, tween(400), label = "accent_alpha")

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed        by interactionSource.collectIsPressedAsState()
    val cardScale        by animateFloatAsState(
        targetValue    = if (isPressed) 0.97f else 1f,
        animationSpec  = spring(stiffness = Spring.StiffnessMedium),
        label          = "card_scale"
    )

    Box(
        modifier = Modifier
            .scale(cardScale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isAvailable)
                    Brush.linearGradient(listOf(AppColors.Calorie.copy(0.18f), glass.orbB.copy(0.22f)))
                else
                    Brush.linearGradient(listOf(glass.surface, glass.surface))
            )
            .border(
                1.dp,
                if (isAvailable)
                    Brush.linearGradient(listOf(AppColors.Calorie.copy(0.5f), AppColors.Calorie.copy(0.1f)))
                else
                    Brush.linearGradient(listOf(glass.borderStrong, glass.border)),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = if (isAvailable) onOpenStore else onRefresh
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Calorie.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    AndroidUpdateState.Checking -> CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color       = AppColors.Calorie
                    )
                    else -> Icon(
                        imageVector = when (state) {
                            is AndroidUpdateState.Available -> Icons.Filled.SystemUpdate
                            is AndroidUpdateState.UpToDate  -> Icons.Filled.CheckCircle
                            else                             -> Icons.Filled.Sync
                        },
                        contentDescription = null,
                        tint               = AppColors.Calorie,
                        modifier           = Modifier.size(22.dp)
                    )
                }
                if (isAvailable) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(AppColors.Calorie)
                            .border(1.5.dp, glass.bgTop, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                val label = when (state) {
                    AndroidUpdateState.Checking     -> stringResource(R.string.about_update_checking)
                    is AndroidUpdateState.Available -> stringResource(R.string.about_update_available)
                    is AndroidUpdateState.UpToDate  -> stringResource(R.string.about_app_version)
                    is AndroidUpdateState.Failed    -> stringResource(R.string.about_check_updates)
                    AndroidUpdateState.Idle         -> stringResource(R.string.about_check_updates)
                }
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)

                val subtitle = when (state) {
                    is AndroidUpdateState.Available ->
                        stringResource(R.string.about_update_details_format, state.current, state.latest)
                    is AndroidUpdateState.UpToDate  -> state.current
                    is AndroidUpdateState.Failed    ->
                        stringResource(R.string.about_version_format, state.current)
                    else ->
                        stringResource(R.string.about_version_format, currentVersion)
                }
                Text(subtitle, fontSize = 12.sp, color = glass.textPrimary.copy(alpha = 0.5f))
            }

            if (isAvailable) {
                Text(
                    stringResource(R.string.about_update_action),
                    fontSize    = 13.sp,
                    fontWeight  = FontWeight.Bold,
                    color       = AppColors.Calorie.copy(alpha = accentAlpha)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint     = glass.textPrimary.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section Label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    val glass = LocalGlassTheme.current
    Text(
        text         = text.uppercase(),
        fontSize     = 11.sp,
        fontWeight   = FontWeight.Bold,
        color        = glass.textPrimary.copy(alpha = 0.35f),
        letterSpacing = 1.5.sp,
        modifier     = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Glass Card container
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    val glass = LocalGlassTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(glass.surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)),
                RoundedCornerShape(20.dp)
            )
            .glowShadow(AppColors.Calorie.copy(alpha = 0.04f), radius = 24.dp, offsetY = 8.dp)
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Glass Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassRow(item: AboutItem, animDelay: Int = 0) {
    val glass = LocalGlassTheme.current
    val visible = remember { mutableStateOf(false) }
    val rowAlpha by animateFloatAsState(
        targetValue   = if (visible.value) 1f else 0f,
        animationSpec = tween(350, delayMillis = animDelay, easing = EaseOutCubic),
        label         = "row_alpha"
    )

    LaunchedEffect(Unit) { visible.value = true }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed        by interactionSource.collectIsPressedAsState()
    val bgAlpha          by animateFloatAsState(if (isPressed) 1f else 0f, tween(150), label = "row_bg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clickable(interactionSource = interactionSource, indication = null, onClick = item.action)
            .background(glass.surfaceHover.copy(alpha = bgAlpha))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (item.isSpecial)
                        Brush.linearGradient(listOf(AppColors.Calorie.copy(0.3f), AppColors.Calorie.copy(0.15f)))
                    else
                        Brush.linearGradient(listOf(glass.surface, glass.shimmer))
                )
                .border(
                    0.5.dp,
                    if (item.isSpecial) AppColors.Calorie.copy(0.4f) else glass.border,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = null,
                tint               = if (item.isSpecial) AppColors.Calorie else AppColors.Calorie.copy(alpha = 0.8f),
                modifier           = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text       = stringResource(item.labelRes),
            fontSize   = 15.sp,
            fontWeight = if (item.isSpecial) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (item.isSpecial) glass.textPrimary else glass.textPrimary.copy(alpha = 0.85f),
            modifier   = Modifier.weight(1f)
        )

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint     = glass.textPrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hairline Divider
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassDivider() {
    val glass = LocalGlassTheme.current
    Box(
        Modifier
            .padding(start = 68.dp, end = 18.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(glass.divider)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Footer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassFooter() {
    val glass = LocalGlassTheme.current

    // ── Heartbeat animation ───────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "footer")
    val heartScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.28f,
        animationSpec = infiniteRepeatable(
            tween(750, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "heart_beat"
    )

    // ── Gentle fade-in on first composition ──────────────────────────────
    val footerAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        footerAlpha.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(footerAlpha.value)
            .padding(vertical = 32.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ── Divider line ─────────────────────────────────────────────────
        Box(
            Modifier
                .width(48.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            AppColors.Calorie.copy(alpha = 0.4f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Spacer(Modifier.height(20.dp))

        // ── Italic story line ─────────────────────────────────────────────
        Text(
            text          = stringResource(R.string.about_story_line),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Normal,
            fontStyle     = FontStyle.Italic,
            color         = glass.textPrimary.copy(alpha = 0.35f),
            letterSpacing = 0.3.sp,
            textAlign     = TextAlign.Center,
            lineHeight    = 17.sp,
        )

        Spacer(Modifier.height(16.dp))

        // ── Beating heart ─────────────────────────────────────────────────
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint     = AppColors.Calorie,
            modifier = Modifier
                .size(16.dp)
                .scale(heartScale)
        )

        Spacer(Modifier.height(12.dp))

        // ── "Crafted by" label ────────────────────────────────────────────
        Text(
            text          = stringResource(R.string.about_crafted_by_label),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Normal,
            color         = glass.textPrimary.copy(alpha = 0.30f),
            letterSpacing = 1.8.sp,
            textAlign     = TextAlign.Center,
        )

        Spacer(Modifier.height(5.dp))

        // ── Founder name ──────────────────────────────────────────────────
        Text(
            text          = stringResource(R.string.about_developer_name),
            fontSize      = 18.sp,
            fontWeight    = FontWeight.Bold,
            color         = glass.textPrimary.copy(alpha = 0.88f),
            letterSpacing = 0.4.sp,
            textAlign     = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        // ── Company tag ───────────────────────────────────────────────────
        Text(
            text          = stringResource(R.string.about_company_tag),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Medium,
            color         = AppColors.Calorie.copy(alpha = 0.70f),
            letterSpacing = 2.2.sp,
            textAlign     = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        // ── Bottom divider ────────────────────────────────────────────────
        Box(
            Modifier
                .width(32.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            glass.textPrimary.copy(alpha = 0.15f),
                            Color.Transparent,
                        )
                    )
                )
        )
    }
}

