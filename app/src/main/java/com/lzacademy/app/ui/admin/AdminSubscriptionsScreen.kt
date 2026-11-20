package com.lzacademy.app.ui.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZDropdownField
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSearchBar
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTextInput
import com.lzacademy.app.components.LZTopBar
import com.lzacademy.app.data.supabase.RemotePayment
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.repository.SyncResult
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.viewmodel.SyncViewModel
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Administration-only screen: credit a fresh pack of sessions to a student. This is the single
 * renewal entry point of the app — students and teachers can only read the counter, never renew.
 *
 * The list comes ONLY from the real academy database (Supabase), mirrored offline in Room after
 * each sign-in — search all 381 students and renew their pack with a real server write.
 */
@Composable
fun AdminSubscriptionsScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    syncViewModel: SyncViewModel = viewModel(),
) {
    val c = LZ.colors
    val syncedStudents by syncViewModel.students.collectAsStateWithLifecycle()
    val syncedSubs by syncViewModel.subscriptions.collectAsStateWithLifecycle()
    val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()
    val packs by syncViewModel.packs.collectAsStateWithLifecycle()
    val studentPayments by syncViewModel.studentPayments.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var renewTarget by remember { mutableStateOf<SyncedStudentEntity?>(null) }
    var historyTarget by remember { mutableStateOf<SyncedStudentEntity?>(null) }

    LaunchedEffect(Unit) { syncViewModel.loadPacks() }

    androidx.compose.runtime.SideEffect {
        android.util.Log.d(
            "LZSync",
            "composition abonnements: syncedStudents=${syncedStudents.size} vm=${System.identityHashCode(syncViewModel)}",
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        LZTopBar("Abonnements des élèves", onBack = onNavigateBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LZCard(modifier = Modifier.fillMaxWidth()) {
                    LZText("Renouvellement des packs", style = LZ.type.cardTitle)
                    Spacer(Modifier.height(6.dp))
                    LZText(
                        "Seule l'administration crédite les séances. Chaque présence (Présent, Retard) " +
                            "décompte une séance de l'abonnement actuel.",
                        style = LZ.type.secondary,
                        color = c.textSecondary,
                    )
                }
            }

            // ---------- Real academy database (Supabase mirror) ----------
            if (syncedStudents.isNotEmpty()) {
                val subsByMatricule = syncedSubs.associateBy { it.studentMatricule }
                val filtered = syncedStudents.filter {
                    query.isBlank() ||
                        it.fullName.contains(query, ignoreCase = true) ||
                        it.matricule.contains(query, ignoreCase = true)
                }
                item {
                    LZSectionHeader("Base de l'académie • ${syncedStudents.size} élèves")
                    LZCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = when {
                                    syncState.busy -> Icons.Rounded.Autorenew
                                    syncState.lastResult == null || syncState.lastResult == SyncResult.Success -> Icons.Rounded.CloudDone
                                    else -> Icons.Rounded.CloudOff
                                },
                                contentDescription = null,
                                tint = if (syncState.busy) c.textSecondary else c.primary,
                                modifier = Modifier.padding(0.dp),
                            )
                            LZText(
                                when (syncState.lastResult) {
                                    null -> if (syncState.busy) "Synchronisation…" else "Données hors ligne prêtes"
                                    SyncResult.Success -> "Synchronisé avec Supabase"
                                    SyncResult.Offline -> "Hors ligne — données locales"
                                    else -> "Synchronisation impossible (réseau)"
                                },
                                style = LZ.type.secondary,
                                color = c.textSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            com.lzacademy.app.components.LZSecondaryButton(
                                text = "Actualiser",
                                onClick = { syncViewModel.refreshIfPossible(force = true) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LZSearchBar(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Nom ou matricule (LZ-0025)…",
                    )
                    Spacer(Modifier.height(8.dp))
                    if (filtered.isEmpty()) {
                        LZCard(modifier = Modifier.fillMaxWidth()) {
                            LZText("Aucun élève ne correspond à « $query ».", style = LZ.type.secondary, color = c.textSecondary)
                        }
                    }
                }
                // Virtualized: one card per LazyColumn item — composing all 381 at once in a
                // single item took a 2.5 s frame on low-end devices and left the screen blank.
                items(filtered, key = { "sync-${it.matricule}" }) { remote ->
                                val sub = subsByMatricule[remote.matricule]
                                val remaining = sub?.sessionsRemaining ?: 0
                                val total = sub?.sessionsTotal ?: 0
                                LZCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        LZAvatar(initialsOf(remote.fullName), size = 46.dp)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            LZText(remote.fullName, style = LZ.type.cardTitle, maxLines = 1)
                                            LZText(
                                                listOf(remote.matricule, remote.levelName).filterNotNull().joinToString(" • "),
                                                style = LZ.type.label,
                                                color = c.textSecondary,
                                                maxLines = 1,
                                            )
                                        }
                                        when {
                                            sub == null -> LZStatusPill("—", c.textSecondary, c.surfaceSoft)
                                            remaining <= 0 -> LZStatusPill("Épuisé", c.error, c.errorSoft)
                                            else -> LZStatusPill("$remaining/$total", c.primary, c.successSoft)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LZSecondaryButton(
                                            text = "Historique",
                                            onClick = {
                                                historyTarget = remote
                                                syncViewModel.loadPaymentsOf(remote.remoteId)
                                            },
                                            leadingIcon = Icons.Rounded.History,
                                        )
                                        LZPrimaryButton(
                                            text = if (total > 0) "Renouveler (+$total)" else "Renouveler",
                                            onClick = { renewTarget = remote },
                                            modifier = Modifier.weight(1f),
                                            leadingIcon = Icons.Rounded.Autorenew,
                                        )
                                    }
                                }
                }
            }

            item {
                LZSectionHeader("Séances consommées")
                LZCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        val consumedRows = syncedSubs.mapNotNull { sub ->
                            val student = syncedStudents.firstOrNull { it.matricule == sub.studentMatricule }
                            student?.let { it.fullName to (sub.sessionsTotal - sub.sessionsRemaining) }
                        }
                        if (consumedRows.isEmpty()) {
                            LZText("Aucune donnée — aucune séance consommée.", style = LZ.type.secondary, color = c.textSecondary)
                        }
                        consumedRows.forEach { (fullName, consumed) ->
                            LZText(
                                "$fullName : $consumed séance(s) consommée(s)",
                                style = LZ.type.secondary,
                                color = c.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------ dialogs

    renewTarget?.let { student ->
        Dialog(onDismissRequest = { renewTarget = null }) {
            LZCard(modifier = Modifier.fillMaxWidth()) {
                RenewDialog(
                    student = student,
                    packs = packs,
                    onConfirm = { packId, sessionsCount, priceDzd, durationMonths, method ->
                        syncViewModel.renewWithPayment(student.matricule, packId, sessionsCount, priceDzd, durationMonths, method) { error ->
                            if (error == null) {
                                onShowMessage("Abonnement renouvelé et paiement enregistré : ${student.fullName}.")
                            } else {
                                onShowMessage(error)
                            }
                        }
                        // close immediately: the result (or the refresh) arrives via snackbar
                        renewTarget = null
                    },
                    onDismiss = { renewTarget = null },
                )
            }
        }
    }

    historyTarget?.let { student ->
        Dialog(onDismissRequest = { historyTarget = null }) {
            LZCard(modifier = Modifier.fillMaxWidth()) {
                StudentPaymentHistoryDialog(
                    student = student,
                    payments = studentPayments,
                    onDismiss = { historyTarget = null },
                )
            }
        }
    }
}

/**
 * Real paid renewal: choose the pack (Carnet 4 by default), adjust the amount if needed,
 * pick the payment method — the admin records that the student has paid.
 */
@Composable
private fun RenewDialog(
    student: SyncedStudentEntity,
    packs: List<SupabaseApi.RemotePack>,
    onConfirm: (packId: String, sessionsCount: Int, priceDzd: Double, durationMonths: Int?, method: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var packName by remember { mutableStateOf(packs.firstOrNull()?.name.orEmpty()) }
    var amount by remember { mutableStateOf(packs.firstOrNull()?.priceDzd?.let { "%,.0f".format(it).replace(',', ' ') }.orEmpty()) }
    var methodIndex by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf("") }
    val methods = listOf("Espèces" to "cash", "CCP" to "ccp", "BaridiMob" to "baridimob", "Banque" to "bank")

    Column(Modifier.fillMaxWidth()) {
        LZText("Renouveler — ${student.fullName}", style = LZ.type.pageTitle, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        LZText(
            "Nouveau carnet de séances pour la base réelle. Le paiement est enregistré avec la date d'aujourd'hui.",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        LZDropdownField(
            options = packs.map { it.name },
            selected = packName.ifBlank { "Choisir un pack..." },
            onSelected = { chosen ->
                packName = chosen
                packs.firstOrNull { it.name == chosen }?.let {
                    amount = "%,.0f".format(it.priceDzd).replace(',', ' ')
                }
            },
            placeholder = "Pack",
        )
        Spacer(Modifier.height(8.dp))
        LZTextInput(
            value = amount,
            onValueChange = { amount = it },
            placeholder = "Montant payé (DZD) *",
            leadingIcon = Icons.Rounded.Payments,
        )
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = methods.map { it.first },
            selected = methods[methodIndex].first,
            onSelected = { selected -> methodIndex = methods.indexOfFirst { it.first == selected } },
            placeholder = "Méthode de paiement",
        )
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(12.dp))
        LZPrimaryButton(
            text = "Enregistrer le paiement",
            onClick = {
                val pack = packs.firstOrNull { it.name == packName }
                val amountValue = amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                when {
                    pack == null -> error = "Choisissez un pack."
                    amountValue == null || amountValue <= 0.0 -> error = "Saisissez un montant valide."
                    else -> onConfirm(pack.remoteId, pack.sessionsCount, amountValue, pack.durationMonths, methods[methodIndex].second)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}

/** Real payment history of one student (live public.payments rows), newest first. */
@Composable
private fun StudentPaymentHistoryDialog(
    student: SyncedStudentEntity,
    payments: List<RemotePayment>,
    onDismiss: () -> Unit,
) {
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
    Column(Modifier.fillMaxWidth()) {
        LZText("Paiements — ${student.fullName}", style = LZ.type.pageTitle, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        LZText(
            if (payments.isEmpty()) "Aucune donnée — aucun paiement enregistré."
            else "${payments.size} paiement(s) • total ${"%,.0f".format(payments.sumOf { it.amountDzd }).replace(',', ' ')} DZD",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        payments.forEach { payment ->
            val paidDate = payment.paidAtIso?.let { runCatching { OffsetDateTime.parse(it).toLocalDate() }.getOrNull() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    LZText(
                        "${"%,.0f".format(payment.amountDzd).replace(',', ' ')} DZD • ${payment.method}",
                        style = LZ.type.bodyStrong,
                    )
                    LZText(
                        "Payé le ${paidDate?.format(dateFmt) ?: "—"} • statut : ${payment.status}",
                        style = LZ.type.labelSmall,
                        color = LZ.colors.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LZDivider()
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Fermer", onDismiss, Modifier.fillMaxWidth())
    }
}

private fun initialsOf(fullName: String): String =
    fullName.split(" ", "-", "'")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
