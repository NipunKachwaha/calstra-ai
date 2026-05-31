package com.apoorvdarshan.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apoorvdarshan.ai.ui.theme.LocalGlassTheme // YAHAN IMPORT ADD KIYA HAI

/**
 * iOS-style segmented control. Two options side-by-side inside a pill;
 * the selected side gets the pink fill and white text.
 */
@Composable
fun UnitToggle(
    leftLabel: String,
    rightLabel: String,
    isLeft: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassTheme.current // GLASS THEME CALL KIYA
    
    // YAHAN BACKGROUND COLOR UPDATE KIYA HAI
    val trackColor = glass.surface 
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(trackColor)
            .padding(4.dp)
    ) {
        Row {
            UnitSegment(
                label = leftLabel,
                selected = isLeft,
                onClick = { if (!isLeft) onSelect(true) },
                modifier = Modifier.weight(1f)
            )
            UnitSegment(
                label = rightLabel,
                selected = !isLeft,
                onClick = { if (isLeft) onSelect(false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UnitSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlassTheme.current // GLASS THEME CALL KIYA

    // YAHAN BACKGROUND AUR TEXT COLORS UPDATE KIYE HAIN
    val bg by animateColorAsState(
        if (selected) glass.surfaceHover
        else Color.Transparent,
        label = "segBg"
    )
    val fg by animateColorAsState(
        if (selected) glass.textPrimary
        else glass.textPrimary.copy(alpha = 0.6f),
        label = "segFg"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}