package com.lzacademy.app.ui.student

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZIconButton
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTopBar
import com.lzacademy.app.components.TeacherAvatar
import com.lzacademy.app.ui.previews.PreviewSamples
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.theme.LZStatusBarIcons
import com.lzacademy.app.theme.LZTheme
import com.lzacademy.app.viewmodel.TeachersViewModel

/**
 * Public profile of a teacher, opened from the home carousel or the directory.
 * Keeps the student view strictly read-only: teachers own their settings.
 */
@Composable
fun StudentTeacherProfileRoute(
    teacherId: String,
    onNavigateBack: () -> Unit,
    onContact: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeachersViewModel = viewModel(),
) {
    val teacher by remember(teacherId) { viewModel.teacher(teacherId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val pendingMessages by remember(teacherId) { viewModel.messageCount(teacherId) }
        .collectAsStateWithLifecycle(initialValue = 0)

    StudentTeacherProfileScreen(
        teacher = teacher,
        pendingMessages = pendingMessages,
        onNavigateBack = onNavigateBack,
        onContact = onContact,
        modifier = modifier,
    )
}

@Composable
fun StudentTeacherProfileScreen(
    teacher: Teacher?,
    pendingMessages: Int,
    onNavigateBack: () -> Unit,
    onContact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .testTag("student_teacher_profile_screen"),
    ) {
        LZStatusBarIcons(lightIcons = true)

        when {
            teacher == null -> {
                LZTopBar(title = "Profil enseignant", onBack = onNavigateBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.primary)
                }
            }

            else -> {
                LZGradientHeader(brush = c.teacherHeaderBrush, extraBottomPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LZIconButton(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            onClick = onNavigateBack,
                            container = Color.Transparent,
                            tint = c.onHeader,
                        )
                        Spacer(Modifier.width(4.dp))
                        LZText("Profil enseignant", style = LZ.type.pageTitle, color = c.onHeader, maxLines = 1)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        TeacherAvatar(teacher, size = 84.dp, ring = true)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LZText(teacher.fullName, style = LZ.type.pageTitle, color = c.onHeader, maxLines = 2)
                            LZText(
                                teacher.subjectLabel,
                                style = LZ.type.body,
                                color = c.onHeader.copy(alpha = 0.72f),
                                maxLines = 2,
                            )
                            if (teacher.acceptsStudentMessages) {
                                LZStatusPill("Disponible", c.primary, Color.White)
                            } else {
                                LZStatusPill("Hors ligne", c.textSecondary, Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(teacher.subjects) { subject ->
                            LZStatusPill(subject, c.onHeader, c.onHeader.copy(alpha = 0.10f))
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                            ContactCard(teacher, onContact)
                        }
                    }
                    if (pendingMessages > 0) {
                        item {
                            Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                                LocalOutboxNote(pendingMessages)
                            }
                        }
                    }
                    item {
                        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                            LZCard(Modifier.fillMaxWidth()) {
                                LZText("À propos", style = LZ.type.cardTitle)
                                Spacer(Modifier.height(8.dp))
                                LZText(
                                    teacher.professionalDescription,
                                    style = LZ.type.body,
                                    color = c.textSecondary,
                                )
                                Spacer(Modifier.height(10.dp))
                                LZDivider()
                                Spacer(Modifier.height(10.dp))
                                DetailRow(Icons.Rounded.Schedule, teacher.experienceLabel)
                                Spacer(Modifier.height(6.dp))
                                DetailRow(Icons.Rounded.Group, "${teacher.groups} groupes pris en charge")
                            }
                        }
                    }
                    item {
                        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                            LZCard(Modifier.fillMaxWidth()) {
                                LZText("Matières & niveaux", style = LZ.type.cardTitle)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    teacher.subjects.forEach { subject ->
                                        LZStatusPill(subject, c.primary, c.surfaceSoft)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(
                                        Icons.Rounded.School,
                                        contentDescription = null,
                                        tint = c.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    LZText(
                                        if (teacher.levelsLabel.isBlank()) "Niveaux communiqués par l'académie"
                                        else "Niveaux enseignés : ${teacher.levelsLabel}",
                                        style = LZ.type.secondary,
                                        color = c.textSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCard(teacher: Teacher, onContact: (String) -> Unit) {
    val c = LZ.colors
    val available = teacher.acceptsStudentMessages
    LZCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.Chat,
                contentDescription = null,
                tint = if (available) c.primary else c.textSecondary,
                modifier = Modifier.size(22.dp),
            )
            LZText("Contacter ce professeur", style = LZ.type.cardTitle, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        AnimatedVisibility(visible = !available) {
            Column(Modifier.padding(bottom = 10.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(c.warningSoft, com.lzacademy.app.theme.LZShapes.small)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = c.warning,
                        modifier = Modifier.size(20.dp),
                    )
                    LZText(
                        "Cet enseignant n'est actuellement pas disponible pour recevoir des messages.",
                        style = LZ.type.secondary,
                        color = c.textPrimary,
                    )
                }
            }
        }
        LZPrimaryButton(
            text = if (available) "Écrire à ${teacher.fullName}" else "Contact indisponible",
            onClick = { if (available) onContact(teacher.id) },
            enabled = available,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LocalOutboxNote(count: Int) {
    val c = LZ.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.infoSoft, com.lzacademy.app.theme.LZShapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = c.info, modifier = Modifier.size(20.dp))
        LZText(
            if (count == 1) {
                "1 message enregistré sur cet appareil dans l'attente de l'envoi."
            } else {
                "$count messages enregistrés sur cet appareil dans l'attente de l'envoi."
            },
            style = LZ.type.secondary,
            color = c.textPrimary,
        )
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    val c = LZ.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
        LZText(label, style = LZ.type.body, color = c.textSecondary, maxLines = 2)
    }
}

@Preview(name = "Profil enseignant – disponible", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun ProfileAvailablePreview() {
    LZTheme(darkTheme = false) {
        StudentTeacherProfileScreen(
            teacher = PreviewSamples.teachers.first { it.acceptsStudentMessages },
            pendingMessages = 1,
            onNavigateBack = {},
            onContact = {},
        )
    }
}

@Preview(name = "Profil enseignant – hors ligne", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun ProfileOfflinePreview() {
    LZTheme(darkTheme = false) {
        StudentTeacherProfileScreen(
            teacher = PreviewSamples.teachers.first { !it.acceptsStudentMessages },
            pendingMessages = 0,
            onNavigateBack = {},
            onContact = {},
        )
    }
}
