package com.lzacademy.app.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZAttendanceStatusBadge
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZLinearProgress
import com.lzacademy.app.components.LZLinkButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTopBar
import com.lzacademy.app.models.AttendanceOutcome
import com.lzacademy.app.models.AttendanceStatus
import com.lzacademy.app.models.AttendanceUndo
import com.lzacademy.app.models.StudentRosterEntry
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.AttendanceViewModel
import com.lzacademy.app.viewmodel.SyncViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Real roll-call screen. Every action goes through [AttendanceViewModel] → Room: a presence
 * decrements the student pack inside a transaction, twice the same tap is a no-op, and the
 * session is refused when the pack is empty.
 */
@Composable
fun TeacherAttendanceScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    groupId: String = "",
    viewModel: AttendanceViewModel = viewModel(),
) {
    // Roster restricted to the group the teacher tapped (real enrollments).
    val roster by remember(groupId) { viewModel.roster(groupId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    // Group identity for the header (name + level), read from the local mirror.
    val syncViewModel: SyncViewModel = viewModel()
    val groups by syncViewModel.groups.collectAsStateWithLifecycle()
    val group = remember(groups, groupId) { groups.firstOrNull { it.remoteId == groupId } }
    val groupLabel = listOfNotNull(
        group?.levelName?.takeIf { it.isNotBlank() },
        group?.name,
    ).joinToString(" • ").ifBlank { "Groupe" }
    val c = LZ.colors
    val dateLabel = viewModel.today
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
        .replaceFirstChar { it.titlecase(Locale.FRENCH) }
    val marked = roster.count { it.isMarked }
    val progress = if (roster.isEmpty()) 0f else marked.toFloat() / roster.size

    Column(
        modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        LZTopBar("Feuille de présence", onBack = onNavigateBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LZCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .background(c.surfaceSoft, LZShapes.standard),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.HowToReg, null, tint = c.primary, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            LZText("$dateLabel • ${viewModel.sessionLabel}", style = LZ.type.cardTitle)
                            LZText(
                                groupLabel,
                                style = LZ.type.secondary,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        LZStatusPill("$marked/${roster.size}", c.primary, c.successSoft)
                    }
                    Spacer(Modifier.height(14.dp))
                    LZLinearProgress(progress)
                    Spacer(Modifier.height(8.dp))
                    LZText(
                        "Présent et Retard consomment une séance. Absent et absence justifiée ne consomment rien.",
                        style = LZ.type.label,
                        color = c.textSecondary,
                    )
                }
            }

            if (roster.isEmpty()) {
                item {
                    LZCard(modifier = Modifier.fillMaxWidth()) {
                        LZText("Aucun élève dans ce groupe.", style = LZ.type.bodyStrong)
                        LZText(
                            "Les élèves sont chargés depuis la base locale.",
                            style = LZ.type.secondary,
                            color = c.textSecondary,
                        )
                    }
                }
            }

            items(roster, key = { it.student.id }) { entry ->
                RosterCard(
                    entry = entry,
                    onStatus = { status ->
                        viewModel.record(entry.student.id, status) { outcome ->
                            onShowMessage(outcome.message(entry.student.fullName))
                        }
                    },
                    onUndo = {
                        val record = entry.record ?: return@RosterCard
                        viewModel.undo(record.id) { result ->
                            val message = when (result) {
                                is AttendanceUndo.Refunded -> "Présence annulée. Séance remboursée."
                                AttendanceUndo.NotFound -> "Présence introuvable."
                            }
                            onShowMessage(message)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RosterCard(
    entry: StudentRosterEntry,
    onStatus: (AttendanceStatus) -> Unit,
    onUndo: () -> Unit,
) {
    val c = LZ.colors
    val subscription = entry.subscription
    val record = entry.record

    LZCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LZAvatar(entry.student.initials, size = 46.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LZText(entry.student.fullName, style = LZ.type.cardTitle, maxLines = 1)
                LZText(
                    "${subscription.planLabel} • ${subscription.remainingSessions}/${subscription.totalSessions} restantes",
                    style = LZ.type.label,
                    color = c.textSecondary,
                )
            }
            if (record != null) {
                LZAttendanceStatusBadge(record.status)
            } else if (subscription.isExhausted) {
                LZStatusPill("Épuisé", c.error, c.errorSoft)
            } else {
                LZStatusPill("À marquer", c.textSecondary, c.surfaceSoft)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (subscription.isExhausted && record?.consumedSession != true) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.errorSoft, LZShapes.small)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.EventAvailable, null, tint = c.error, modifier = Modifier.size(18.dp))
                LZText(
                    "Abonnement épuisé : aucune présence ne peut être marquée. " +
                        "Le renouvellement est effectué par l'administration.",
                    style = LZ.type.label,
                    color = c.error,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LZFilterChip(
                    label = "Présent",
                    selected = record?.status == AttendanceStatus.Present,
                    onClick = { onStatus(AttendanceStatus.Present) },
                    modifier = Modifier.weight(1f),
                )
                LZFilterChip(
                    label = "Absent",
                    selected = record?.status == AttendanceStatus.Absent,
                    onClick = { onStatus(AttendanceStatus.Absent) },
                    modifier = Modifier.weight(1f),
                )
                LZFilterChip(
                    label = "Justifié",
                    selected = record?.status == AttendanceStatus.Justified,
                    onClick = { onStatus(AttendanceStatus.Justified) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (record != null) {
                LZLinkButton(
                    text = "Annuler cette présence",
                    onClick = onUndo,
                    color = c.error,
                    showArrow = false,
                )
            }
        }
    }
}

private fun AttendanceOutcome.message(studentName: String): String = when (this) {
    is AttendanceOutcome.Recorded ->
        if (record.consumedSession) {
            "$studentName : ${record.status.label} — ${subscription.remainingSessions} séance(s) restante(s)."
        } else {
            "$studentName : ${record.status.label} — aucune séance décomptée."
        }
    AttendanceOutcome.AlreadyRecorded -> "$studentName : présence déjà enregistrée, aucun double décompte."
    AttendanceOutcome.NoSubscription -> "$studentName n'a pas d'abonnement."
    is AttendanceOutcome.Exhausted ->
        "$studentName : abonnement épuisé (${subscription.remainingSessions}/${subscription.totalSessions}). Renouvellement via l'administration."
    AttendanceOutcome.NotFound -> "Élève introuvable."
}
