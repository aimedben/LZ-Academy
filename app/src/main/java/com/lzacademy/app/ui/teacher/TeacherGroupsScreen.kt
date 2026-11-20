package com.lzacademy.app.ui.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.rememberRemoteAvatar
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.data.supabase.SyncedGroupEntity
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.SyncViewModel

/**
 * Espace enseignant — liste SIMPLE et réelle :
 *  - uniquement les groupes du professeur connecté (groups.teacher_id = sa fiche enseignant) ;
 *  - sous chaque groupe, les élèves réellement inscrits (enrollments actifs → students).
 *
 * Aucune donnée n'est inventée : tant que la synchronisation n'a rien rapporté, un état vide
 * honnête est affiché. La photo de l'enseignant vient de son profil Supabase.
 */
@Composable
fun TeacherGroupsScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTakeAttendance: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val preferences = remember(context.applicationContext) {
        AppPreferences(context.applicationContext)
    }
    var profileId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { profileId = preferences.ownProfileId }

    val syncViewModel: SyncViewModel = viewModel()
    val teachers by syncViewModel.teachers.collectAsStateWithLifecycle()
    val allGroups by syncViewModel.groups.collectAsStateWithLifecycle()
    val enrollments by syncViewModel.enrollments.collectAsStateWithLifecycle()
    val students by syncViewModel.students.collectAsStateWithLifecycle()
    val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()

    // 1) La fiche enseignant liée au compte connecté (teachers.profile_id = profile du user).
    val teacher = remember(teachers, profileId) {
        teachers.firstOrNull { it.profileId != null && it.profileId == profileId }
    }
    // 2) Ses groupes uniquement.
    val myGroups = remember(allGroups, teacher) {
        allGroups
            .filter { teacher != null && it.teacherId != null && it.teacherId == teacher.remoteId }
            .sortedBy { it.name }
    }
    // 3) Les élèves actifs de chaque groupe.
    val studentsById = remember(students) { students.associateBy { it.remoteId } }
    val rosters = remember(myGroups, enrollments, studentsById) {
        myGroups.associate { group ->
            group.remoteId to enrollments
                .filter { it.groupId == group.remoteId && it.leftOn == null }
                .mapNotNull { studentsById[it.studentId] }
                .distinctBy { it.remoteId }
                .sortedBy { it.fullName }
        }
    }
    val totalStudents = rosters.values.sumOf { it.size }

    val teacherPhoto = rememberRemoteAvatar(
        teacher?.photoUrl
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { SupabaseApi().avatarUrl(it) },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("teacher_groups_screen"),
    ) {
        LZGradientHeader(brush = LZ.colors.teacherHeaderBrush) {
            LZGreetingRow(
                initials = teacherInitials(teacher?.fullName),
                title = teacher?.fullName ?: "Mes groupes",
                subtitle = "Mes groupes & mes élèves",
                onAvatarClick = onAvatarClick,
                photo = teacherPhoto,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Résumé en une ligne — lisible d'un coup d'œil.
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = LZSize.ScreenPadding)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryTile(
                        value = "${myGroups.size}",
                        label = if (myGroups.size > 1) "groupes" else "groupe",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryTile(
                        value = "$totalStudents",
                        label = if (totalStudents > 1) "élèves" else "élève",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (myGroups.isEmpty()) {
                item {
                    if (teachers.isEmpty() && syncState.busy) {
                        LZEmptyState(
                            title = "Chargement…",
                            message = "Récupération de vos groupes.",
                            icon = Icons.Rounded.Groups,
                        )
                    } else {
                        LZEmptyState(
                            title = "Aucun groupe",
                            message = "Aucun groupe ne vous est encore attribué.",
                            icon = Icons.Rounded.Groups,
                        )
                    }
                }
            } else {
                items(myGroups, key = { it.remoteId }) { group ->
                    GroupRosterCard(
                        group = group,
                        students = rosters[group.remoteId].orEmpty(),
                        onStudentClick = { student ->
                            onShowMessage("${student.fullName} • ${student.matricule}")
                        },
                        onTakeAttendance = { onTakeAttendance(group.remoteId) },
                    )
                }
            }
        }
    }
}

/** "KL" for "M. Karim Lebbah" — mêmes initiales que le modèle Teacher. */
private fun teacherInitials(fullName: String?): String = fullName
    ?.replace(Regex("^(M\\.|MM\\.|Mme|M\\.?\\s?|Mme\\s?)"), "")
    ?.trim()
    ?.split(" ")
    ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
    ?.take(2)
    ?.joinToString("")
    ?.takeIf { it.isNotBlank() }
    ?: "LZ"

/** Chiffre clé + libellé, dans une carte compacte. */
@Composable
private fun SummaryTile(value: String, label: String, modifier: Modifier = Modifier) {
    LZCard(modifier = modifier) {
        LZText(value, style = LZ.type.stat, color = LZ.colors.primary)
        LZText(label, style = LZ.type.secondary, color = LZ.colors.textSecondary)
    }
}

/** Un groupe et, dessous, la liste de ses élèves (dépliable pour cacher/montrer). */
@Composable
private fun GroupRosterCard(
    group: SyncedGroupEntity,
    students: List<SyncedStudentEntity>,
    onStudentClick: (SyncedStudentEntity) -> Unit,
    onTakeAttendance: () -> Unit,
) {
    val c = LZ.colors
    var expanded by rememberSaveable(group.remoteId) { mutableStateOf(false) }
    LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(c.surfaceSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = c.primary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LZText(group.name, style = LZ.type.cardTitle, maxLines = 2)
                LZText(
                    listOfNotNull(
                        group.levelName?.takeIf { it.isNotBlank() },
                        "${students.size} ${if (students.size > 1) "élèves" else "élève"}",
                    ).joinToString(" • "),
                    style = LZ.type.secondary,
                    color = c.textSecondary,
                )
            }
            LZStatusPill("${students.size}", c.primary, c.surfaceSoft)
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Cacher les élèves" else "Voir les élèves",
                tint = c.textSecondary,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                LZDivider()
                Spacer(Modifier.height(4.dp))

                if (students.isEmpty()) {
                    LZText(
                        "Aucun élève inscrit dans ce groupe.",
                        style = LZ.type.secondary,
                        color = c.textSecondary,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    students.forEachIndexed { index, student ->
                        if (index > 0) LZDivider()
                        RosterStudentRow(student = student, onClick = { onStudentClick(student) })
                    }
                }

                Spacer(Modifier.height(10.dp))
                LZSecondaryButton(
                    text = "Faire l'appel",
                    onClick = onTakeAttendance,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Rounded.HowToReg,
                    height = 44.dp,
                )
            }
        }
    }
}

/** Une ligne élève : photo (ou initiales), nom, matricule. */
@Composable
private fun RosterStudentRow(student: SyncedStudentEntity, onClick: () -> Unit) {
    val photo = rememberRemoteAvatar(
        student.avatarPath
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { SupabaseApi().avatarUrl(it) },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LZAvatar(
            initials = student.fullName.take(2).uppercase(),
            size = 38.dp,
            photo = photo,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            LZText(student.fullName, style = LZ.type.bodyStrong, maxLines = 1)
            LZText(student.matricule, style = LZ.type.labelSmall, color = LZ.colors.textSecondary)
        }
        if (!student.isActive) {
            LZStatusPill("Inactif", LZ.colors.textSecondary, LZ.colors.surfaceSoft)
        } else {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = LZ.colors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}