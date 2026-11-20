package com.lzacademy.app.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZCalendarDayItem
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDashboardSkeleton
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZLessonCard
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.color
import com.lzacademy.app.data.FrenchDates
import com.lzacademy.app.models.LessonUi
import com.lzacademy.app.models.StudentDashboardData
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.StudentDashboardViewModel
import com.lzacademy.app.viewmodel.UiState

private enum class PlanningMode(val label: String) { Day("Jour"), Week("Semaine") }

@Composable
fun StudentPlanningScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onOpenPdf: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StudentDashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UiState.Loading -> LZDashboardSkeleton(LZ.colors.studentHeaderBrush, modifier)
        is UiState.Error -> LZEmptyState(
            title = "Chargement impossible",
            message = s.message,
            actionLabel = "Réessayer",
            onAction = viewModel::load,
            modifier = modifier.fillMaxSize(),
        )
        is UiState.Success -> StudentPlanningContent(
            data = s.data,
            onNavigateBack = onNavigateBack,
            onShowMessage = onShowMessage,
            onAvatarClick = onAvatarClick,
            onOpenPdf = onOpenPdf,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentPlanningContent(
    data: StudentDashboardData,
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onOpenPdf: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allLessons = data.lessonsByDay.values.flatten()
    val subjects = remember(allLessons) {
        listOf("Tous") + allLessons.map { it.subject }.distinct()
    }

    var mode by rememberSaveable { mutableStateOf(PlanningMode.Day) }
    var selectedDayIndex by rememberSaveable { mutableIntStateOf(data.todayIndex) }
    var subjectFilter by rememberSaveable { mutableStateOf("Tous") }
    var selectedLesson by remember { mutableStateOf<LessonUi?>(null) }
    var showPdfSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // couleurs d'accent résolues ici (composable) pour être utilisées par le générateur PDF
    val lessonColors = allLessons.associateWith { it.accent.color().toArgb() }

    fun withPdf(action: (java.io.File) -> Unit) {
        scope.launch {
            val file = try {
                withContext(Dispatchers.IO) { PlanningPdf.create(context, data) { lessonColors[it] ?: 0xFF534AB7.toInt() } }
            } catch (e: Exception) {
                onShowMessage("Impossible de créer le PDF")
                null
            }
            if (file != null) {
                action(file)
                showPdfSheet = false
            }
        }
    }

    fun List<LessonUi>.filtered() =
        if (subjectFilter == "Tous") this
        else filter { it.subject.contains(subjectFilter, ignoreCase = true) }

    val dayLessons = data.lessonsByDay[selectedDayIndex].orEmpty().filtered()
    val doneCount = dayLessons.count { it.done }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("student_planning_screen"),
    ) {
        LZGradientHeader(brush = LZ.colors.studentHeaderBrush) {
            LZGreetingRow(
                initials = data.initials,
                title = "Planning",
                subtitle = "Mes séances • semaine en cours",
                onAvatarClick = onAvatarClick,
                photo = rememberOwnStudentPhoto(),
                trailing = {
                    IconButton(
                        onClick = { showPdfSheet = true },
                        modifier = Modifier.testTag("view_planning_pdf"),
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, "Voir le PDF", tint = Color.White)
                    }
                },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Segmented toggle Jour / Semaine
            item {
                SegmentedToggle(
                    selected = mode,
                    onSelect = { mode = it },
                    modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                )
            }

            // 2. Week strip: all days visible at once, equal width
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LZSize.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    data.week.forEach { day ->
                        LZCalendarDayItem(
                            dayLabel = day.label,
                            dayNumber = day.number,
                            selected = mode == PlanningMode.Day && selectedDayIndex == day.index,
                            hasLesson = data.lessonsByDay[day.index]?.isNotEmpty() == true,
                            onClick = {
                                selectedDayIndex = day.index
                                mode = PlanningMode.Day
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(74.dp),
                        )
                    }
                }
            }

            // 3. Subject filters
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(subjects) { subject ->
                        LZFilterChip(
                            label = subject,
                            selected = subjectFilter == subject,
                            onClick = { subjectFilter = subject },
                        )
                    }
                }
            }

            if (mode == PlanningMode.Day) {
                // 4a. Day summary
                item {
                    LZCard(
                        modifier = Modifier
                            .padding(horizontal = LZSize.ScreenPadding)
                            .fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                LZText(
                                    FrenchDates.dayLong.getOrElse(selectedDayIndex) { "Ce jour" },
                                    style = LZ.type.cardTitle,
                                )
                                LZText(
                                    if (dayLessons.isEmpty()) "Aucune séance"
                                    else "${dayLessons.size} séance(s) • $doneCount terminée(s)",
                                    style = LZ.type.secondary,
                                    color = LZ.colors.textSecondary,
                                )
                            }
                            if (selectedDayIndex == data.todayIndex) {
                                LZStatusPill(
                                    text = "Aujourd'hui",
                                    color = LZ.colors.primary,
                                    background = LZ.colors.surfaceSoft,
                                    icon = Icons.Rounded.CalendarMonth,
                                )
                            } else {
                                LZSecondaryButton("Aujourd'hui", { selectedDayIndex = data.todayIndex })
                            }
                        }
                    }
                }

                // 4b. Timeline
                if (dayLessons.isEmpty()) {
                    item {
                        LZEmptyState(
                            title = "Rien de prévu",
                            message = "Aucune séance ce jour pour vos groupes.",
                            icon = Icons.Rounded.EventBusy,
                            actionLabel = if (subjectFilter != "Tous") "Voir toutes les matières" else null,
                            onAction = { subjectFilter = "Tous" },
                            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 24.dp),
                        )
                    }
                } else {
                    items(dayLessons) { lesson ->
                        TimelineRow(
                            lesson = lesson,
                            onClick = { selectedLesson = lesson },
                            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                        )
                    }
                }
            } else {
                // 5. Week agenda: every day with lessons, grouped
                val daysWithLessons = data.week.filter {
                    data.lessonsByDay[it.index].orEmpty().filtered().isNotEmpty()
                }
                if (daysWithLessons.isEmpty()) {
                    item {
                        LZEmptyState(
                            title = "Semaine libre",
                            message = "Aucune séance cette semaine pour ce filtre.",
                            icon = Icons.Rounded.EventBusy,
                            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 24.dp),
                        )
                    }
                } else {
                    daysWithLessons.forEach { day ->
                        val lessons = data.lessonsByDay[day.index].orEmpty().filtered()
                        item(key = "h_${day.index}") {
                            LZSectionHeader(
                                title = "${FrenchDates.dayLong.getOrElse(day.index) { day.label }} ${day.number}",
                                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                            )
                        }
                        items(lessons) { lesson ->
                            TimelineRow(
                                lesson = lesson,
                                onClick = { selectedLesson = lesson },
                                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                            )
                        }
                    }
                }
            }
        }
    }

    // PDF : visualiser / télécharger / partager
    if (showPdfSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPdfSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = LZ.colors.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LZSize.ScreenPadding)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LZText("Planning de la semaine", style = LZ.type.pageTitle)
                LZText(
                    "PDF complet de vos séances, prêt à consulter ou à enregistrer.",
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                )
                LZPrimaryButton("Visualiser", { withPdf { PlanningPdf.open(context, it) } }, Modifier.fillMaxWidth())
                LZSecondaryButton(
                    "Télécharger",
                    {
                        withPdf {
                            val ok = PlanningPdf.saveToDownloads(context, it)
                            onShowMessage(if (ok) "PDF enregistré dans Téléchargements" else "Enregistrement impossible")
                        }
                    },
                    Modifier.fillMaxWidth(),
                )
                LZSecondaryButton("Partager", { withPdf { PlanningPdf.share(context, it) } }, Modifier.fillMaxWidth())
            }
        }
    }

    // Detail bottom sheet (only real data, nothing invented)
    selectedLesson?.let { lesson ->
        ModalBottomSheet(
            onDismissRequest = { selectedLesson = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = LZ.colors.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LZSize.ScreenPadding)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LZText(lesson.subject, style = LZ.type.pageTitle)
                DetailLine(Icons.Rounded.Schedule, lesson.timeRange, lesson.accent.color())
                DetailLine(Icons.Rounded.LocationOn, lesson.room, lesson.accent.color())
                DetailLine(Icons.Rounded.Person, lesson.person, lesson.accent.color())
                Spacer(Modifier.height(6.dp))
                LZSecondaryButton(
                    "Ajouter un rappel",
                    {
                        onShowMessage("Rappel programmé 15 min avant le cours de ${lesson.subject}")
                        selectedLesson = null
                    },
                    Modifier.fillMaxWidth(),
                )
                LZPrimaryButton("Fermer", { selectedLesson = null }, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Heure à gauche, ligne verticale avec point, carte de cours à droite. */
@Composable
private fun TimelineRow(
    lesson: LessonUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (start, end) = splitRange(lesson.timeRange)
    val accent = lesson.accent.color()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.End,
        ) {
            LZText(start, style = LZ.type.cardTitle)
            if (end.isNotEmpty()) {
                LZText(end, style = LZ.type.secondary, color = LZ.colors.textSecondary)
            }
        }
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (lesson.done) LZ.colors.textSecondary else accent),
            )
            Box(
                Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(LZ.colors.surfaceSoft),
            )
        }
        Box(Modifier.weight(1f).padding(bottom = 6.dp)) {
            LZLessonCard(
                title = lesson.subject,
                time = lesson.timeRange,
                location = lesson.room,
                person = lesson.person,
                accent = accent,
                done = lesson.done,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun SegmentedToggle(
    selected: PlanningMode,
    onSelect: (PlanningMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LZ.colors.surfaceSoft)
            .padding(4.dp),
    ) {
        PlanningMode.values().forEach { m ->
            val isSelected = m == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) LZ.colors.primary else Color.Transparent)
                    .clickable { onSelect(m) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                LZText(
                    m.label,
                    style = LZ.type.cardTitle,
                    color = if (isSelected) Color.White else LZ.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        LZText(text, style = LZ.type.body)
    }
}

private fun splitRange(range: String): Pair<String, String> {
    val parts = range.split("-", "–", "—").map { it.trim() }.filter { it.isNotEmpty() }
    return (parts.getOrNull(0) ?: range) to (parts.getOrNull(1) ?: "")
}
