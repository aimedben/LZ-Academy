package com.lzacademy.app.ui.student

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.color
import com.lzacademy.app.components.softColor
import com.lzacademy.app.theme.LZShapes
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZDashboardSkeleton
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZGraduationIllustration
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZHeaderIconButton
import com.lzacademy.app.components.rememberRemoteAvatar
import com.lzacademy.app.components.LZHeroCard
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZStatCard
import com.lzacademy.app.components.LZSubscriptionCard
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.TeacherShowcaseSection
import com.lzacademy.app.components.overlapTop
import com.lzacademy.app.ui.previews.PreviewSamples
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.models.StudentDashboardData
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.navigation.LZRoutes
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZStatusBarIcons
import com.lzacademy.app.theme.LZTheme
import com.lzacademy.app.viewmodel.StudentDashboardViewModel
import com.lzacademy.app.viewmodel.TeachersViewModel
import com.lzacademy.app.viewmodel.UiState

/**
 * Callbacks of the student dashboard. All default to no-ops so previews stay trivial.
 * No renewal callback: crediting sessions is the administration's job, never the student's.
 */
class StudentDashboardActions(
    val onBellClick: () -> Unit = {},
    val onAvatarClick: () -> Unit = {},
    val onNextLessonClick: () -> Unit = {},
    val onSubscriptionClick: () -> Unit = {},
    val onSeeAllNews: () -> Unit = {},
    val onAnnouncementOpened: (String) -> Unit = {},
    val onSeeAllTeachers: () -> Unit = {},
    val onTeacherClick: (String) -> Unit = {},
    val onContactTeacher: (String) -> Unit = {},
)

/** Stateful entry point used by navigation. */
@Composable
fun StudentDashboardRoute(
    onNavigate: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    studentId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: StudentDashboardViewModel = viewModel(),
    teachersViewModel: TeachersViewModel = viewModel(),
    attendanceViewModel: com.lzacademy.app.viewmodel.AttendanceViewModel = viewModel(),
    syncViewModel: com.lzacademy.app.viewmodel.SyncViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val featuredTeachers by teachersViewModel.featuredTeachers.collectAsStateWithLifecycle()
    val subscriptions by attendanceViewModel.subscriptions.collectAsStateWithLifecycle()
    // Real identity: the counter ledger is keyed by the real matricule — resolve it from
    // the Supabase mirror, and fall back to the mirrored subscription when the local
    // ledger has not been seeded yet.
    val context = androidx.compose.ui.platform.LocalContext.current
    val syncedStudents by syncViewModel.students.collectAsStateWithLifecycle()
    val appPrefs = remember { com.lzacademy.app.data.preferences.AppPreferences(context) }
    // Reactive: turning the switch off in Settings hides the badge immediately, without a restart.
    val notificationsEnabled by remember {
        appPrefs.notificationFlow(com.lzacademy.app.data.settings.STUDENT_NOTIFICATIONS, true)
    }.collectAsStateWithLifecycle(initialValue = true)
    val ownMatricule = remember { appPrefs.ownMatricule }
    val ownStudent = syncedStudents.firstOrNull { it.matricule == ownMatricule }
    // Persist the own avatar URL for the other student screens (planning, courses,
    // notifications, profile) — they don't hold the sync mirror themselves.
    LaunchedEffect(ownStudent?.avatarPath) {
        appPrefs.ownAvatarUrl = ownStudent?.avatarPath
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { com.lzacademy.app.data.supabase.SupabaseApi().avatarUrl(it) }
    }
    val subKey = ownStudent?.matricule ?: studentId
    val syncedSubs by syncViewModel.subscriptions.collectAsStateWithLifecycle()
    // Re-compute the dashboard once the real identity arrives from the sync — the first
    // load may race the Supabase sync and fall back to the generic "Élève" state.
    LaunchedEffect(ownStudent?.remoteId) { viewModel.load() }
    when (val s = state) {
        UiState.Loading -> {
            LZStatusBarIcons(lightIcons = true)
            LZDashboardSkeleton(LZ.colors.studentHeaderBrush, modifier)
        }
        is UiState.Error -> LZEmptyState(
            title = "Chargement impossible",
            message = s.message,
            actionLabel = "Réessayer",
            onAction = viewModel::load,
            modifier = modifier.fillMaxSize(),
        )
        is UiState.Success -> {
            val ledgerSub = subKey?.let { subscriptions[it] }
            val mirroredSub = syncedSubs.firstOrNull { it.studentMatricule == subKey }
            val remaining = ledgerSub?.remainingSessions ?: mirroredSub?.sessionsRemaining ?: 0
            val total = ledgerSub?.totalSessions ?: mirroredSub?.sessionsTotal ?: 4
            val statusText = ledgerSub?.statusLabel
                ?: mirroredSub?.status?.replaceFirstChar { it.uppercase() }
                ?: "Actif"
            val display = s.data.copy(remainingSessions = remaining, totalSessions = total)
            StudentDashboardScreen(
                data = display,
                statusLabel = subscriptionStatusLabel(display.remainingSessions),
                modifier = modifier,
                featuredTeachers = featuredTeachers,
                showNotificationBadge = notificationsEnabled,
                actions = StudentDashboardActions(
                    onBellClick = { onNavigate(LZRoutes.StudentNotifications) },
                    onAvatarClick = onAvatarClick,
                    onNextLessonClick = {
                        val lesson = display.nextLesson
                        if (lesson != null) {
                            onShowMessage("${lesson.subject} à ${lesson.whenLabel} • ${lesson.room}")
                        }
                    },
                    onSubscriptionClick = {
                        onShowMessage(
                            "Abonnement • $remaining/$total séances restantes • $statusText • renouvellement auprès de l'administration",
                        )
                    },
                    onSeeAllNews = { onNavigate(LZRoutes.StudentNotifications) },
                    onAnnouncementOpened = viewModel::markAnnouncementRead,
                    onSeeAllTeachers = { onNavigate(LZRoutes.StudentTeacherDirectory) },
                    onTeacherClick = { id -> onNavigate(LZRoutes.studentTeacherProfile(id)) },
                    onContactTeacher = { id -> onNavigate(LZRoutes.studentTeacherContact(id)) },
                ),
            )
        }
    }
}

