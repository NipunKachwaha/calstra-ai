package com.apoorvdarshan.ai.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apoorvdarshan.ai.ui.settings.LocalGlassTheme
import com.apoorvdarshan.ai.ui.theme.glowShadow
import com.apoorvdarshan.ai.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun FudAIBottomNavBar(
    currentRoute: String?,
    showAboutBadge: Boolean,
    onTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassTheme.current
    val density = LocalDensity.current

    // Auto-Hide State & Timer
    var isNavBarHidden by remember { mutableStateOf(false) }
    var interactionTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(isNavBarHidden, interactionTrigger, currentRoute) {
        if (!isNavBarHidden) {
            delay(5000L) 
            isNavBarHidden = true
        }
    }

    // Hide Animation (Skipping composition by using IntOffset directly)
    val hideOffsetDp by animateDpAsState(
        targetValue = if (isNavBarHidden) 68.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "hideOffsetDp"
    )

    val horizontalPadding = 20.dp
    val cutoutRadiusPx = with(density) { 34.dp.toPx() }
    val cornerRadiusPx = with(density) { 24.dp.toPx() }

    // OPTIMIZATION 1: BoxWithConstraints for absolute responsiveness on all screens
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, hideOffsetDp.roundToPx()) } 
            .clickable(
                enabled = isNavBarHidden,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    isNavBarHidden = false
                    interactionTrigger++
                }
            )
            .navigationBarsPadding()
            .padding(bottom = 24.dp, start = horizontalPadding, end = horizontalPadding)
            .height(96.dp)
    ) {
        // maxWidth is now the exact dynamic width of the available space
        val tabWidth = maxWidth / FudAIRoutes.bottomTabs.size
        val selectedIndex = FudAIRoutes.bottomTabs.indexOf(currentRoute).coerceAtLeast(0)

        // OPTIMIZATION 2: Consolidated to a single, highly-tuned animation state
        val indicatorOffsetDp by animateDpAsState(
            targetValue = (tabWidth * selectedIndex) + (tabWidth / 2),
            // OPTIMIZATION 3: Premium fluid dynamics
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
            label = "indicatorOffsetDp"
        )

        // Calculate Pixel offset dynamically without triggering recompositions
        val indicatorOffsetPx = with(density) { indicatorOffsetDp.toPx() }
        val navBarShape = FluidCutoutShape(indicatorOffsetPx, cutoutRadiusPx, cornerRadiusPx)

        // 1. The Custom Background Layer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(72.dp)
                .border(width = 1.dp, color = glass.textPrimary.copy(alpha = 0.35f), shape = navBarShape),
            shape = navBarShape,
            color = glass.surface,
            shadowElevation = 16.dp
        ) {}

        // 2. The Unselected Items Layer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FudAIRoutes.bottomTabs.forEachIndexed { _, tab ->
                val isSelected = currentRoute == tab
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { 
                            isNavBarHidden = false
                            interactionTrigger++
                            onTap(tab) 
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CustomNavItem(
                        tab = tab,
                        isSelected = isSelected,
                        showBadge = tab == FudAIRoutes.ABOUT && showAboutBadge
                    )
                }
            }
        }

        // 3. The Floating Indicator Layer
        val activeTabData = tabDetails(currentRoute ?: FudAIRoutes.HOME)
        
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = with(density) { (indicatorOffsetDp - 28.dp).toPx() }.toInt(), 
                        y = with(density) { 4.dp.toPx() }.toInt() 
                    )
                }
                .size(56.dp)
                .glowShadow(AppColors.Calorie.copy(alpha = 0.4f), radius = 12.dp, offsetY = 4.dp)
                .clip(CircleShape)
                .background(glass.surface) 
                .border(6.dp, glass.surface, CircleShape) 
                .background(AppColors.Calorie, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = activeTabData.filledIcon,
                contentDescription = activeTabData.label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun CustomNavItem(
    tab: String,
    isSelected: Boolean,
    showBadge: Boolean
) {
    val glass = LocalGlassTheme.current
    val (label, _, outlineIcon) = tabDetails(tab)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        if (!isSelected) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = outlineIcon,
                    contentDescription = label,
                    tint = glass.textPrimary.copy(alpha = 0.75f),
                    modifier = Modifier.size(26.dp)
                )
                
                if (showBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppColors.Calorie)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) glass.textPrimary else glass.textPrimary.copy(alpha = 0.8f)
        )
    }
}

// -----------------------------------------------------------
// The Custom Cutout Shape with Corner Radii
// -----------------------------------------------------------
class FluidCutoutShape(
    private val indicatorOffsetPx: Float,
    private val cutoutRadiusPx: Float,
    private val cornerRadiusPx: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height

            moveTo(0f, cornerRadiusPx)

            arcTo(
                rect = Rect(0f, 0f, cornerRadiusPx * 2, cornerRadiusPx * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            val startCutout = indicatorOffsetPx - cutoutRadiusPx * 1.4f
            val endCutout = indicatorOffsetPx + cutoutRadiusPx * 1.4f
            val controlPointOffset = cutoutRadiusPx * 0.7f
            val dipDepth = cutoutRadiusPx * 1.1f

            lineTo(startCutout, 0f)

            cubicTo(
                startCutout + controlPointOffset, 0f,
                indicatorOffsetPx - cutoutRadiusPx, dipDepth,
                indicatorOffsetPx, dipDepth
            )
            cubicTo(
                indicatorOffsetPx + cutoutRadiusPx, dipDepth,
                endCutout - controlPointOffset, 0f,
                endCutout, 0f
            )

            lineTo(width - cornerRadiusPx, 0f)
            arcTo(
                rect = Rect(width - cornerRadiusPx * 2, 0f, width, cornerRadiusPx * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            lineTo(width, height - cornerRadiusPx)
            arcTo(
                rect = Rect(width - cornerRadiusPx * 2, height - cornerRadiusPx * 2, width, height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            lineTo(cornerRadiusPx, height)
            arcTo(
                rect = Rect(0f, height - cornerRadiusPx * 2, cornerRadiusPx * 2, height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            close()
        }
        return Outline.Generic(path)
    }
}

private data class TabData(val label: String, val filledIcon: ImageVector, val outlineIcon: ImageVector)

private fun tabDetails(tab: String): TabData = when (tab) {
    FudAIRoutes.HOME -> TabData("Home", Icons.Filled.History, Icons.Outlined.History)
    FudAIRoutes.PROGRESS -> TabData("Progress", Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp)
    FudAIRoutes.COACH -> TabData("Coach", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    FudAIRoutes.SETTINGS -> TabData("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    FudAIRoutes.ABOUT -> TabData("About", Icons.Filled.Info, Icons.Outlined.Info)
    else -> TabData("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}