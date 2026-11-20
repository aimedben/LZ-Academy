package com.lzacademy.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.models.PaymentStatus
import com.lzacademy.app.models.TimelineState
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZSize

// ------------------------------------------------------------------ base card

/** Base surface of the design system: rounded, soft shadow, optional press feedback and gradient. */
@Composable
fun LZCard(
    modifier: Modifier = Modifier,
    shape: Shape = LZShapes.standard,
    containerColor: Color = LZ.colors.surface,
    brush: Brush? = null,
    borderColor: Color? = if (LZ.colors.isDark) LZ.colors.border else null,
    elevation: androidx.compose.ui.unit.Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(LZSize.CardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (onClick != null && pressed) 0.98f else 1f, label = "cardPress")
    val bg = if (brush != null) Modifier.background(brush, shape) else Modifier.background(containerColor, shape)
    val border = if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier
    val click = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    } else Modifier
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .softShadow(elevation, shape)
            .then(bg)
            .then(border)
            .clip(shape)
            .then(click)
            .padding(contentPadding),
        content = content,
    )
}

// ------------------------------------------------------------------ stat cards

/** Icon + big number + label. Used on the student, teacher and admin dashboards. */
@Composable
fun LZStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: LessonAccent = LessonAccent.Green,
    containerColor: Color = LZ.colors.surface,
    onClick: (() -> Unit)? = null,
) {
    val tint = accent.color()
    LZCard(
        modifier = modifier,
        containerColor = containerColor,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(42.dp).background(accent.softColor(), LZShapes.small),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp)) }
            LZText(value, style = LZ.type.stat, maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        LZText(label, style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
    }
}