/** Stateless student dashboard: identité, séances restantes, prochain cours, enseignants, actualités. */
@Composable
fun StudentDashboardScreen(
    data: StudentDashboardData,
    statusLabel: String,
    modifier: Modifier = Modifier,
    featuredTeachers: List<Teacher> = emptyList(),
    showNotificationBadge: Boolean = true,
    actions: StudentDashboardActions = StudentDashboardActions(),
) {
    val c = LZ.colors

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState()),
    ) {
        LZStatusBarIcons(lightIcons = true)

        LZGradientHeader(brush = c.studentHeaderBrush, extraBottomPadding = 50.dp) {
            LZGreetingRow(
                initials = data.initials,
                title = "Bonjour, ${data.firstName} 👋",
                subtitle = "",
                onAvatarClick = actions.onAvatarClick,
                photo = rememberRemoteAvatar(data.avatarUrl),
            ) {
                LZHeaderIconButton(
                    icon = Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    onClick = actions.onBellClick,
                    badge = if (showNotificationBadge) data.unreadNotifications else 0,
                )
            }
        }

        Row(
            Modifier.padding(horizontal = 20.dp).overlapTop(50.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LZStatCard(
                icon = Icons.Rounded.CalendarMonth,
                value = data.remainingSessions.toString(),
                label = "Séances restantes",
                modifier = Modifier.weight(1f),
                containerColor = c.surfaceSoft,
                onClick = actions.onSubscriptionClick,
            )
            LZStatCard(
                icon = Icons.Rounded.MenuBook,
                value = data.activeSubjects.toString(),
                label = "Matières actives",
                modifier = Modifier.weight(1f),
                accent = LessonAccent.Violet,
                containerColor = c.surfaceSoft,
            )
        }

        Column(
            Modifier.padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Next lesson • honest empty state when the student has no real session
            if (data.nextLesson != null) {
                LZHeroCard(
                    eyebrow = "Prochain cours",
                    title = data.nextLesson.subject,
                    subtitle = "Avec ${data.nextLesson.teacher} à ${data.nextLesson.room}",
                    meta = data.nextLesson.whenLabel,
                    brush = c.heroGreenBrush,
                    onClick = actions.onNextLessonClick,
                    illustration = {
                        LZGraduationIllustration(
                            Modifier.align(Alignment.CenterEnd),
                            size = 112.dp,
                        )
                    },
                )
            } else {
                LZHeroCard(
                    eyebrow = "Prochain cours",
                    title = "Pas encore de cours",
                    subtitle = "",
                    meta = "",
                    brush = c.heroGreenBrush,
                    onClick = {},
                    illustration = {
                        LZGraduationIllustration(
                            Modifier.align(Alignment.CenterEnd),
                            size = 112.dp,
                        )
                    },
                )
            }

            // Subscription • read-only for the student: only the administration renews.
            LZSubscriptionCard(
                remaining = data.remainingSessions,
                total = data.totalSessions,
                statusLabel = statusLabel,
                onClick = actions.onSubscriptionClick,
            )

            // Featured teachers
            TeacherShowcaseSection(
                teachers = featuredTeachers,
                onSeeAll = actions.onSeeAllTeachers,
                onOpenProfile = actions.onTeacherClick,
                onContact = actions.onContactTeacher,
            )

            // News • ultra-simple for kids: max 2 one-liners, tap = full list
            if (data.announcements.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LZSectionHeader("Actualités", actionLabel = "Voir tout", onAction = actions.onSeeAllNews)
                    data.announcements.take(2).forEach { news ->
                        KidNewsCard(
                            title = news.title,
                            subtitle = news.subtitle,
                            icon = news.icon,
                            accent = news.accent,
                            unread = news.unread,
                            onClick = actions.onSeeAllNews,
                        )
                    }
                }
            }
        }
    }
}

