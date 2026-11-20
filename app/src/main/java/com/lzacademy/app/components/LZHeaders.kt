package com.lzacademy.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZSize

/**
 * Compact gradient header (violet foncé ↔ émeraude) with rounded bottom corners that
 * extends behind the status bar. Text and actions use [LZColors.onHeader].
 * Use [extraBottomPadding] together with `Modifier.overlapTop` on the next element
 * for overlapping cards.
 */
@Composable
fun LZGradientHeader(
    brush: Brush,
    modifier: Modifier = Modifier,
    extraBottomPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LZShapes.header)
            .background(brush)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.06f), radius = size.width * 0.42f, center = Offset(size.width * 0.98f, size.height * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.04f), radius = size.width * 0.30f, center = Offset(size.width * 0.02f, size.height * 0.95f))
            }
            .statusBarsPadding()
            .padding(horizontal = LZSize.ScreenPadding)
            .padding(top = 12.dp, bottom = 20.dp + extraBottomPadding),
        content = content,
    )
}

/** Avatar + greeting + trailing actions (bell, analytics...) used on every dashboard header. */
@Composable
fun LZGreetingRow(
    initials: String,
    title: String,
    subtitle: String,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    photo: Painter? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.pressable(CircleShape, onClick = onAvatarClick)) {
            LZAvatar(initials, size = 54.dp, ring = true, photo = photo)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LZText(title, style = LZ.type.pageTitle, color = LZ.colors.onHeader, maxLines = 1)
            LZText(subtitle, style = LZ.type.secondary, color = LZ.colors.onHeader.copy(alpha = 0.72f), maxLines = 2)
        }
        trailing()
    }
}

/** Translucent round button for headers, adapts to light or dark header surfaces. */
@Composable
fun LZHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    LZIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        container = LZ.colors.onHeader.copy(alpha = 0.10f),
        tint = LZ.colors.onHeader,
        badge = badge,
    )
}

/** Plain top bar for inner screens. */
@Composable
fun LZTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onGradient: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val fg = if (onGradient) Color.White else LZ.colors.textPrimary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            LZIconButton(
                Icons.AutoMirrored.Rounded.ArrowBack, "Retour", onBack,
                container = Color.Transparent, tint = fg,
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        LZText(title, style = LZ.type.pageTitle, color = fg, modifier = Modifier.weight(1f), maxLines = 1)
        actions()
    }
}
