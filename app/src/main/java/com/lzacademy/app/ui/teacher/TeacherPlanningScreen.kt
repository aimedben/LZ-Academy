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
import androidx.compose.material.icons.rounded.CalendarMonth
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
import com.lzacademy.app.components.LZCalendarDayItem
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDashboardSkeleton
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTimelineItem
import com.lzacademy.app.components.color
import com.lzacademy.app.data.FrenchDates
import com.lzacademy.app.models.TeacherDashboardData
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.TeacherDashboardViewModel
import com.lzacademy.app.viewmodel.UiState
import java.time.LocalDate

@Composable
fun TeacherPlanningScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherDashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UiState.Loading -> LZDashboardSkeleton(LZ.colors.teacherHeaderBrush, modifier)
        is UiState.Error -> LZEmptyState(
            title = "Chargement impossible",
            message = s.message,
            actionLabel = "Réessayer",
            onAction = viewModel::load,
            modifier = modifier.fillMaxSize(),
        )
        is UiState.Success -> TeacherPlanningContent(
            data = s.data,
            onShowMessage = onShowMessage,
            onAvatarClick = onAvatarClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun TeacherPlanningContent(
    data: TeacherDashboardData,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // days of the current week (Monday-first), numbered with the real dates
    val today = remember { LocalDate.now() }
    val weekStart = remember(today) { today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong()) }
    val days = remember(weekStart) {
        FrenchDates.dayShort.mapIndexed { i, label -> Triple(i, label, weekStart.plusDays(i.toLong()).dayOfMonth) }
    }
    // today's ISO weekday (1..7) → Monday-first index used by the selector
    val selectedDayIndex = remember(today) { today.dayOfWeek.value - 1 }

    val currentDaySessions = data.sessionsByDay[selectedDayIndex + 1].orEmpty()
    val totalHours = data.weeklyHours.values.sum()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("teacher_planning_screen"),
    ) {
        // Teacher gradient header
        LZGradientHeader(brush = LZ.colors.teacherHeaderBrush) {
            LZGreetingRow(
                initials = data.initials,
                title = "Planning des séances",
                subtitle = "${"%.1f".format(totalHours).replace(".", ",")} h cette semaine • Séances réelles",
                onAvatarClick = onAvatarClick,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Day selector tabs (real week)
            item {
                Column(Modifier.padding(top = 16.dp)) {
                    LZSectionHeader(
                        title = "Emploi du temps hebdomadaire",
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(days) { (index, label, dayNumber) ->
                            LZCalendarDayItem(
                                dayLabel = label.take(3),
                                dayNumber = dayNumber,
                                selected = selectedDayIndex == index,
                                hasLesson = data.sessionsByDay[index + 1]?.isNotEmpty() == true,
                                onClick = { onShowMessage("Consultez vos séances du jour sur le tableau de bord.") },
                                modifier = Modifier.size(width = 54.dp, height = 74.dp),
                            )
                        }
                    }
                }
            }

            // Volume hours card (real hours from the mirror)
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            LZText("Volume horaire hebdomadaire", style = LZ.type.cardTitle)
                            LZText(
                                "${"%.1f".format(totalHours).replace(".", ",")} h de séances planifiées cette semaine",
                                style = LZ.type.secondary,
                                color = LZ.colors.textSecondary,
                            )
                        }
                        LZStatusPill(
                            "${data.sessionsByDay.values.sumOf { it.size }} séances",
                            LZ.colors.primary,
                            LZ.colors.surfaceSoft,
                        )
                    }
                }
            }

            // Timeline header (today's real sessions)
            item {
                LZSectionHeader(
                    title = "Séances d'aujourd'hui",
                    actionLabel = "${currentDaySessions.size} séances",
                    modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                )
            }

            // Timeline items
            if (currentDaySessions.isEmpty()) {
                item {
                    LZEmptyState(
                        title = "Aucune séance aujourd'hui",
                        message = "Aucune donnée — vous n'avez pas de séance planifiée aujourd'hui.",
                        icon = Icons.Rounded.CalendarMonth,
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 24.dp),
                    )
                }
            } else {
                items(currentDaySessions.size) { index ->
                    val session = currentDaySessions[index]
                    Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                        LZTimelineItem(
                            time = session.start,
                            title = session.title,
                            subtitle = session.subtitle,
                            state = session.state,
                            accent = session.accent.color(),
                            isLast = index == currentDaySessions.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
