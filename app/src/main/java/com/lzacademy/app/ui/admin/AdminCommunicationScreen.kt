package com.lzacademy.app.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZLinkButton
import com.lzacademy.app.components.LZMultilineInput
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTextInput
import com.lzacademy.app.data.preferences.PreparedSms
import com.lzacademy.app.data.preferences.SmsHistoryStore
import com.lzacademy.app.data.sms.SmsComposer
import com.lzacademy.app.data.sms.SmsRecipient
import com.lzacademy.app.data.sms.SmsRecipients
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.AdminReportsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Central de communication de l'administration : sélection MANUELLE des destinataires,
 * puis ouverture de l'app Messages préremplie (aucune permission SMS, aucun envoi
 * automatique). L'historique local ne prétend jamais qu'un SMS a été délivré.
 */
@Composable
fun AdminCommunicationScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminReportsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val historyStore = remember { SmsHistoryStore(context) }
    val recipients by viewModel.recipients.collectAsStateWithLifecycle()

    var body by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var history by remember { mutableStateOf(historyStore.load()) }
    var lotIndex by remember { mutableIntStateOf(0) }

    val levels = remember(recipients) { SmsRecipients.levels(recipients) }
    val filtered = remember(recipients, query, level) { SmsRecipients.filter(recipients, query, level) }

    // Only students with a usable number can actually receive anything, and two students
    // sharing a number (siblings) are ONE recipient, not two.
    val selectedPhones = remember(selected, recipients) {
        recipients
            .filter { it.matricule in selected && it.canReceive }
            .mapNotNull { it.phone }
            .distinct()
    }
    val lots = remember(selectedPhones) { SmsComposer.batches(selectedPhones) }
    LaunchedEffect(selectedPhones, body) { lotIndex = 0 }

    // A re-sync can drop a student (or his number): the stale matricule must leave the
    // selection, otherwise the header claims recipients that cannot be reached.
    val availableMatricules = remember(recipients) { recipients.map { it.matricule }.toSet() }
    LaunchedEffect(availableMatricules) { selected = selected.intersect(availableMatricules) }

    val openMessages = {
        val batch = lots.getOrNull(lotIndex.coerceIn(0, (lots.size - 1).coerceAtLeast(0))).orEmpty()
        when {
            body.isBlank() -> onShowMessage("Écrivez le texte du message.")
            batch.isEmpty() -> onShowMessage("Sélectionnez au moins un élève avec un numéro.")
            else -> {
                val intent = Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse(SmsComposer.smsUri(batch)),
                ).apply {
                    putExtra("sms_body", body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val opened = runCatching { context.startActivity(intent) }.isSuccess
                if (!opened) {
                    onShowMessage("Aucune application SMS n'est installée sur cet appareil.")
                } else {
                    val now = System.currentTimeMillis()
                    historyStore.add(
                        PreparedSms(
                            id = "sms_$now",
                            atMillis = now,
                            recipientCount = batch.size,
                            preview = body.take(80),
                        ),
                    )
                    history = historyStore.load()
                    if (lots.size > 1) lotIndex = (lotIndex + 1) % lots.size
                    onShowMessage("${batch.size} destinataire(s) préremplis — appuyez sur Envoyer dans Messages.")
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("admin_communication_screen"),
    ) {
        LZGradientHeader(brush = LZ.colors.adminHeaderBrush) {
            LZGreetingRow(
                initials = "LZ",
                title = "Centre de Communication",
                subtitle = "SMS aux élèves • Administration",
                onAvatarClick = onAvatarClick,
                photo = rememberAdminLocalPhoto(),
            )
        }

        AdminSmsContent(
            recipients = filtered,
            levels = levels,
            history = history,
            body = body,
            onBodyChange = { body = it },
            query = query,
            onQueryChange = { query = it },
            level = level,
            onLevelChange = { level = it },
            selected = selected,
            onToggle = { matricule ->
                selected = if (matricule in selected) selected - matricule else selected + matricule
            },
            onSelectAll = { select ->
                selected = if (select) {
                    selected + filtered.filter { it.canReceive }.map { it.matricule }
                } else {
                    selected - filtered.map { it.matricule }.toSet()
                }
            },
            onOpenMessages = openMessages,
            totalCount = recipients.size,
            selectedCount = selectedPhones.size,
            batchLabel = if (lots.size > 1) {
                "Lot ${lotIndex + 1}/${lots.size} • ${lots.getOrNull(lotIndex)?.size ?: 0} destinataires"
            } else {
                null
            },
            batchWarning = if (selectedPhones.size > SmsComposer.DEFAULT_BATCH_SIZE) {
                "Au-delà de ${SmsComposer.DEFAULT_BATCH_SIZE} destinataires, certaines app Messages tronquent la liste : envoi par lots."
            } else {
                null
            },
        )
    }
}

/**
 * Stateless part of the screen (no ViewModel, no Android context): everything below is
 * driven by parameters, which is what the Compose test pins.
 */
@Composable
fun AdminSmsContent(
    recipients: List<SmsRecipient>,
    levels: List<String>,
    history: List<PreparedSms>,
    body: String,
    onBodyChange: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    level: String?,
    onLevelChange: (String?) -> Unit,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onOpenMessages: () -> Unit,
    modifier: Modifier = Modifier,
    totalCount: Int = recipients.size,
    selectedCount: Int = selected.size,
    batchLabel: String? = null,
    batchWarning: String? = null,
) {
    val withPhone = recipients.count { it.canReceive }
    val messageParts = ((body.length + 159) / 160).coerceAtLeast(1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_sms_list"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---------------------------------------------------------------- message
        item {
            LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Campaign, null, tint = LZ.colors.primary, modifier = Modifier.size(24.dp))
                    LZText("Nouveau SMS", style = LZ.type.cardTitle)
                }
                Spacer(Modifier.height(12.dp))
                LZMultilineInput(
                    value = body,
                    onValueChange = onBodyChange,
                    placeholder = "Corps du SMS…",
                )
                Spacer(Modifier.height(8.dp))
                LZText(
                    "$selectedCount destinataire(s) • ${body.length} caractères • $messageParts SMS",
                    style = LZ.type.labelSmall,
                    color = LZ.colors.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates) { (label, text) ->
                        LZFilterChip(
                            label = label,
                            selected = body == text,
                            onClick = { onBodyChange(text) },
                        )
                    }
                }
            }
        }

        // ------------------------------------------------------------- recipients
        item {
            LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                LZText("Destinataires", style = LZ.type.cardTitle)
                Spacer(Modifier.height(10.dp))
                LZTextInput(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "Nom, matricule ou téléphone",
                    leadingIcon = Icons.Rounded.Search,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                )
                if (levels.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            LZFilterChip(
                                label = "Tous",
                                selected = level == null,
                                onClick = { onLevelChange(null) },
                            )
                        }
                        items(levels) { name ->
                            LZFilterChip(
                                label = name,
                                selected = level == name,
                                onClick = { onLevelChange(name) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LZText(
                        "$selectedCount sélectionné(s) / $withPhone avec numéro",
                        style = LZ.type.labelSmall,
                        color = LZ.colors.textSecondary,
                    )
                    LZLinkButton(
                        if (selectedCount == 0) "Tout sélectionner" else "Tout désélectionner",
                        onClick = { onSelectAll(selectedCount == 0) },
                    )
                }
            }
        }

        if (recipients.isEmpty()) {
            item {
                LZEmptyState(
                    title = "Aucun élève synchronisé",
                    message = if (totalCount == 0) {
                        "Ouvrez l'app avec une connexion pour récupérer la liste des élèves."
                    } else {
                        "Aucun élève ne correspond à cette recherche ou à ce niveau."
                    },
                    modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                )
            }
        }

        items(recipients, key = { it.matricule }) { recipient ->
            val checked = recipient.matricule in selected
            Row(
                modifier = Modifier
                    .padding(horizontal = LZSize.ScreenPadding)
                    .fillMaxWidth()
                    .background(LZ.colors.surface, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggle(recipient.matricule) },
                    enabled = recipient.canReceive,
                    colors = CheckboxDefaults.colors(
                        checkedColor = LZ.colors.primary,
                        uncheckedColor = LZ.colors.border,
                    ),
                )
                Column(Modifier.weight(1f)) {
                    LZText(
                        recipient.fullName,
                        style = LZ.type.body,
                        color = if (recipient.canReceive) LZ.colors.textPrimary else LZ.colors.textSecondary,
                        maxLines = 1,
                    )
                    LZText(
                        "${recipient.levelName} • ${recipient.phone ?: "sans numéro"}",
                        style = LZ.type.labelSmall,
                        color = LZ.colors.textSecondary,
                        maxLines = 1,
                    )
                }
                LZText(recipient.matricule, style = LZ.type.labelSmall, color = LZ.colors.textSecondary)
            }
        }

        // ---------------------------------------------------------------- sending
        item {
            LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                if (batchWarning != null) {
                    LZStatusPill("Attention", LZ.colors.warning, LZ.colors.warningSoft)
                    Spacer(Modifier.height(8.dp))
                    LZText(batchWarning, style = LZ.type.secondary, color = LZ.colors.textSecondary)
                    Spacer(Modifier.height(10.dp))
                }
                if (batchLabel != null) {
                    LZText(batchLabel, style = LZ.type.labelSmall, color = LZ.colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                }
                LZPrimaryButton(
                    text = "Ouvrir Messages ($selectedCount)",
                    onClick = onOpenMessages,
                    enabled = selectedCount > 0 && body.isNotBlank(),
                    leadingIcon = Icons.Rounded.Send,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LZText(
                    "L'app Messages s'ouvre avec les numéros et le texte déjà remplis. Vous gardez la main sur l'envoi.",
                    style = LZ.type.labelSmall,
                    color = LZ.colors.textSecondary,
                )
            }
        }

        // ---------------------------------------------------------------- history
        item {
            LZSectionHeader(
                title = "SMS préparés sur cet appareil",
                actionLabel = "${history.size} entrée(s)",
                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
            )
        }

        if (history.isEmpty()) {
            item {
                LZText(
                    "Aucun SMS préparé pour l'instant.",
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                )
            }
        }

        items(history, key = { it.id }) { entry ->
            LZCard(
                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        LZText(entry.preview, style = LZ.type.cardTitle, maxLines = 1)
                        LZText(
                            "${entry.recipientCount} destinataire(s) • ${formatPreparedAt(entry.atMillis)}",
                            style = LZ.type.labelSmall,
                            color = LZ.colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    LZStatusPill("Préparé", LZ.colors.warning, LZ.colors.warningSoft)
                }
            }
        }
    }
}

private val templates = listOf(
    "Rappel de cours" to "Rappel : votre cours est maintenu aujourd'hui. Merci d'arriver 10 minutes avant.",
    "Absence" to "Votre absence a été signalée. Merci de contacter l'administration dès que possible.",
    "Paiement" to "Rappel : votre abonnement arrive à échéance. Merci de régulariser auprès de l'accueil.",
)

private val timeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm")

private fun formatPreparedAt(epochMillis: Long): String =
    timeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
