package com.lzacademy.app.ui.student

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.LZAvatarProgressRing
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZHeaderIconButton
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZSubscriptionCard
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.rememberRemoteAvatar
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.models.AttendanceRecord
import com.lzacademy.app.models.AttendanceStatus
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import kotlinx.coroutines.flow.flowOf

/**
 * Stateful entry point: the displayed counter comes from Room, so a presence marked by the
 * teacher is visible here without restarting the app.
 */
/**
 * Photo de l'élève connecté pour les headers des écrans élève (planning, cours,
 * notifications) — lise l'URL persistée par le dashboard, fallback initials si absente.
 */
@Composable
internal fun rememberOwnStudentPhoto(): androidx.compose.ui.graphics.painter.Painter? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val url = remember {
        com.lzacademy.app.data.preferences.AppPreferences(context).ownAvatarUrl
    }
    return rememberRemoteAvatar(url)
}

@Composable
fun StudentProfileRoute(
    studentId: String?,
    fullName: String,
    initials: String,
    email: String,
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    attendanceViewModel: com.lzacademy.app.viewmodel.AttendanceViewModel = viewModel(),
    syncViewModel: com.lzacademy.app.viewmodel.SyncViewModel = viewModel(),
) {
    // Real identity from the mirror (own matricule) + real photo from Supabase storage.
    val context = androidx.compose.ui.platform.LocalContext.current
    val synced by syncViewModel.students.collectAsStateWithLifecycle()
    val ownMatricule = remember { com.lzacademy.app.data.preferences.AppPreferences(context).ownMatricule }
    val ownStudent = synced.firstOrNull { it.matricule == ownMatricule }
    val photo = rememberRemoteAvatar(
        ownStudent?.avatarPath
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { SupabaseApi().avatarUrl(it) },
    )

    val subscriptions by attendanceViewModel.subscriptions.collectAsStateWithLifecycle()
    // The counter ledger is keyed by real matricule (LZ-895) — resolve it from the mirror,
    // the nav arg (local seed id like "s-amine") never matches.
    val ownMatriculeKey = ownStudent?.matricule ?: studentId
    val ledgerSub = ownMatriculeKey?.let { subscriptions[it] }
    // Fall back to the real mirrored subscription (public.subscriptions) when the local
    // ledger hasn't been seeded yet — the numbers stay honest either way.
    val syncedSubs by syncViewModel.subscriptions.collectAsStateWithLifecycle()
    val mirroredSub = syncedSubs.firstOrNull { it.studentMatricule == ownMatriculeKey }
    val remainingSessions = ledgerSub?.remainingSessions ?: mirroredSub?.sessionsRemaining ?: 0
    val totalSessions = ledgerSub?.totalSessions ?: mirroredSub?.sessionsTotal ?: 4
    val statusLabel = ledgerSub?.statusLabel
        ?: mirroredSub?.status?.replaceFirstChar { it.uppercase() }
        ?: "Actif"
    val history = remember(ownMatriculeKey) {
        if (ownMatriculeKey == null) flowOf(emptyList()) else attendanceViewModel.history(ownMatriculeKey)
    }
    val records by history.collectAsStateWithLifecycle(initialValue = emptyList())

    // The student's multiple course enrollments (real public.enrollments → groups → teachers).
    val enrollmentsList by syncViewModel.enrollments.collectAsStateWithLifecycle()
    val groupsList by syncViewModel.groups.collectAsStateWithLifecycle()
    val teachersList by syncViewModel.teachers.collectAsStateWithLifecycle()
    val courses = remember(ownStudent, enrollmentsList, groupsList, teachersList) {
        if (ownStudent == null) {
            emptyList()
        } else {
            enrollmentsList
                .filter { it.studentId == ownStudent.remoteId && it.leftOn == null }
                .mapNotNull { enrollment -> groupsList.firstOrNull { it.remoteId == enrollment.groupId } }
                .map { group ->
                    StudentCourse(
                        name = group.name,
                        teacherName = teachersList.firstOrNull { it.remoteId == group.teacherId }?.fullName,
                        levelName = group.levelName,
                    )
                }
        }
    }

    StudentProfileScreen(
        studentName = fullName,
        initials = initials,
        email = email,
        photo = photo,
        remainingSessions = remainingSessions,
        totalSessions = totalSessions,
        statusLabel = statusLabel,
        attendanceRecords = records,
        levelName = ownStudent?.levelName,
        phone = ownStudent?.phone,
        courses = courses,
        onNavigateBack = onNavigateBack,
        onShowMessage = onShowMessage,
        onAvatarClick = onAvatarClick,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

/** One course the student is enrolled in, flattened from the Room mirror. */
data class StudentCourse(
    val name: String,
    val teacherName: String?,
    val levelName: String?,
)

@Composable
fun StudentProfileScreen(
    studentName: String,
    initials: String,
    email: String,
    remainingSessions: Int,
    totalSessions: Int,
    statusLabel: String,
    attendanceRecords: List<AttendanceRecord> = emptyList(),
    levelName: String? = null,
    phone: String? = null,
    courses: List<StudentCourse> = emptyList(),
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    photo: androidx.compose.ui.graphics.painter.Painter? = null,
) {
    // Real assiduity, aggregated from the Room attendance table (teacher roll-calls).
    val assiduity = remember(attendanceRecords) {
        val present = attendanceRecords.count { it.status == AttendanceStatus.Present }
        val late = attendanceRecords.count { it.status == AttendanceStatus.Late }
        val absent = attendanceRecords.count { it.status == AttendanceStatus.Absent }
        val justified = attendanceRecords.count { it.status == AttendanceStatus.Justified }
        AssiduityStats(
            attended = present + late,
            lates = late,
            absent = absent,
            justified = justified,
            total = attendanceRecords.size,
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("student_profile_screen"),
    ) {
        // Gradient Header
        LZGradientHeader(brush = LZ.colors.studentHeaderBrush) {
            LZGreetingRow(
                initials = initials,
                title = "Mon profil",
                subtitle = "Espace élève",
                onAvatarClick = onAvatarClick,
                photo = photo,
            ) {
                LZHeaderIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Paramètres",
                    onClick = onOpenSettings,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Identity — light hero card, photo-ready avatar (photos arrive with the Supabase database)
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LZAvatarProgressRing(
                            remaining = remainingSessions,
                            total = totalSessions,
                        ) {
                            LZAvatar(
                                initials = studentName.take(2).uppercase(),
                                size = 84.dp,
                                photo = photo,
                            )
                        }
                        if (totalSessions > 0) {
                            LZText(
                                "$remainingSessions/$totalSessions séances",
                                style = LZ.type.label,
                                color = LZ.colors.textSecondary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LZText(studentName, style = LZ.type.cardTitle)
                                Icon(Icons.Rounded.Verified, "Vérifié", tint = LZ.colors.primary, modifier = Modifier.size(16.dp))
                            }
                            LZText(email, style = LZ.type.secondary, color = LZ.colors.textSecondary)
                        }
                        LZStatusPill("Inscription Active", LZ.colors.success, LZ.colors.successSoft)
                        Spacer(Modifier.height(4.dp))
                        LZDivider()
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.School, null, tint = LZ.colors.textSecondary, modifier = Modifier.size(14.dp))
                                LZText(levelName ?: "Niveau non renseigné", style = LZ.type.label, color = LZ.colors.textSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Phone, null, tint = LZ.colors.textSecondary, modifier = Modifier.size(14.dp))
                                LZText(phone ?: "Téléphone non renseigné", style = LZ.type.label, color = LZ.colors.textSecondary)
                            }
                        }
                    }
                }
            }

            // Subscription card with progress ring — read-only, renewal is admin-only
            item {
                Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                    Column {
                        LZSubscriptionCard(
                            remaining = remainingSessions,
                            total = totalSessions,
                            statusLabel = statusLabel,
                            onClick = {
                                onShowMessage(
                                    "$remainingSessions/$totalSessions séances • $statusLabel • le renouvellement est effectué par l'administration",
                                )
                            },
                        )
                    }
                }
            }

            // Mes cours — the student can be enrolled in several courses at once
            item {
                LZCard(
                    modifier = Modifier
                        .padding(horizontal = LZSize.ScreenPadding)
                        .fillMaxWidth()
                        .testTag("student_courses_card"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LZText("Mes cours", style = LZ.type.cardTitle)
                        LZStatusPill(
                            text = if (courses.isEmpty()) "Aucun cours" else "${courses.size} cours",
                            color = if (courses.isEmpty()) LZ.colors.textSecondary else LZ.colors.primary,
                            background = LZ.colors.surfaceSoft,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (courses.isEmpty()) {
                        LZText(
                            "Aucune donnée — vous n'êtes inscrit à aucun cours pour l'instant.",
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                        )
                    }
                    courses.forEach { course ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Groups, null,
                                tint = LZ.colors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                LZText(course.name, style = LZ.type.bodyStrong)
                                LZText(
                                    "${course.teacherName ?: "Enseignant —"} • ${course.levelName ?: "Sans niveau"}",
                                    style = LZ.type.labelSmall,
                                    color = LZ.colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            // Assiduité — real Room data, fixed by the teacher's roll-calls
            item {
                LZCard(
                    modifier = Modifier
                        .padding(horizontal = LZSize.ScreenPadding)
                        .fillMaxWidth()
                        .testTag("student_assiduity_card"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LZText("Assiduité", style = LZ.type.cardTitle)
                        LZStatusPill(
                            text = if (assiduity.total == 0) "Aucune séance" else "${assiduity.ratePercent}% assiduité",
                            color = if (assiduity.total == 0) LZ.colors.textSecondary else LZ.colors.primary,
                            background = LZ.colors.surfaceSoft,
                            icon = Icons.Rounded.Verified,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StatColumn(
                            value = "${assiduity.attended}",
                            label = "Séances suivies",
                            valueColor = LZ.colors.primary,
                        )
                        StatColumn(
                            value = "${assiduity.lates}",
                            label = "Retards",
                            valueColor = LZ.colors.warning,
                        )
                        StatColumn(
                            value = "${assiduity.absent}",
                            label = "Absences",
                            valueColor = LZ.colors.error,
                        )
                        StatColumn(
                            value = "${assiduity.justified}",
                            label = "Justifiées",
                            valueColor = LZ.colors.textSecondary,
                        )
                    }
                    if (assiduity.total == 0) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.EventBusy, null, tint = LZ.colors.textSecondary, modifier = Modifier.size(14.dp))
                            LZText(
                                "Vos marques d'appel apparaîtront ici.",
                                style = LZ.type.secondary,
                                color = LZ.colors.textSecondary,
                            )
                        }
                    }
                }
            }

            // Settings — the one control surface: appearance, notifications, account
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    LZText("Paramètres", style = LZ.type.cardTitle)
                    Spacer(Modifier.height(6.dp))
                    LZText(
                        "Apparence, notifications et compte connecté.",
                        style = LZ.type.secondary,
                        color = LZ.colors.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    LZSecondaryButton(
                        text = "Ouvrir les paramètres",
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Settings,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ assiduity

/** Aggregated, real attendance numbers shown in the assiduity card. */
private data class AssiduityStats(
    val attended: Int,
    val lates: Int,
    val absent: Int,
    val justified: Int,
    val total: Int,
) {
    val ratePercent: Int get() = if (total == 0) 0 else (attended * 100) / total
}

/** Small centered number with its label, used by the assiduity summary row. */
@Composable
private fun StatColumn(
    value: String,
    label: String,
    valueColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        LZText(value, style = LZ.type.stat, color = valueColor ?: LZ.colors.textPrimary)
        LZText(label, style = LZ.type.label, color = LZ.colors.textSecondary)
    }
}