/** One-line news card for the kid-friendly home: icon + title, tap opens the full list. */
@Composable
private fun KidNewsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: LessonAccent,
    unread: Boolean,
    onClick: () -> Unit,
) {
    LZCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(accent.softColor(), LZShapes.small),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent.color(), modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LZText(title, style = LZ.type.bodyStrong, maxLines = 1)
                LZText(subtitle, style = LZ.type.secondary, color = LZ.colors.textSecondary, maxLines = 1)
            }
            if (unread) Box(Modifier.size(9.dp).background(LZ.colors.error, CircleShape))
        }
    }
}

// ---------------------------------------------------------------- previews

/** Label shown everywhere the student views the pack: derived, never stored. */
internal fun subscriptionStatusLabel(remaining: Int): String =
    if (remaining <= 0) "épuisé" else "Actif"

@Preview(name = "Élève • clair", showBackground = true, widthDp = 393, heightDp = 1500)
@Composable
private fun StudentDashboardPreviewLight() {
    LZTheme(darkTheme = false) {
        val data = PreviewSamples.student(java.time.LocalDate.of(2026, 11, 3), java.time.LocalTime.of(10, 30))
        StudentDashboardScreen(
            data,
            statusLabel = subscriptionStatusLabel(data.remainingSessions),
            featuredTeachers = PreviewSamples.featured,
        )
    }
}

@Preview(
    name = "Élève • sombre", showBackground = true, widthDp = 393, heightDp = 1500,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun StudentDashboardPreviewDark() {
    LZTheme(darkTheme = true) {
        val data = PreviewSamples.student(java.time.LocalDate.of(2026, 11, 3), java.time.LocalTime.of(10, 30))
        StudentDashboardScreen(
            data,
            statusLabel = subscriptionStatusLabel(data.remainingSessions),
            featuredTeachers = PreviewSamples.featured,
        )
    }
}

@Preview(name = "Élève • 360dp", showBackground = true, widthDp = 360, heightDp = 1500)
@Composable
private fun StudentDashboardPreviewSmall() {
    LZTheme(darkTheme = false) {
        val data = PreviewSamples.student(java.time.LocalDate.of(2026, 11, 7), java.time.LocalTime.of(9, 0))
        StudentDashboardScreen(
            data,
            statusLabel = subscriptionStatusLabel(data.remainingSessions),
            featuredTeachers = PreviewSamples.featured,
        )
    }
}

