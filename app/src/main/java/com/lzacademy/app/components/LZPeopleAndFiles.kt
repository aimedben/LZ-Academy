package com.lzacademy.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lzacademy.app.models.FileKind
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZPalette
import com.lzacademy.app.theme.LZShapes

// ------------------------------------------------------------------ avatars

/**
 * Initials avatar. When [photo] is provided (profile photo of the person), the image is drawn
 * cropped in a circle instead of the initials; the initials remain the offline fallback.
 * Optional white ring for use on gradient headers.
 */
@Composable
fun LZAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    ring: Boolean = false,
    background: Brush = Brush.linearGradient(listOf(LZPalette.Turquoise, LZPalette.PrimaryGreen)),
    textColor: Color = Color.White,
    photo: Painter? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .then(if (ring) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier)
            .padding(if (ring) 2.5.dp else 0.dp)
            .background(background, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                painter = photo,
                contentDescription = initials,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            LZText(
                initials,
                style = TextStyle(
                    fontFamily = LZ.type.cardTitle.fontFamily,
                    fontWeight = LZ.type.cardTitle.fontWeight,
                    fontSize = (size.value * 0.36f).sp,
                ),
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

/** Overlapping avatars with a "+N" bubble. */
@Composable
fun LZAvatarGroup(
    initials: List<String>,
    modifier: Modifier = Modifier,
    max: Int = 4,
    size: Dp = 34.dp,
) {
    val shown = initials.take(max)
    val extra = initials.size - shown.size
    val palette = listOf(
        listOf(LZPalette.Turquoise, LZPalette.PrimaryGreen),
        listOf(LZPalette.Violet, LZPalette.DeepPurple),
        listOf(LZPalette.Gold, LZPalette.Orange),
        listOf(LZPalette.Info, Color(0xFF2B5FC7)),
    )
    val slots = shown.size - 1 + if (extra > 0) 1 else 0
    Box(modifier.width((size.value * (1f + 0.7f * slots.coerceAtLeast(0))).dp).height(size)) {
        shown.forEachIndexed { i, s ->
            LZAvatar(
                initials = s,
                size = size,
                background = Brush.linearGradient(palette[i % palette.size]),
                modifier = Modifier
                    .offset(x = (size.value * 0.7f * i).dp)
                    .border(2.dp, LZ.colors.surface, CircleShape),
            )
        }
        if (extra > 0) {
            Box(
                Modifier
                    .offset(x = (size.value * 0.7f * shown.size).dp)
                    .size(size)
                    .background(LZ.colors.surfaceSoft, CircleShape)
                    .border(2.dp, LZ.colors.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) { LZText("+$extra", style = LZ.type.labelSmall, color = LZ.colors.primary, maxLines = 1) }
        }
    }
}

/** Student row: avatar, name, group and an optional trailing slot (status badge). */
@Composable
fun LZStudentCard(
    name: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    initials: String = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString(""),
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    LZCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LZAvatar(initials, size = 44.dp)
            Column(Modifier.weight(1f)) {
                LZText(name, style = LZ.type.bodyStrong, maxLines = 1)
                LZText(subtitle, style = LZ.type.secondary, color = LZ.colors.textSecondary, maxLines = 1)
            }
            if (trailing != null) trailing()
        }
    }
}

/** Teacher row: avatar, name, subject and counters. */
@Composable
fun LZTeacherCard(
    name: String,
    subject: String,
    groups: Int,
    students: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    LZCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LZAvatar(
                name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString(""),
                size = 48.dp,
                background = Brush.linearGradient(listOf(LZPalette.Turquoise, LZPalette.PrimaryGreen)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LZText(name, style = LZ.type.bodyStrong, maxLines = 1)
                LZText(subject, style = LZ.type.secondary, color = LZ.colors.textSecondary, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LZText("$groups groupes", style = LZ.type.label, color = LZ.colors.primary, maxLines = 1)
                LZText("$students élèves", style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------------ files

@Composable
private fun FileKind.iconAndTint(): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    val c = LZ.colors
    return when (this) {
        FileKind.Pdf -> Triple(Icons.Rounded.PictureAsPdf, c.error, c.errorSoft)
        FileKind.Doc -> Triple(Icons.Rounded.Description, c.info, c.infoSoft)
        FileKind.Slides -> Triple(Icons.Rounded.Slideshow, c.warning, c.warningSoft)
        FileKind.Image -> Triple(Icons.Rounded.Image, LZPalette.Violet, c.purpleSoft)
    }
}

/** Flat file row (wrap several in an [LZCard] separated by [LZDivider]). */
@Composable
fun LZFileItem(
    name: String,
    meta: String,
    kind: FileKind,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    val (icon, tint, soft) = kind.iconAndTint()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(LZShapes.small, onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(42.dp).background(soft, LZShapes.small), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LZText(name, style = LZ.type.bodyStrong, maxLines = 1)
            LZText(meta, style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
        }
        if (onMore != null) {
            LZIconButton(
                Icons.Rounded.MoreVert, "Plus d'options", onMore,
                container = Color.Transparent, tint = LZ.colors.textSecondary, size = 36.dp,
            )
        }
    }
}

/** Flat folder row with a chevron, as in the course library. */
@Composable
fun LZFolderItem(
    name: String,
    meta: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(LZShapes.small, onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Folder, null, tint = LZPalette.Gold, modifier = Modifier.size(40.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LZText(name, style = LZ.type.bodyStrong, maxLines = 1)
            LZText(meta, style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = LZ.colors.textSecondary)
    }
}
