package com.lzacademy.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes

/** Animated ring. [progress] is 0..1; the arc starts at 12 o'clock. */
@Composable
fun LZCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    color: Color = LZ.colors.primary,
    trackColor: Color = LZ.colors.surfaceSoft,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val entrance by rememberEntranceProgress(progress, 1100)
    val sweep = 360f * progress.coerceIn(0f, 1f) * entrance
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/**
 * Avatar ring split into one arc per session of the pack (4 today): the consumed sessions
 * are filled, the remaining ones are a faint track. The label that always accompanies it
 * carries the real numbers, so the colour is never the only signal.
 */
@Composable
fun LZAvatarProgressRing(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 3.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val segments = ringSegments(remaining, total)
    val sweepPerSegment = if (segments.hasSegments) 360f / segments.total else 360f
    val gap = (sweepPerSegment * 0.18f).coerceAtMost(9f)
    val entrance by rememberEntranceProgress(segments.filled, 600)
    // Colours are read here: the Canvas draw scope is not composable.
    val filledColor = LZ.colors.primary
    val trackColor = LZ.colors.border.copy(alpha = 0.55f)

    Box(modifier.padding(ringWidth + 1.dp), contentAlignment = Alignment.Center) {
        if (segments.hasSegments) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = ringWidth.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                repeat(segments.total) { index ->
                    val filled = index < segments.filled
                    drawArc(
                        color = if (filled) filledColor else trackColor,
                        startAngle = -90f + index * sweepPerSegment + gap / 2f,
                        sweepAngle = (sweepPerSegment - gap) * if (filled) entrance else 1f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        content()
    }
}

/** Large subscription card with a ring ("3 / 4 Séances restantes"). */
@Composable
fun LZCircularProgressCard(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier,
    title: String = "Séances restantes",
    caption: String = "",
    statusLabel: String? = "Actif",
    onDark: Boolean = true,
) {
    val fg = if (onDark) Color.White else LZ.colors.textPrimary
    val sub = if (onDark) Color.White.copy(alpha = 0.8f) else LZ.colors.textSecondary
    LZCard(
        modifier = modifier.fillMaxWidth(),
        shape = LZShapes.large,
        brush = if (onDark) LZ.colors.heroGreenBrush else null,
        borderColor = null,
        elevation = 8.dp,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LZCircularProgress(
                progress = if (total == 0) 0f else remaining / total.toFloat(),
                diameter = 168.dp,
                strokeWidth = 14.dp,
                color = if (onDark) Color.White else LZ.colors.primary,
                trackColor = if (onDark) Color.White.copy(alpha = 0.22f) else LZ.colors.surfaceSoft,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LZText("$remaining / $total", style = LZ.type.stat.copy(fontSize = 34.sp), color = fg)
                    LZText(title, style = LZ.type.label, color = sub)
                }
            }
            if (statusLabel != null) {
                Spacer(Modifier.height(14.dp))
                LZStatusPill(statusLabel, if (onDark) LZ.colors.primaryDark else LZ.colors.success, if (onDark) Color.White else LZ.colors.successSoft)
            }
            if (caption.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LZText(caption, style = LZ.type.secondary, color = sub)
            }
        }
    }
}

/** Animated horizontal bar. */
@Composable
fun LZLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LZ.colors.primary,
    trackColor: Color = LZ.colors.surfaceSoft,
    height: Dp = 8.dp,
) {
    val entrance by rememberEntranceProgress(progress)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            Modifier
                .fillMaxWidth((progress.coerceIn(0f, 1f) * entrance).coerceAtLeast(0.001f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Label + value + bar row, e.g. attendance per group. */
@Composable
fun LZLinearProgressCard(
    title: String,
    valueLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    color: Color = LZ.colors.primary,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LZText(title, style = LZ.type.bodyStrong, maxLines = 1)
                if (subtitle != null) LZText(subtitle, style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
            }
            LZText(valueLabel, style = LZ.type.bodyStrong, color = color, maxLines = 1)
        }
        LZLinearProgress(progress, color = color)
    }
}
