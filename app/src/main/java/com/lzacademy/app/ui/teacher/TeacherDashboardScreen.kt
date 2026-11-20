package com.lzacademy.app.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZChartCard
import com.lzacademy.app.components.LZBarChart
import com.lzacademy.app.components.LZDashboardSkeleton
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFileItem
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.rememberRemoteAvatar
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZHeaderIconButton
import com.lzacademy.app.components.LZHeroCard
import com.lzacademy.app.components.LZLinearProgressCard
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZStatCard
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTimelineItem
import com.lzacademy.app.components.color
import com.lzacademy.app.components.overlapTop
import com.lzacademy.app.ui.previews.PreviewSamples
import com.lzacademy.app.models.FileUi
import com.lzacademy.app.models.TeacherDashboardData
import com.lzacademy.app.models.TimelineLessonUi
import com.lzacademy.app.navigation.LZRoutes
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZStatusBarIcons
import com.lzacademy.app.theme.LZTheme
import com.lzacademy.app.viewmodel.TeacherDashboardViewModel
import com.lzacademy.app.viewmodel.UiState

class TeacherDashboardActions(
    val onAvatarClick: () -> Unit = {},
    val onAnalyticsClick: () -> Unit = {},
    val onTakeAttendance: () -> Unit = {},
    val onNextSessionClick: () -> Unit = {},
    val onTimelineClick: (TimelineLessonUi) -> Unit = {},
    val onSeeAllToday: () -> Unit = {},
    val onSeeAllPublications: () -> Unit = {},
    val onOpenFile: (FileUi) -> Unit = {},
    val onGroupClick: (String) -> Unit = {},
)

@Composable
fun TeacherDashboardRoute(
    onNavigate: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherDashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UiState.Loading -> {
            LZStatusBarIcons(lightIcons = true)
            LZDashboardSkeleton(LZ.colors.teacherHeaderBrush, modifier)
        }
        is UiState.Error -> LZEmptyState(
            title = "Chargement impossible",
            message = s.message,
            actionLabel = "Réessayer",
            onAction = viewModel::load,
            modifier = modifier.fillMaxSize(),
        )
        is UiState.Success -> TeacherDashboardScreen(
            data = s.data,
            modifier = modifier,
            actions = TeacherDashboardActions(
                onAvatarClick = onAvatarClick,
                onAnalyticsClick = { onShowMessage("Les statistiques détaillées arrivent dans la livraison Enseignants.") },
                onTakeAttendance = { onNavigate(LZRoutes.TeacherAttendance) },
                onNextSessionClick = {
                    val session = s.data.nextSession
                    if (session != null) {
                        onShowMessage("${session.title} • ${session.timeRange} • ${session.room}")
                    }
                },
                onTimelineClick = { onShowMessage("${it.title} • ${it.subtitle}") },
                onSeeAllToday = { onNavigate(LZRoutes.TeacherPlanning) },
                onSeeAllPublications = { onNavigate(LZRoutes.TeacherResources) },
                onOpenFile = { onShowMessage("Ouverture de « ${it.name} » (aperçu démo).") },
                onGroupClick = { onNavigate(LZRoutes.TeacherGroups) },
            ),
        )
    }
}

