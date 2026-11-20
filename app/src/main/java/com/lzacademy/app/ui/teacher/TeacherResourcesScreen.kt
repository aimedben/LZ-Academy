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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDropdownField
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFileItem
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZFolderItem
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSearchBar
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTextInput
import com.lzacademy.app.models.FileKind
import com.lzacademy.app.models.FileUi
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize

@Composable
fun TeacherResourcesScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFiles = remember {
        listOf(
            FileUi("f1", "Série d'exercices - Nombres complexes & Géométrie.pdf", "PDF • 2.4 Mo • 3AS Math-Tech", FileKind.Pdf),
            FileUi("f2", "Sujet BAC Blanc Session 2025 + Corrigé type.pdf", "PDF • 4.1 Mo • Tous groupes 3AS", FileKind.Pdf),
            FileUi("f3", "Polycopié de cours - Calcul Intégral & Primitives.pdf", "PDF • 3.8 Mo • 3AS Sciences", FileKind.Pdf),
            FileUi("f4", "Présentation Méthodes de Révision BAC 2026.pptx", "Slides • 6.5 Mo • Partagé", FileKind.Slides),
            FileUi("f5", "Devoir maison N°4 - Probabilités & Arbres pondérés.docx", "Doc • 1.2 Mo • 3AS Sciences", FileKind.Doc),
        )
    }

    val files = remember { mutableStateListOf<FileUi>().apply { addAll(initialFiles) } }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("Tous") }
    var showAddFileDialog by remember { mutableStateOf(false) }
    var selectedFileForActions by remember { mutableStateOf<FileUi?>(null) }

    // Add file state
    var newFileName by remember { mutableStateOf("") }
    var newFileGroup by remember { mutableStateOf("3AS Sciences") }

    val filters = listOf("Tous", "PDF", "Devoirs", "BAC Blanc")

    val filteredFiles = remember(files.toList(), searchQuery, selectedFilter) {
        files.filter { file ->
            val matchesSearch = file.name.contains(searchQuery, ignoreCase = true) || file.meta.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "PDF" -> file.kind == FileKind.Pdf
                "Devoirs" -> file.name.contains("Devoir", ignoreCase = true) || file.name.contains("exercices", ignoreCase = true)
                "BAC Blanc" -> file.name.contains("BAC", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("teacher_resources_screen"),
    ) {
        // Teacher gradient header
        LZGradientHeader(brush = LZ.colors.teacherHeaderBrush) {
            LZGreetingRow(
                initials = "KL",
                title = "Ressources & Documents",
                subtitle = "${files.size} fichiers partagés • Math",
                onAvatarClick = onAvatarClick,
                trailing = {
                    IconButton(
                        onClick = { showAddFileDialog = true },
                        modifier = Modifier.testTag("add_file_button"),
                    ) {
                        Icon(Icons.Rounded.Add, "Ajouter un fichier", tint = Color.White)
                    }
                },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Search Bar
            item {
                Box(Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 16.dp)) {
                    LZSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Rechercher un document, polycopié...",
                    )
                }
            }

            // Quick Folder categories
            item {
                Column {
                    LZSectionHeader(
                        title = "Dossiers de cours",
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            LZFolderItem("Annales BAC 2020-2025", "14 documents", onClick = {
                                selectedFilter = "BAC Blanc"
                            })
                        }
                        item {
                            LZFolderItem("Séries & Devoirs maison", "8 séries", onClick = {
                                selectedFilter = "Devoirs"
                            })
                        }
                        item {
                            LZFolderItem("Polycopiés de cours", "6 chapitres", onClick = {
                                selectedFilter = "PDF"
                            })
                        }
                    }
                }
            }

            // Filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filters) { filter ->
                        LZFilterChip(
                            label = filter,
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                        )
                    }
                }
            }

            // Documents List
            if (filteredFiles.isEmpty()) {
                item {
                    LZEmptyState(
                        title = "Aucun document trouvé",
                        message = "Aucun fichier ne correspond à votre recherche ou filtre.",
                        icon = Icons.Rounded.FolderOpen,
                        actionLabel = "Réinitialiser",
                        onAction = {
                            searchQuery = ""
                            selectedFilter = "Tous"
                        },
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 24.dp),
                    )
                }
            } else {
                item {
                    LZSectionHeader(
                        title = "Documents publiés",
                        actionLabel = "${filteredFiles.size} fichiers",
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                }
                items(filteredFiles) { file ->
                    Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                        LZFileItem(
                            name = file.name,
                            meta = file.meta,
                            kind = file.kind,
                            onClick = { selectedFileForActions = file },
                        )
                    }
                }
            }
        }
    }

    // Add File Dialog
    if (showAddFileDialog) {
        val groups = listOf("3AS Sciences", "3AS Math-Tech", "2AS Sciences", "Tous les élèves")
        Dialog(onDismissRequest = { showAddFileDialog = false }) {
            LZCard(modifier = Modifier.fillMaxWidth()) {
                LZText("Publier un document", style = LZ.type.pageTitle)
                LZText("Rendre le fichier disponible sur l'espace élève", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LZTextInput(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        placeholder = "Titre du document (ex: Corrigé Série 5)",
                        leadingIcon = Icons.Rounded.Description,
                    )
                    LZDropdownField(
                        options = groups,
                        selected = newFileGroup,
                        onSelected = { newFileGroup = it },
                        placeholder = "Groupe destinataire",
                    )
                }
                Spacer(Modifier.height(18.dp))
                LZPrimaryButton(
                    text = "Téléverser & Publier",
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            files.add(
                                0,
                                FileUi(
                                    id = "f_${System.currentTimeMillis()}",
                                    name = "$newFileName.pdf",
                                    meta = "PDF • 2.1 Mo • $newFileGroup",
                                    kind = FileKind.Pdf,
                                ),
                            )
                            newFileName = ""
                            showAddFileDialog = false
                            onShowMessage("Document publié avec succès et partagé avec $newFileGroup !")
                        } else {
                            onShowMessage("Veuillez saisir un titre pour le document")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Rounded.UploadFile,
                )
                Spacer(Modifier.height(8.dp))
                LZSecondaryButton("Annuler", { showAddFileDialog = false }, Modifier.fillMaxWidth())
            }
        }
    }

    // File Action Dialog
    selectedFileForActions?.let { file ->
        Dialog(onDismissRequest = { selectedFileForActions = null }) {
            LZCard(modifier = Modifier.fillMaxWidth()) {
                LZText(file.name, style = LZ.type.cardTitle)
                LZText(file.meta, style = LZ.type.secondary, color = LZ.colors.textSecondary)
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Share, null, tint = LZ.colors.primary, modifier = Modifier.size(20.dp))
                    LZText("Partagé avec les élèves du groupe via l'application.", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                }
                Spacer(Modifier.height(18.dp))
                LZPrimaryButton("Télécharger", {
                    onShowMessage("Téléchargement lancé pour ${file.name}")
                    selectedFileForActions = null
                }, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Download)
                Spacer(Modifier.height(8.dp))
                LZSecondaryButton("Supprimer le document", {
                    files.remove(file)
                    selectedFileForActions = null
                    onShowMessage("Document supprimé de la bibliothèque")
                }, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Delete)
            }
        }
    }
}
