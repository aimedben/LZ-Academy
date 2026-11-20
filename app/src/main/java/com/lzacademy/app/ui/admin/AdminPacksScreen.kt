package com.lzacademy.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZCategoryTabs
import com.lzacademy.app.components.LZDashboardSkeleton
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZFloatingButton
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSearchBar
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTextInput
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.AdminManageViewModel
import com.lzacademy.app.viewmodel.UiState

/**
 * Packs & tarifs — moved out of Gestion (per product decision): opened from « Plus ».
 * Same component system as the Gestion tabs (header, search, cards, FAB, dialogs).
 */
@Composable
fun AdminPacksScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminManageViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actionMessage by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            onShowMessage(it)
            viewModel.consumeMessage()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var packDialog by remember { mutableStateOf<Pair<String, SupabaseApi.RemotePack?>?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background),
    ) {
        LZGradientHeader(brush = LZ.colors.adminHeaderBrush) {
            LZGreetingRow(
                initials = "LZ",
                title = "Packs & tarifs",
                subtitle = "Formules vendues à l'accueil • administre les prix",
                onAvatarClick = onNavigateBack,
                photo = rememberAdminLocalPhoto(),
            )
        }

        when (val s = state) {
            UiState.Loading -> LZDashboardSkeleton(LZ.colors.adminHeaderBrush, Modifier.fillMaxSize())
            is UiState.Error -> LZEmptyState(
                title = "Chargement impossible",
                message = s.message,
                actionLabel = "Réessayer",
                onAction = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            )
            is UiState.Success -> PackContent(
                data = s.data,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onAdd = { packDialog = "add" to null },
                onEdit = { packDialog = "edit" to it },
                onDelete = { deleteTarget = it },
                viewModel = viewModel,
            )
        }
    }

    packDialog?.let { (mode, pack) ->
        LZFormDialog(onDismiss = { packDialog = null }) {
            ManagePackForm(
                mode = mode,
                pack = pack,
                onSave = { name, description, price, sessions, months, active ->
                    if (mode == "add") {
                        viewModel.addPack(name, description, price, sessions, months)
                    } else {
                        viewModel.updatePack(pack!!.remoteId, name, description, price, sessions, months, active)
                    }
                    packDialog = null
                },
                onDismiss = { packDialog = null },
            )
        }
    }

    deleteTarget?.let { key ->
        LZFormDialog(onDismiss = { deleteTarget = null }) {
            LZText("Confirmer la suppression", style = LZ.type.pageTitle)
            Spacer(Modifier.height(8.dp))
            LZText(
                "Supprimer « ${key.substringBeforeLast("|")} » définitivement de la base de l'académie ? Cette action est irréversible.",
                style = LZ.type.secondary,
                color = LZ.colors.textSecondary,
            )
            Spacer(Modifier.height(14.dp))
            LZPrimaryButton(
                text = "Supprimer",
                onClick = {
                    viewModel.deletePack(key.substringAfterLast("|"), key.substringBeforeLast("|"))
                    deleteTarget = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LZSecondaryButton("Annuler", { deleteTarget = null }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PackContent(
    data: AdminManageViewModel.ManageData,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (SupabaseApi.RemotePack) -> Unit,
    onDelete: (String) -> Unit,
    viewModel: AdminManageViewModel,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 16.dp)) {
                LZCategoryTabs(
                    options = listOf("Packs"),
                    selectedIndex = 0,
                    onSelect = {},
                    icons = listOf(Icons.Rounded.Payments),
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                LZSearchBar(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Rechercher dans les packs...",
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                packsSection(data, searchQuery, onAdd, onEdit, onDelete)
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
        LZFloatingButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.packsSection(
    data: AdminManageViewModel.ManageData,
    searchQuery: String,
    onAdd: () -> Unit,
    onEdit: (SupabaseApi.RemotePack) -> Unit,
    onDelete: (String) -> Unit,
) {
    val filtered = data.packs.filter { it.name.contains(searchQuery, ignoreCase = true) }
    item {
        LZSectionHeader(
            title = "Tous les packs",
            actionLabel = "Packs",
            onAction = onAdd,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
        LZText(
            "${data.packs.size} packs • ${data.packs.count { !it.isActive }} inactifs",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 2.dp),
        )
    }
    if (filtered.isEmpty()) {
        item {
            LZEmptyState(
                title = "Aucun pack",
                message = "Aucune donnée — aucun pack tarifaire enregistré dans la base.",
                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 16.dp),
            )
        }
    }
    items(filtered, key = { it.remoteId }) { pack ->
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Payments, null, tint = LZ.colors.primary, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LZText(pack.name, style = LZ.type.cardTitle, maxLines = 2)
                        LZText(
                            buildString {
                                append("${formatDzd(pack.priceDzd)} DZD • ${pack.sessionsCount} séances")
                                pack.durationMonths?.let { append(" • $it mois") }
                            },
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                        )
                        pack.description?.takeIf { it.isNotBlank() }?.let {
                            LZText(it, style = LZ.type.labelSmall, color = LZ.colors.textSecondary, maxLines = 2)
                        }
                    }
                    ManagePill(
                        if (pack.isActive) "Actif" else "Inactif",
                        if (pack.isActive) LZ.colors.success else LZ.colors.textSecondary,
                        if (pack.isActive) LZ.colors.successSoft else LZ.colors.surfaceSoft,
                    )
                    ManageIconButton(Icons.Rounded.Edit, "Modifier", LZ.colors.textSecondary) { onEdit(pack) }
                    ManageIconButton(Icons.Rounded.Delete, "Supprimer", LZ.colors.error) {
                        onDelete(deleteKey(pack.name, pack.remoteId))
                    }
                }
            }
        }
    }
    item {
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZSecondaryButton(text = "Créer un pack", onClick = onAdd, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ManagePackForm(
    mode: String,
    pack: SupabaseApi.RemotePack?,
    onSave: (name: String, description: String, price: Double, sessions: Int, months: Int?, active: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(pack?.name.orEmpty()) }
    var description by remember { mutableStateOf(pack?.description.orEmpty()) }
    var price by remember { mutableStateOf(pack?.priceDzd?.takeIf { it > 0.0 }?.let { formatDzd(it) }.orEmpty()) }
    var sessions by remember { mutableStateOf(pack?.sessionsCount?.takeIf { it > 0 }?.toString().orEmpty()) }
    var months by remember { mutableStateOf(pack?.durationMonths?.toString().orEmpty()) }
    var active by remember { mutableStateOf(pack?.isActive ?: true) }
    var error by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        LZText(if (mode == "add") "Créer un pack" else "Modifier le pack", style = LZ.type.pageTitle)
        Spacer(Modifier.height(12.dp))
        LZTextInput(value = name, onValueChange = { name = it }, placeholder = "Nom du pack *", leadingIcon = Icons.Rounded.Payments)
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = description, onValueChange = { description = it }, placeholder = "Description (contenu, avantages)")
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = price, onValueChange = { price = it }, placeholder = "Prix en DZD *")
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = sessions, onValueChange = { sessions = it }, placeholder = "Nombre de séances *")
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = months, onValueChange = { months = it }, placeholder = "Durée en mois (optionnel)")
        if (mode == "edit") {
            Spacer(Modifier.height(8.dp))
            LZFilterChip(
                label = if (active) "Pack actif" else "Pack inactif",
                selected = active,
                onClick = { active = !active },
            )
        }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(14.dp))
        LZPrimaryButton(
            text = if (mode == "add") "Créer le pack" else "Enregistrer les modifications",
            onClick = {
                val p = price.replace(" ", "").toDoubleOrNull()
                val sc = parseDigits(sessions)
                val mo = months.filter { it.isDigit() }.toIntOrNull()
                when {
                    name.isBlank() -> error = "Le nom du pack est obligatoire."
                    p == null || p <= 0.0 -> error = "Indiquez un prix valide en DZD."
                    sc <= 0 -> error = "Indiquez un nombre de séances valide."
                    else -> onSave(name.trim(), description.trim(), p, sc, mo, active)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}
