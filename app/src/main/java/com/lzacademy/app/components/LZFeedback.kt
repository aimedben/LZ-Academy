package com.lzacademy.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lzacademy.app.models.AttendanceStatus
import com.lzacademy.app.models.PaymentStatus
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import kotlinx.coroutines.launch

/** Red counter bubble ("2", "9+"). Renders nothing for counts <= 0. */
@Composable
fun LZNotificationBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val label = if (count > 9) "9+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .background(LZ.colors.error, CircleShape)
            .border(1.5.dp, LZ.colors.surface, CircleShape)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Generic coloured pill, base of every status badge. */
@Composable
fun LZStatusPill(
    text: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dot: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(background, LZShapes.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (dot) Box(Modifier.size(6.dp).background(color, CircleShape))
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        LZText(text, style = LZ.type.labelSmall, color = color, maxLines = 1)
    }
}

@Composable
fun LZAttendanceStatusBadge(status: AttendanceStatus, modifier: Modifier = Modifier) {
    val c = LZ.colors
    val (fg, bg) = when (status) {
        AttendanceStatus.Present -> c.success to c.successSoft
        AttendanceStatus.Absent -> c.error to c.errorSoft
        AttendanceStatus.Late -> c.warning to c.warningSoft
        AttendanceStatus.Justified -> c.info to c.infoSoft
    }
    LZStatusPill(status.label, fg, bg, modifier, dot = true)
}

@Composable
fun LZPaymentStatusBadge(status: PaymentStatus, modifier: Modifier = Modifier) {
    val c = LZ.colors
    val (fg, bg) = when (status) {
        PaymentStatus.Paid -> c.success to c.successSoft
        PaymentStatus.Pending -> c.warning to c.warningSoft
        PaymentStatus.Overdue -> c.error to c.errorSoft
    }
    LZStatusPill(status.label, fg, bg, modifier)
}

/** Title row of a content section with an optional "Voir tout" action. */
@Composable
fun LZSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LZText(title, style = LZ.type.sectionTitle, modifier = Modifier.weight(1f), maxLines = 1)
        if (actionLabel != null) LZLinkButton(actionLabel, onAction)
    }
}

@Composable
fun LZDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LZ.colors.border))
}

@Composable
fun LZEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Inbox,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(72.dp).background(LZ.colors.surfaceSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = LZ.colors.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(4.dp))
        LZText(title, style = LZ.type.sectionTitle, textAlign = TextAlign.Center)
        LZText(message, style = LZ.type.secondary, color = LZ.colors.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(8.dp))
            LZSecondaryButton(actionLabel, onAction, height = 44.dp)
        }
    }
}

/** Shimmering placeholder block. */
@Composable
fun LZSkeleton(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = LZShapes.small) {
    Box(modifier.shimmer(shape))
}

/** Full-screen loading placeholder of a dashboard (gradient header + cards). */
@Composable
fun LZDashboardSkeleton(headerBrush: androidx.compose.ui.graphics.Brush, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(LZ.colors.background)) {
        Box(Modifier.fillMaxWidth().height(210.dp).background(headerBrush, LZShapes.header))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LZSkeleton(Modifier.weight(1f).height(84.dp), LZShapes.standard)
                LZSkeleton(Modifier.weight(1f).height(84.dp), LZShapes.standard)
            }
            LZSkeleton(Modifier.fillMaxWidth().height(150.dp), LZShapes.large)
            LZSkeleton(Modifier.width(180.dp).height(20.dp))
            LZSkeleton(Modifier.fillMaxWidth().height(76.dp), LZShapes.standard)
            LZSkeleton(Modifier.fillMaxWidth().height(76.dp), LZShapes.standard)
        }
    }
}

/** Bottom sheet asking the user to confirm an action. Show it by adding it to the composition. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LZConfirmationBottomSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    dismissLabel: String = "Annuler",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(after: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = LZ.colors.surface,
        shape = LZShapes.sheet,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(64.dp).background(LZ.colors.surfaceSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = LZ.colors.primary, modifier = Modifier.size(30.dp)) }
            LZText(title, style = LZ.type.sectionTitle, textAlign = TextAlign.Center)
            LZText(message, style = LZ.type.body, color = LZ.colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            LZPrimaryButton(confirmLabel, { close(onConfirm) }, Modifier.fillMaxWidth())
            LZOutlinedButton(dismissLabel, { close(onDismiss) }, Modifier.fillMaxWidth())
        }
    }
}

/** Centered confirmation dialog. Show it by adding it to the composition. */
@Composable
fun LZModernDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    dismissLabel: String? = "Annuler",
    footnote: String? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(LZ.colors.surface, LZShapes.large)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(60.dp).background(LZ.colors.surfaceSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = LZ.colors.primary, modifier = Modifier.size(28.dp)) }
            LZText(title, style = LZ.type.sectionTitle, textAlign = TextAlign.Center)
            LZText(message, style = LZ.type.body, color = LZ.colors.textSecondary, textAlign = TextAlign.Center)
            if (footnote != null) {
                LZText(
                    footnote,
                    style = LZ.type.label,
                    color = LZ.colors.warning,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LZ.colors.warningSoft, LZShapes.small)
                        .padding(10.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (dismissLabel != null) {
                    LZOutlinedButton(dismissLabel, onDismiss, Modifier.weight(1f), height = 46.dp)
                }
                LZPrimaryButton(confirmLabel, onConfirm, Modifier.weight(1f), height = 46.dp)
            }
        }
    }
}

/** Toast-style message host (Snackbar) themed for LZ Academy. */
@Composable
fun LZSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = LZShapes.standard,
            containerColor = if (LZ.colors.isDark) LZ.colors.surfaceSoft else LZ.colors.textPrimary,
            contentColor = Color.White,
            actionColor = LZ.colors.turquoise,
        )
    }
}
