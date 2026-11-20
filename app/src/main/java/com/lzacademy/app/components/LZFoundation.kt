package com.lzacademy.app.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZPalette
import com.lzacademy.app.theme.LZShapes

/** The single text composable of the design system: LZ type style + LZ colour token. */
@Composable
fun LZText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LZ.type.body,
    color: Color = LZ.colors.textPrimary,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        textAlign = textAlign,
        overflow = overflow,
    )
}

/** Concrete colour for a [LessonAccent]. */
@Composable
fun LessonAccent.color(): Color = when (this) {
    LessonAccent.Green -> LZ.colors.primary
    LessonAccent.Blue -> LZ.colors.info
    LessonAccent.Violet -> LZPalette.Violet
    LessonAccent.Orange -> LZ.colors.warning
}

/** Soft tinted background matching a [LessonAccent]. */
@Composable
fun LessonAccent.softColor(): Color = when (this) {
    LessonAccent.Green -> LZ.colors.surfaceSoft
    LessonAccent.Blue -> LZ.colors.infoSoft
    LessonAccent.Violet -> LZ.colors.purpleSoft
    LessonAccent.Orange -> LZ.colors.warningSoft
}

/** Soft, wide shadow used by cards and bars. Disabled with elevation 0. */
@Composable
fun Modifier.softShadow(
    elevation: Dp = 6.dp,
    shape: Shape = LZShapes.standard,
    color: Color = LZ.colors.shadow,
): Modifier =
    if (elevation <= 0.dp) this
    else this.shadow(elevation = elevation, shape = shape, clip = false, ambientColor = color, spotColor = color)

/** Clickable with a subtle press-scale feedback and a ripple clipped to [shape]. */
fun Modifier.pressable(
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, label = "pressScale")
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** Pulls the element up over the previous sibling (used for stat cards overlapping a header). */
fun Modifier.overlapTop(overlap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val o = overlap.roundToPx()
    layout(placeable.width, (placeable.height - o).coerceAtLeast(0)) {
        placeable.placeRelative(0, -o)
    }
}

/** Animated shimmer background for loading skeletons. */
fun Modifier.shimmer(shape: Shape = LZShapes.small): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    val base = LZ.colors.border
    val highlight = LZ.colors.surfaceSoft
    Modifier.background(
        brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(x * 1400f - 400f, 0f),
            end = Offset(x * 1400f, 200f),
        ),
        shape = shape,
    )
}

/**
 * 0f → 1f animation started when [key] changes, used for chart and progress entrances.
 * In Android Studio previews it is immediately 1f so that previews show the final picture.
 */
@Composable
fun rememberEntranceProgress(key: Any?, durationMillis: Int = 900): State<Float> {
    val inspection = LocalInspectionMode.current
    val anim = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(key) {
        if (!inspection) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
        }
    }
    return anim.asState()
}
