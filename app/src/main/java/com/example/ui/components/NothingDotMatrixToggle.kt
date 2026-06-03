package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun NothingDotMatrixToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 220),
        label = "thumb_position"
    )
    
    val glowProgress by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 250),
        label = "glow_progress"
    )
    
    val trackBgColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF181818) else Color(0xFF0F0F0F),
        animationSpec = tween(durationMillis = 200),
        label = "track_bg"
    )

    val borderStrokeColor by animateColorAsState(
        targetValue = if (checked) NothingWhite.copy(alpha = 0.5f) else NothingBorder,
        animationSpec = tween(durationMillis = 200),
        label = "border_color"
    )

    Canvas(
        modifier = modifier
            .size(width = 54.dp, height = 28.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        val width = size.width
        val height = size.height
        val trackRadius = height / 2f
        
        // 1. Draw solid rounded track
        drawRoundRect(
            color = trackBgColor,
            size = Size(width, height),
            cornerRadius = CornerRadius(trackRadius, trackRadius)
        )
        
        // 2. Hairline high-contrast border
        drawRoundRect(
            color = borderStrokeColor,
            size = Size(width, height),
            cornerRadius = CornerRadius(trackRadius, trackRadius),
            style = Stroke(width = 1.25.dp.toPx())
        )
        
        // 3. Dot-matrix background dots
        val dotRadius = 1.0.dp.toPx()
        val spacingPx = 4.8.dp.toPx()
        val insetX = 5.dp.toPx()
        val insetY = 4.5.dp.toPx()
        
        val drawWidth = width - (insetX * 2)
        val drawHeight = height - (insetY * 2)
        val cols = (drawWidth / spacingPx).toInt()
        val rows = (drawHeight / spacingPx).toInt()
        
        val startX = insetX + (drawWidth - (cols * spacingPx)) / 2f
        val startY = insetY + (drawHeight - (rows * spacingPx)) / 2f
        
        for (r in 0..rows) {
            val cy = startY + r * spacingPx
            for (c in 0..cols) {
                val cx = startX + c * spacingPx
                
                // Clip mathematically to the rounded track pill shape
                if (isLocationInsidePill(cx, cy, width, height, trackRadius)) {
                    // Custom active animation path glows beautifully
                    val factor = cx / width
                    val isActive = checked && (factor <= thumbPosition || glowProgress > 0.1f)
                    
                    val dotColor = if (isActive) {
                        NothingRed.copy(alpha = 0.3f + (0.7f * glowProgress))
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    }
                    
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
        
        // 4. Dot-matrix themed slider handle
        val thumbRadius = 9.dp.toPx()
        val minThumbX = thumbRadius + 3.5.dp.toPx()
        val maxThumbX = width - thumbRadius - 3.5.dp.toPx()
        val thumbX = minThumbX + (maxThumbX - minThumbX) * thumbPosition
        val thumbY = height / 2f
        
        // Subtle ambient drop core reflection/glow for ON state
        if (checked) {
            drawCircle(
                color = NothingRed.copy(alpha = 0.18f),
                radius = thumbRadius + 2.5.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
        }
        
        // Solid outer circle slider
        drawCircle(
            color = if (checked) NothingWhite else NothingGrey,
            radius = thumbRadius,
            center = Offset(thumbX, thumbY)
        )
        
        // Hollow inner core circle
        drawCircle(
            color = if (checked) NothingBlack else Color(0xFF1C1C1C),
            radius = thumbRadius * 0.45f,
            center = Offset(thumbX, thumbY)
        )
    }
}

private fun isLocationInsidePill(cx: Float, cy: Float, width: Float, height: Float, radius: Float): Boolean {
    if (cx < radius) {
        val dx = radius - cx
        val dy = (height / 2f) - cy
        return (dx * dx + dy * dy) <= (radius * radius) - 2f
    }
    if (cx > width - radius) {
        val dx = cx - (width - radius)
        val dy = (height / 2f) - cy
        return (dx * dx + dy * dy) <= (radius * radius) - 2f
    }
    return cy >= 1.5f && cy <= height - 1.5f
}
