package com.lzacademy.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZSize

@Composable
private fun ButtonContent(
    text: String,
    color: Color,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    loading: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = color,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        LZText(text, style = LZ.type.button, color = color, maxLines = 1)
        if (trailingIcon != null && !loading) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

/** Main call to action: emerald gradient, 52dp high. */
@Composable
fun LZPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = LZSize.ButtonHeight,
) {
    val colors = LZ.colors
    val shape = LZShapes.standard
    Box(
        modifier = modifier
            .height(height)
            .defaultMinSize(minWidth = 64.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .softShadow(if (enabled) 6.dp else 0.dp, shape, colors.primary.copy(alpha = 0.35f))
            .background(colors.primaryBrush, shape)
            .pressable(shape = shape, enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonContent(text, Color.White, leadingIcon, trailingIcon, loading)
    }
}

/** Soft green button for secondary actions. */
@Composable
fun LZSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = LZSize.ButtonHeight,
) {
    val colors = LZ.colors
    val shape = LZShapes.standard
    Box(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .background(colors.surfaceSoft, shape)
            .pressable(shape = shape, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonContent(text, if (colors.isDark) colors.primary else colors.primaryDark, leadingIcon, trailingIcon, false)
    }
}

/** Outlined button (white surface, emerald outline). */
@Composable
fun LZOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = LZSize.ButtonHeight,
) {
    val colors = LZ.colors
    val shape = LZShapes.standard
    Box(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .background(colors.surface, shape)
            .border(BorderStroke(1.5.dp, colors.primary), shape)
            .pressable(shape = shape, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonContent(text, colors.primary, leadingIcon, trailingIcon, false)
    }
}

/** Round icon button with a 44dp touch target. */
@Composable
fun LZIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = LZ.colors.surfaceSoft,
    tint: Color = LZ.colors.primary,
    size: Dp = LZSize.IconButton,
    badge: Int = 0,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .background(container, CircleShape)
                .pressable(shape = CircleShape, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
        if (badge > 0) {
            LZNotificationBadge(badge, Modifier.align(Alignment.TopEnd).padding(end = 0.dp))
        }
    }
}

/** Floating action button with emerald gradient. */
@Composable
fun LZFloatingActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val colors = LZ.colors
    Box(
        modifier = modifier
            .size(size)
            .softShadow(10.dp, CircleShape, colors.primary.copy(alpha = 0.45f))
            .background(colors.primaryBrush, CircleShape)
            .pressable(shape = CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/** Small text link such as "Voir tout →". */
@Composable
fun LZLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LZ.colors.primary,
    showArrow: Boolean = true,
) {
    Row(
        modifier = modifier
            .pressable(shape = LZShapes.small, onClick = onClick)
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LZText(text, style = LZ.type.label, color = color, maxLines = 1)
        if (showArrow) {
            Spacer(Modifier.width(2.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