@Composable
fun TeacherDashboardScreen(
    data: TeacherDashboardData,
    modifier: Modifier = Modifier,
    actions: TeacherDashboardActions = TeacherDashboardActions(),
) {
    val c = LZ.colors
    var selectedBar by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState()),
    ) {
        LZStatusBarIcons(lightIcons = true)

        LZGradientHeader(brush = c.teacherHeaderBrush, extraBottomPadding = 44.dp) {
            LZGreetingRow(
                initials = data.initials,
                title = "Bonjour, ${data.name} 👋",
                subtitle = "Voici votre activité aujourd'hui",
                onAvatarClick = actions.onAvatarClick,
                photo = rememberRemoteAvatar(data.avatarUrl),
            ) {
                LZHeaderIconButton(Icons.Rounded.Analytics, "Statistiques", actions.onAnalyticsClick)
            }
        }

        // Six KPIs, two per row, overlapping the header
        Column(
            Modifier.padding(horizontal = 20.dp).overlapTop(44.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            data.stats.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { stat ->
                        LZStatCard(
                            icon = stat.icon,
                            value = stat.value,
                            label = stat.label,
                            accent = stat.accent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            // Next session — honest empty state when nothing is scheduled
            if (data.nextSession != null) {
                LZHeroCard(
                    eyebrow = "Prochaine séance",
                    title = data.nextSession.title,
                    subtitle = "${data.nextSession.room} • ${data.nextSession.studentCount} élèves",
                    meta = data.nextSession.timeRange,
                    brush = c.heroPurpleBrush,
                    metaIcon = Icons.Rounded.Schedule,
                    onClick = actions.onNextSessionClick,
                    action = {
                        LZPrimaryButton(
                            "Faire l'appel",
                            actions.onTakeAttendance,
                            leadingIcon = Icons.Rounded.HowToReg,
                            height = 44.dp,
                        )
                    },
                )
            } else {
                LZHeroCard(
                    eyebrow = "Prochaine séance",
                    title = "Aucune séance programmée",
                    subtitle = "Aucune donnée — planifiez une séance pour ce groupe.",
                    meta = "",
                    brush = c.heroPurpleBrush,
                    metaIcon = Icons.Rounded.Schedule,
                    onClick = {},
                    action = {
                        LZPrimaryButton(
                            "Faire l'appel",
                            actions.onTakeAttendance,
                            leadingIcon = Icons.Rounded.HowToReg,
                            height = 44.dp,
                        )
                    },
                )
            }

            // Today timeline
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LZSectionHeader("Aujourd'hui", actionLabel = "Planning", onAction = actions.onSeeAllToday)
                LZCard {
                    if (data.today.isEmpty()) {
                        LZText("Aucune donnée — pas de séance aujourd'hui.", style = LZ.type.secondary)
                    }
                    data.today.forEachIndexed { i, item ->
                        LZTimelineItem(
                            time = item.start,
                            title = item.title,
                            subtitle = item.subtitle,
                            accent = item.accent.color(),
                            state = item.state,
                            isLast = i == data.today.lastIndex,
                            onClick = { actions.onTimelineClick(item) },
                        )
                    }
                }
            }

            // Attendance per group
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LZSectionHeader("Présence par groupe", actionLabel = "Groupes", onAction = { actions.onGroupClick("") })
                LZCard(
                    onClick = { actions.onGroupClick("") },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (data.groupAttendance.isEmpty()) {
                            LZText("Aucune donnée — pas encore d'appel enregistré.", style = LZ.type.secondary)
                        }
                        data.groupAttendance.forEach { g ->
                            LZLinearProgressCard(
                                title = g.group,
                                subtitle = "${g.studentCount} élèves",
                                valueLabel = "${(g.rate * 100).toInt()} %",
                                progress = g.rate,
                                color = when {
                                    g.rate >= 0.93f -> c.success
                                    g.rate >= 0.89f -> c.primary
                                    else -> c.warning
                                },
                            )
                        }
                    }
                }
            }

            // Weekly hours
            LZChartCard(
                title = "Heures d'enseignement",
                subtitle = selectedBar?.let { "${data.weeklyHours.labels[it]} : ${formatHours(data.weeklyHours.values[it])}" }
                    ?: "Cette semaine • touchez une barre",
            ) {
                LZBarChart(
                    values = data.weeklyHours.values,
                    labels = data.weeklyHours.labels,
                    selectedIndex = selectedBar,
                    onSelect = { selectedBar = if (selectedBar == it) null else it },
                    chartHeight = 130.dp,
                )
            }

            // Publications
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LZSectionHeader("Mes dernières publications", actionLabel = "Voir tout", onAction = actions.onSeeAllPublications)
                LZCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    data.publications.forEachIndexed { i, file ->
                        LZFileItem(
                            name = file.name,
                            meta = file.meta,
                            kind = file.kind,
                            onClick = { actions.onOpenFile(file) },
                            onMore = { actions.onOpenFile(file) },
                        )
                        if (i < data.publications.lastIndex) LZDivider()
                    }
                }
            }
        }
    }
}

private fun formatHours(value: Float): String =
    if (value % 1f == 0f) "${value.toInt()} h" else "${value.toString().replace('.', ',')} h"

@Preview(name = "Enseignant – clair", showBackground = true, widthDp = 393, heightDp = 1700)
@Composable
private fun TeacherDashboardPreviewLight() {
    LZTheme(darkTheme = false) { TeacherDashboardScreen(PreviewSamples.teacher()) }
}

@Preview(
    name = "Enseignant – sombre", showBackground = true, widthDp = 393, heightDp = 1700,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TeacherDashboardPreviewDark() {
    LZTheme(darkTheme = true) { TeacherDashboardScreen(PreviewSamples.teacher()) }
}
