package com.lzacademy.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lzacademy.app.models.DonutSegmentUi
import com.lzacademy.app.theme.LZ

/**
 * Animated bar chart. Tap a bar to select it (hoist [selectedIndex] to show its value elsewhere).
 */
@Composable
fun LZBarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp,
    selectedIndex: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    barColor: Color = LZ.colors.primary,
    accentColor: Color = LZ.colors.turquoise,
) {
    if (values.isEmpty()) return
    val progress by rememberEntranceProgress(values)
    val maxValue = (values.max()).coerceAtLeast(1f) * 1.1f
    val grid = LZ.colors.border
    val labelEvery = if (labels.size > 8) 2 else 1
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(values, onSelect) {
                    if (onSelect != null) {
                        detectTapGestures { offset ->
                            val slot = size.width / values.size.toFloat()
                            onSelect((offset.x / slot).toInt().coerceIn(0, values.lastIndex))
                        }
                    }
                },
        ) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            for (i in 0..3) {
                val y = this.size.height * i / 3f
                drawLine(grid, Offset(0f, y), Offset(this.size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
            }
            val slot = this.size.width / values.size
            val barW = slot * 0.56f
            values.forEachIndexed { i, v ->
                val h = (v / maxValue) * this.size.height * progress
                val x = slot * i + (slot - barW) / 2f
                val alpha = if (selectedIndex == null || selectedIndex == i) 1f else 0.4f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = alpha), barColor.copy(alpha = alpha)),
                        startY = this.size.height - h,
                        endY = this.size.height,
                    ),
                    topLeft = Offset(x, this.size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW * 0.3f, barW * 0.3f),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            labels.forEachIndexed { i, label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    LZText(
                        if (i % labelEvery == 0) label else "",
                        style = LZ.type.labelSmall,
                        color = if (selectedIndex == i) LZ.colors.primary else LZ.colors.textSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

/** Smooth animated area/line chart. Set [compact] for a label-free sparkline. */
@Composable
fun LZLineChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp,
    lineColor: Color = LZ.colors.primary,
    compact: Boolean = false,
) {
    if (values.size < 2) return
    val progress by rememberEntranceProgress(values)
    val grid = LZ.colors.border
    val marker = LZ.colors.surface
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            val minV = values.min()
            val maxV = values.max()
            val range = (maxV - minV).coerceAtLeast(1f)
            val padTop = 12.dp.toPx()
            val padBottom = 10.dp.toPx()
            val padX = 8.dp.toPx()
            val w = this.size.width - 2 * padX
            val h = this.size.height - padTop - padBottom
            val pts = values.mapIndexed { i, v ->
                Offset(padX + w * i / (values.size - 1), padTop + h * (1f - (v - minV) / range))
            }
            val line = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    val p0 = pts[i - 1]
                    val p1 = pts[i]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(pts.last().x, this@Canvas.size.height)
                lineTo(pts.first().x, this@Canvas.size.height)
                close()
            }
            if (!compact) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                for (i in 0..3) {
                    val y = padTop + h * i / 3f
                    drawLine(grid, Offset(0f, y), Offset(this.size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }
            }
            clipRect(right = this.size.width * progress) {
                drawPath(
                    fill,
                    Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f))),
                )
                drawPath(
                    line,
                    lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            if (progress > 0.97f) {
                val last = pts.last()
                drawCircle(lineColor.copy(alpha = 0.2f), 11.dp.toPx(), last)
                drawCircle(marker, 6.dp.toPx(), last)
                drawCircle(lineColor, 4.dp.toPx(), last)
            }
        }
        if (!compact && labels.isNotEmpty()) {
            val every = if (labels.size > 7) 2 else 1
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEachIndexed { i, label ->
                    if (i % every == 0) LZText(label, style = LZ.type.labelSmall, color = LZ.colors.textSecondary, maxLines = 1)
                }
            }
        }
    }
}

/** Animated donut with legend (percentages are computed from the segment values). */
@Composable
fun LZDonutChart(
    segments: List<DonutSegmentUi>,
    modifier: Modifier = Modifier,
    diameter: Dp = 128.dp,
    strokeWidth: Dp = 22.dp,
) {
    val progress by rememberEntranceProgress(segments)
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Canvas(Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val gap = 3f
            var start = -90f
            segments.forEach { seg ->
                val sweep = seg.value / total * 360f * progress
                drawArc(
                    color = seg.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                start += sweep
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            segments.forEach { seg ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(seg.color, CircleShape))
                    LZText(seg.label, style = LZ.type.secondary, modifier = Modifier.weight(1f), maxLines = 1)
                    LZText("${(seg.value / total * 100).toInt()}%", style = LZ.type.bodyStrong, maxLines = 1)
                }
            }
        }
    }
}