/** Pastel tile with centered icon, value and label (administration header). */
@Composable
fun LZKpiTile(
    icon: ImageVector,
    value: String,
    label: String,
    container: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = LZShapes.standard
    Column(
        modifier = modifier
            .clip(shape)
            .background(container, shape)
            .then(if (onClick != null) Modifier.pressable(shape, onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        LZText(value, style = LZ.type.stat.copy(fontSize = 21.sp), color = LZ.colors.textPrimary, maxLines = 1)
        LZText(label, style = LZ.type.labelSmall, color = LZ.colors.textPrimary.copy(alpha = 0.7f), maxLines = 1)
    }
}

// ------------------------------------------------------------------ hero & lesson

/** Large gradient card ("Prochain cours", "Prochaine séance"). */
@Composable
fun LZHeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    meta: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    metaIcon: ImageVector = Icons.Rounded.Schedule,
    onClick: (() -> Unit)? = null,
    illustration: @Composable BoxScope.() -> Unit = {},
    action: (@Composable () -> Unit)? = null,
) {
    LZCard(
        modifier = modifier,
        shape = LZShapes.large,
        brush = brush,
        borderColor = null,
        elevation = 8.dp,
        onClick = onClick,
        contentPadding = PaddingValues(20.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            illustration()
            Column(Modifier.fillMaxWidth(0.68f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LZText(eyebrow, style = LZ.type.label, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                LZText(title, style = LZ.type.pageTitle, color = Color.White, maxLines = 2)
                LZText(subtitle, style = LZ.type.secondary, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .background(Color.White.copy(alpha = 0.16f), LZShapes.pill)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(metaIcon, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    LZText(meta, style = LZ.type.label, color = Color.White, maxLines = 1)
                }
                if (action != null) {
                    Spacer(Modifier.height(10.dp))
                    action()
                }
            }
        }
    }
}

/** Lesson row: coloured dot, subject, time range, room and optional person. */
@Composable
fun LZLessonCard(
    title: String,
    time: String,
    location: String,
    accent: Color,
    modifier: Modifier = Modifier,
    person: String? = null,
    done: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    LZCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).background(accent, CircleShape))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LZText(title, style = LZ.type.cardTitle, maxLines = 1)
                LZText(time, style = LZ.type.bodyStrong, color = LZ.colors.textSecondary, maxLines = 1)
                LZText(
                    if (person != null) "$location • $person" else location,
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                    maxLines = 1,
                )
            }
            if (done) {
                Icon(Icons.Rounded.CheckCircle, "Terminé", tint = LZ.colors.success, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** One step of a vertical timeline (teacher "Aujourd'hui"). */
@Composable
fun LZTimelineItem(
    time: String,
    title: String,
    subtitle: String,
    accent: Color,
    state: TimelineState,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = LZ.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.pressable(LZShapes.small, onClick = onClick) else Modifier),
    ) {
        LZText(
            time,
            style = LZ.type.bodyStrong,
            color = if (state == TimelineState.Done) c.textSecondary else c.textPrimary,
            modifier = Modifier.width(48.dp).padding(top = 1.dp),
            maxLines = 1,
        )
        Column(Modifier.fillMaxHeight().width(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .size(if (state == TimelineState.Current) 16.dp else 12.dp)
                    .background(if (state == TimelineState.Done) c.success else accent, CircleShape)
                    .then(
                        if (state == TimelineState.Current) Modifier.border(3.dp, accent.copy(alpha = 0.25f), CircleShape)
                        else Modifier,
                    ),
            )
            if (!isLast) {
                Box(Modifier.width(2.dp).weight(1f).background(c.border))
            }
        }
        Column(
            Modifier.weight(1f).padding(start = 10.dp, bottom = if (isLast) 0.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            LZText(
                title,
                style = LZ.type.cardTitle,
                color = if (state == TimelineState.Done) c.textSecondary else c.textPrimary,
                maxLines = 1,
            )
            LZText(subtitle, style = LZ.type.secondary, color = c.textSecondary, maxLines = 1)
        }
        if (state == TimelineState.Done) {
            Icon(Icons.Rounded.CheckCircle, "Terminé", tint = c.success, modifier = Modifier.size(20.dp))
        }
    }
}

/** Day chip of a week strip ("Lun 3"). */
@Composable
fun LZCalendarDayItem(
    dayLabel: String,
    dayNumber: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasLesson: Boolean = false,
    isToday: Boolean = false,
) {
    val c = LZ.colors
    val bg by animateColorAsState(if (selected) c.primary else c.surface, label = "dayBg")
    val fg by animateColorAsState(if (selected) Color.White else c.textPrimary, label = "dayFg")
    val sub by animateColorAsState(if (selected) Color.White.copy(alpha = 0.85f) else c.textSecondary, label = "daySub")
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .background(bg, shape)
            .border(1.dp, if (selected) c.primary else c.border, shape)
            .pressable(shape, role = androidx.compose.ui.semantics.Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LZText(dayLabel, style = LZ.type.labelSmall, color = if (isToday && !selected) c.primary else sub, maxLines = 1)
        LZText(dayNumber.toString(), style = LZ.type.cardTitle, color = fg, maxLines = 1)
        Box(
            Modifier
                .size(5.dp)
                .background(if (hasLesson) (if (selected) Color.White else c.primary) else Color.Transparent, CircleShape),
        )
        // "Aujourd'hui" marker: thin accent bar, legible on both surfaces.
        Box(
            Modifier
                .width(16.dp)
                .height(3.dp)
                .background(
                    if (!isToday) Color.Transparent else if (selected) Color.White else c.primary,
                    CircleShape,
                ),
        )
    }
}

// ------------------------------------------------------------------ content cards

/** Expandable announcement ("Dernières actualités"). */
@Composable
fun LZAnnouncementCard(
    title: String,
    subtitle: String,
    body: String,
    icon: ImageVector,
    accent: LessonAccent,
    unread: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    LZCard(modifier = modifier.fillMaxWidth(), onClick = onToggle, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(accent.softColor(), LZShapes.small),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent.color(), modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LZText(title, style = LZ.type.bodyStrong, maxLines = if (expanded) 3 else 1)
                LZText(subtitle, style = LZ.type.secondary, color = LZ.colors.textSecondary, maxLines = 1)
            }
            if (unread) Box(Modifier.size(9.dp).background(LZ.colors.error, CircleShape))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Réduire" else "Développer",
                tint = LZ.colors.textSecondary,
                modifier = Modifier.size(22.dp).rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                LZDivider()
                Spacer(Modifier.height(12.dp))
                LZText(body, style = LZ.type.body, color = LZ.colors.textSecondary)
            }
        }
    }
}

