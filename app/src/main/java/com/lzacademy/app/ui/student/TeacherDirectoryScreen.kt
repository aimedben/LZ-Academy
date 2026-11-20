package com.lzacademy.app.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZSearchBar
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZTopBar
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.TeacherDirectoryRow
import com.lzacademy.app.ui.previews.PreviewSamples
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.theme.LZStatusBarIcons
import com.lzacademy.app.theme.LZTheme
import com.lzacademy.app.viewmodel.TeachersViewModel

/**
 * "Tous nos enseignants": the complete directory. It always shows every active teacher,
 * including those who opted out of the student-home showcase.
 */
@Composable
fun TeacherDirectoryRoute(
    onNavigateBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onContact: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeachersViewModel = viewModel(),
) {
    val teachers by viewModel.allTeachers.collectAsStateWithLifecycle()
    TeacherDirectoryScreen(
        teachers = teachers,
        onNavigateBack = onNavigateBack,
        onOpenProfile = onOpenProfile,
        onContact = onContact,
        modifier = modifier,
    )
}

private val availabilityFilters = listOf("Tous", "Disponibles", "Hors ligne")

@Composable
fun TeacherDirectoryScreen(
    teachers: List<Teacher>,
    onNavigateBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onContact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSubject by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAvailability by rememberSaveable { mutableIntStateOf(0) }

    val subjects = teachers.flatMap { it.subjects }.distinct().sorted()

    val filtered = teachers.filter { teacher ->
        val matchesQuery = searchQuery.isBlank() ||
            teacher.fullName.contains(searchQuery, ignoreCase = true) ||
            teacher.subjects.any { it.contains(searchQuery, ignoreCase = true) }
        val matchesSubject = selectedSubject == null || teacher.subjects.contains(selectedSubject)
        val matchesAvailability = selectedAvailability == 0 ||
            (selectedAvailability == 1 && teacher.acceptsStudentMessages) ||
            (selectedAvailability == 2 && !teacher.acceptsStudentMessages)
        matchesQuery && matchesSubject && matchesAvailability
    }

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .testTag("teacher_directory_screen"),
    ) {
        LZStatusBarIcons(lightIcons = false)
        LZTopBar(title = "Tous nos enseignants", onBack = onNavigateBack)

        Column(
            Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 6.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LZSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Rechercher par nom ou matière...",
            )
            if (subjects.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        LZFilterChip(
                            label = "Toutes les matières",
                            selected = selectedSubject == null,
                            onClick = { selectedSubject = null },
                        )
                    }
                    items(subjects) { subject ->
                        LZFilterChip(
                            label = subject,
                            selected = selectedSubject == subject,
                            onClick = {
                                selectedSubject = if (selectedSubject == subject) null else subject
                            },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availabilityFilters.forEachIndexed { index, label ->
                    LZFilterChip(
                        label = label,
                        selected = selectedAvailability == index,
                        onClick = { selectedAvailability = index },
                    )
                }
                Box(Modifier.weight(1f))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 2.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LZSectionHeader(
                    title = if (filtered.size == 1) "1 enseignant" else "${filtered.size} enseignants",
                    actionLabel = "LZ Academy",
                    modifier = Modifier
                        .padding(horizontal = LZSize.ScreenPadding)
                        .padding(bottom = 4.dp),
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                        LZCard(Modifier.fillMaxWidth()) {
                            LZEmptyState(
                                icon = Icons.Rounded.SearchOff,
                                title = if (teachers.isEmpty()) "Aucun enseignant" else "Aucun enseignant trouvé",
                                message = if (teachers.isEmpty()) {
                                    "Aucune donnée — l'annuaire se remplit après la synchronisation avec la base de l'académie."
                                } else {
                                    "Modifiez la recherche ou réinitialisez les filtres pour élargir la liste."
                                },
                                actionLabel = if (teachers.isEmpty()) null else "Réinitialiser",
                                onAction = {
                                    searchQuery = ""
                                    selectedSubject = null
                                    selectedAvailability = 0
                                },
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { teacher ->
                    Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                        TeacherDirectoryRow(
                            teacher = teacher,
                            onOpenProfile = { onOpenProfile(teacher.id) },
                            onContact = { onContact(teacher.id) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Annuaire enseignants", showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun TeacherDirectoryPreview() {
    LZTheme(darkTheme = false) {
        TeacherDirectoryScreen(
            teachers = PreviewSamples.teachers,
            onNavigateBack = {},
            onOpenProfile = {},
            onContact = {},
        )
    }
}
