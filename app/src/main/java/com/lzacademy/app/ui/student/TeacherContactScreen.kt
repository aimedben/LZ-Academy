package com.lzacademy.app.ui.student

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTopBar
import com.lzacademy.app.components.TeacherAvatar
import com.lzacademy.app.components.TeacherAvailabilityBadge
import com.lzacademy.app.components.softShadow
import com.lzacademy.app.ui.previews.PreviewSamples
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.models.TeacherMessage
import com.lzacademy.app.models.TeacherMessageStatus
import com.lzacademy.app.repository.SendMessageResult
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.theme.LZStatusBarIcons
import com.lzacademy.app.theme.LZTheme
import com.lzacademy.app.viewmodel.TeachersViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Contact flow of the student. Messages are queued **locally** only: this build has no network
 * messaging backend, so the screen never claims a delivery — it says "en attente d'envoi".
 * When the teacher does not accept messages, writing is refused and explained, never faked.
 */
@Composable
fun TeacherContactRoute(
    teacherId: String,
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeachersViewModel = viewModel(),
) {
    val teacher by remember(teacherId) { viewModel.teacher(teacherId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val messages by remember(teacherId) { viewModel.messages(teacherId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    TeacherContactScreen(
        teacher = teacher,
        messages = messages,
        onNavigateBack = onNavigateBack,
        onSend = { body, onSent ->
            viewModel.sendMessage(teacherId, body) { result ->
                onSent(
                    when (result) {
                        SendMessageResult.Sent ->
                            "Message enregistré sur votre appareil, en attente d'envoi."
                        SendMessageResult.DailyLimitReached ->
                            "Limite atteinte : 3 messages maximum par jour et par enseignant."
                        SendMessageResult.Empty ->
                            "Écrivez un message avant d'envoyer."
                        SendMessageResult.Unavailable ->
                            "Envoi impossible : cet enseignant n'accepte pas les messages pour le moment."
                    },
                )
            }
        },
        onShowMessage = onShowMessage,
        modifier = modifier,
    )
}

private val quickTemplates = listOf(
    "Bonjour, j'aimerais des informations sur vos cours.",
    "J'ai une question sur le dernier exercice donné en classe.",
    "Serait-il possible de prendre rendez-vous pour un bilan ?",
)

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM • HH:mm")

@Composable
fun TeacherContactScreen(
    teacher: Teacher?,
    messages: List<TeacherMessage>,
    onNavigateBack: () -> Unit,
    onSend: (String, (String) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    var draft by rememberSaveable { mutableStateOf("") }
    var sending by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .testTag("teacher_contact_screen"),
    ) {
        LZStatusBarIcons(lightIcons = false)
        LZTopBar(
            title = if (teacher == null) "Contacter" else "Contacter ${teacher.fullName}",
            onBack = onNavigateBack,
        )

        when {
            teacher == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.primary)
            }

            else -> {
                val available = teacher.acceptsStudentMessages
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = LZSize.ScreenPadding,
                        end = LZSize.ScreenPadding,
                        top = 14.dp,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { RecipientCard(teacher) }

                    if (!available) {
                        item { UnavailabilityCard() }
                    } else {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                LZText("Votre message", style = LZ.type.cardTitle)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemsIndexed(quickTemplates) { _, template ->
                                        LZFilterChip(
                                            label = shortTemplateLabel(template),
                                            selected = false,
                                            onClick = { draft = template },
                                        )
                                    }
                                }
                                MessageComposer(
                                    value = draft,
                                    onValueChange = { draft = it },
                                    enabled = !sending,
                                )
                                LZText(
                                    "3 messages maximum par jour et par enseignant • supprimés après 7 jours.",
                                    style = LZ.type.labelSmall,
                                    color = LZ.colors.textSecondary,
                                )
                                LZPrimaryButton(
                                    text = if (sending) "Envoi en cours..." else "Envoyer le message",
                                    onClick = {
                                        if (draft.isNotBlank() && !sending) {
                                            sending = true
                                            val body = draft
                                            onSend(body) { feedback ->
                                                sending = false
                                                if (feedback.startsWith("Message")) draft = ""
                                                onShowMessage(feedback)
                                            }
                                        }
                                    },
                                    enabled = draft.isNotBlank() && !sending,
                                    loading = sending,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    if (messages.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                LZText(
                                    "Boîte locale (cet appareil)",
                                    style = LZ.type.sectionTitle,
                                )
                                LZCard(contentPadding = PaddingValues(14.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        messages.forEachIndexed { index, message ->
                                            Column {
                                                LZText(message.body, style = LZ.type.body, maxLines = 3)
                                                Spacer(Modifier.height(6.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Info,
                                                        contentDescription = null,
                                                        tint = c.textSecondary,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    LZText(
                                                        formatTime(message.createdAtMillis),
                                                        style = LZ.type.labelSmall,
                                                        color = c.textSecondary,
                                                        maxLines = 1,
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    LZStatusPill(
                                                        message.status.label,
                                                        c.warning,
                                                        c.warningSoft,
                                                    )
                                                }
                                            }
                                            if (index < messages.lastIndex) LZDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        LocalPolicyNote()
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipientCard(teacher: Teacher) {
    val c = LZ.colors
    Row(
        Modifier
            .fillMaxWidth()
            .softShadow(6.dp, LZShapes.standard, c.primary.copy(alpha = 0.25f))
            .background(c.surface, LZShapes.standard)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TeacherAvatar(teacher, size = 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            LZText(teacher.fullName, style = LZ.type.bodyStrong, maxLines = 1)
            LZText(teacher.subjectLabel, style = LZ.type.secondary, color = c.textSecondary, maxLines = 1)
            TeacherAvailabilityBadge(teacher.acceptsStudentMessages)
        }
        Icon(
            Icons.AutoMirrored.Rounded.Chat,
            contentDescription = null,
            tint = if (teacher.acceptsStudentMessages) c.primary else c.textSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun UnavailabilityCard() {
    val c = LZ.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.errorSoft, LZShapes.standard)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = c.error, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LZText(
                "Cet enseignant n'est actuellement pas disponible pour recevoir des messages.",
                style = LZ.type.bodyStrong,
            )
            LZText(
                "Il reste consultable dans l'annuaire et pourra être contacté dès qu'il réactivera la réception.",
                style = LZ.type.secondary,
                color = c.textSecondary,
            )
            LZPrimaryButton(
                text = "Contact indisponible",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun LocalPolicyNote() {
    val c = LZ.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surfaceSoft, LZShapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = c.primary, modifier = Modifier.size(18.dp))
        LZText(
            "Dans cette version, les messages restent enregistrés sur votre appareil (statut " +
                "« ${TeacherMessageStatus.Pending.label} »). Aucune livraison réseau n'est simulée.",
            style = LZ.type.secondary,
            color = c.textSecondary,
        )
    }
}

@Composable
private fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .background(c.surface, LZShapes.standard)
            .border(
                if (focused) 1.5.dp else 1.dp,
                when {
                    !enabled -> c.border
                    focused -> c.primary
                    else -> c.border
                },
                LZShapes.standard,
            )
            .padding(14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            textStyle = LZ.type.body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.primary),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        LZText(
                            "Écrivez votre message ici...",
                            style = LZ.type.body,
                            color = c.textSecondary,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

private fun shortTemplateLabel(template: String): String = when {
    template.startsWith("Bonjour, j'aimerais des informations") -> "Demande d'informations"
    template.startsWith("J'ai une question") -> "Question sur un cours"
    template.startsWith("Serait-il possible") -> "Demande de rendez-vous"
    else -> template.take(18) + "…"
}

private fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .format(timeFormat)

@Preview(name = "Contact – disponible", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun ContactAvailablePreview() {
    LZTheme(darkTheme = false) {
        TeacherContactScreen(
            teacher = PreviewSamples.teachers.first { it.acceptsStudentMessages },
            messages = listOf(
                TeacherMessage(1, "t-karim", "Bonjour, j'aimerais des informations sur vos cours.", System.currentTimeMillis()),
            ),
            onNavigateBack = {},
            onSend = { _, onSent -> onSent("") },
        )
    }
}

@Preview(name = "Contact – hors ligne", showBackground = true, widthDp = 393, heightDp = 700)
@Composable
private fun ContactOfflinePreview() {
    LZTheme(darkTheme = false) {
        TeacherContactScreen(
            teacher = PreviewSamples.teachers.first { !it.acceptsStudentMessages },
            messages = emptyList(),
            onNavigateBack = {},
            onSend = { _, _ -> },
        )
    }
}