/** Notification list row. */
@Composable
fun LZNotificationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: LessonAccent,
    modifier: Modifier = Modifier,
    time: String? = null,
    unread: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    LZCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        containerColor = if (unread) LZ.colors.surface else LZ.colors.background,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(accent.softColor(), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent.color(), modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LZText(title, style = LZ.type.bodyStrong, maxLines = 1)
                LZText(
                    if (time != null) "$subtitle • $time" else subtitle,
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                    maxLines = 1,
                )
            }
            if (unread) Box(Modifier.size(9.dp).background(LZ.colors.primary, CircleShape))
        }
    }
}

/** Payment row with amount and status. */
@Composable
fun LZPaymentCard(
    title: String,
    subtitle: String,
    amount: String,
    status: PaymentStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    LZCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(LZ.colors.surfaceSoft, LZShapes.small),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Receipt, null, tint = LZ.colors.primary, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LZText(title, style = LZ.type.bodyStrong, maxLines = 1)
                LZText(subtitle, style = LZ.type.secondary, color = LZ.colors.textSecondary, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LZText(amount, style = LZ.type.bodyStrong, maxLines = 1)
                LZPaymentStatusBadge(status)
            }
        }
    }
}

/** Course / subject card with progress. */
@Composable
fun LZCourseCard(
    subject: String,
    teacher: String,
    progress: Float,
    icon: ImageVector,
    accent: LessonAccent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    LZCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(52.dp).background(accent.softColor(), LZShapes.standard),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent.color(), modifier = Modifier.size(28.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LZText(subject, style = LZ.type.cardTitle, maxLines = 1)
                LZText(teacher, style = LZ.type.secondary, color = LZ.colors.textSecondary, maxLines = 1)
            }
            LZText("${(progress * 100).toInt()}%", style = LZ.type.bodyStrong, color = accent.color())
        }
        Spacer(Modifier.height(12.dp))
        LZLinearProgress(progress, color = accent.color())
    }
}

/**
 * Compact subscription summary with progress ring. Students never renew from the app: the
 * administration alone credits new sessions, so there is no renewal action on this card and the
 * message only points to the administration.
 */
@Composable
fun LZSubscriptionCard(
    remaining: Int,
    total: Int,
    statusLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val exhausted = remaining <= 0
    val pillColor = if (exhausted) LZ.colors.error else LZ.colors.success
    val pillSoft = if (exhausted) LZ.colors.errorSoft else LZ.colors.successSoft
    LZCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LZCircularProgress(
                progress = if (total == 0) 0f else remaining / total.toFloat(),
                diameter = 82.dp,
                strokeWidth = 9.dp,
                color = if (exhausted) LZ.colors.error else LZ.colors.primary,
            ) {
                LZText("$remaining/$total", style = LZ.type.cardTitle.copy(fontSize = 19.sp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LZText("Mon abonnement", style = LZ.type.cardTitle, maxLines = 1)
                    LZStatusPill(statusLabel, pillColor, pillSoft)
                }
                LZText(
                    if (exhausted) {
                        "Abonnement épuisé • demandez le renouvellement à l'administration"
                    } else {
                        "$remaining séances restantes • ${total - remaining} consommée${if (total - remaining > 1) "s" else ""}"
                    },
                    style = LZ.type.secondary,
                    color = if (exhausted) LZ.colors.error else LZ.colors.textSecondary,
                    maxLines = 2,
                )
            }
        }
    }
}

/** Card with a headline figure, a delta pill and a free content slot (chart, sparkline). */
@Composable
fun LZAnalyticsCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    deltaPositive: Boolean = true,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    LZCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LZText(title, style = LZ.type.secondary, color = LZ.colors.textSecondary, modifier = Modifier.weight(1f), maxLines = 1)
            if (delta != null) {
                LZStatusPill(
                    delta,
                    if (deltaPositive) LZ.colors.success else LZ.colors.error,
                    if (deltaPositive) LZ.colors.successSoft else LZ.colors.errorSoft,
                    icon = if (deltaPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LZText(value, style = LZ.type.stat.copy(fontSize = 28.sp), maxLines = 1)
        if (caption != null) LZText(caption, style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** Titled card hosting a chart. */
@Composable
fun LZChartCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LZCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LZText(title, style = LZ.type.cardTitle, maxLines = 1)
                if (subtitle != null) LZText(subtitle, style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
            }
            if (trailing != null) trailing()
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}
