package com.lzacademy.app.ui.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.LZCalendarDayItem
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZCategoryTabs
import com.lzacademy.app.components.LZDashboardSkeleton
import com.lzacademy.app.components.rememberRemoteAvatar
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZDropdownField
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZFloatingButton
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZOutlinedButton
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSearchBar
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZSegmentedControl
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTextInput
import com.lzacademy.app.data.FrenchDates
import com.lzacademy.app.data.supabase.RemoteGroup
import com.lzacademy.app.data.supabase.RemoteSession
import com.lzacademy.app.data.supabase.RemoteSlot
import com.lzacademy.app.data.supabase.RemoteStudent
import com.lzacademy.app.data.supabase.RemoteTeacher
import com.lzacademy.app.data.supabase.RemoteTeacherPayment
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.AdminManageViewModel
import com.lzacademy.app.viewmodel.UiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val manageTabs = listOf("Élèves", "Enseignants", "Groupes", "Salles", "Planning")

/** Label of the add button per tab (header action + section headers). */
private fun addLabel(tab: Int) = manageTabs[tab]

private val adminTeacherFilters = listOf("Tous", "Accueil élèves", "Retirés", "Disponibles", "Hors ligne")

private val lastFeaturedFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

/**
 * Fenêtre de formulaire LARGE et scrollable — utilisée par toutes les fenêtres de Gestion
 * (élèves, enseignants, groupes, salles, paiements, packs) : jamais de contenu coupé.
 */
@Composable
internal fun LZFormDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LZCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState()),
            contentPadding = PaddingValues(18.dp),
        ) {
            content()
        }
    }
}

@Composable
fun AdminManagementScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminManageViewModel = viewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // one-shot action result → snackbar message
    val actionMessage by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            onShowMessage(it)
            viewModel.consumeMessage()
        }
    }

    // dialog state: null = closed, "add"/"edit" paired with the edited row (null for add)
    var studentDialog by remember { mutableStateOf<Pair<String, RemoteStudent?>?>(null) }
    var teacherDialog by remember { mutableStateOf<Pair<String, RemoteTeacher?>?>(null) }
    var groupDialog by remember { mutableStateOf<Pair<String, RemoteGroup?>?>(null) }
    var roomDialog by remember { mutableStateOf<Pair<String, SupabaseApi.RemoteRoom?>?>(null) }
    var slotDialog by remember { mutableStateOf<Pair<String, RemoteSlot?>?>(null) }
    var sessionDialog by remember { mutableStateOf<Pair<String, RemoteSession?>?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    val openAdd: () -> Unit = {
        when (selectedTab) {
            0 -> studentDialog = "add" to null
            1 -> teacherDialog = "add" to null
            2 -> groupDialog = "add" to null
            3 -> roomDialog = "add" to null
            else -> slotDialog = "add" to null
        }
    }

    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LZ.colors.background)
                .testTag("admin_management_screen"),
        ) {
            LZGradientHeader(brush = LZ.colors.adminHeaderBrush) {
                LZGreetingRow(
                    initials = "LZ",
                    title = "Gestion de l'Académie",
                    subtitle = "Base réelle • inscrire, modifier, supprimer",
                    onAvatarClick = onAvatarClick,
                    photo = rememberAdminLocalPhoto(),
                )
            }

            when (val s = state) {
                UiState.Loading -> LZDashboardSkeleton(LZ.colors.adminHeaderBrush, Modifier.fillMaxSize())
                is UiState.Error -> LZEmptyState(
                    title = "Chargement impossible",
                    message = s.message,
                    actionLabel = "Réessayer",
                    onAction = { viewModel.load(force = true) },
                    modifier = Modifier.fillMaxSize(),
                )
                is UiState.Success -> ManageContent(
                    data = s.data,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onAdd = openAdd,
                    onEditStudent = { studentDialog = "edit" to it },
                    onEditTeacher = { teacherDialog = "edit" to it },
                    onEditGroup = { groupDialog = "edit" to it },
                    onEditRoom = { roomDialog = "edit" to it },
                    onAddSlot = { slotDialog = "add" to null },
                    onAddSession = { sessionDialog = "add" to null },
                    onEditSlot = { slotDialog = "edit" to it },
                    onEditSession = { sessionDialog = "edit" to it },
                    onDelete = { deleteTarget = it },
                    viewModel = viewModel,
                )
            }
        }
        // Mockup FAB: emerald circle 56dp, bottom-right — shared add entry point
        LZFloatingButton(
            onClick = openAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("admin_add_button"),
        )
    }

    // ------------------------------------------------ dialogs

    studentDialog?.let { (mode, student) ->
        LZFormDialog(onDismiss = { studentDialog = null }) {
            ManageStudentForm(
                mode = mode,
                student = student,
                levels = (state as? UiState.Success)?.data?.levels.orEmpty(),
                onPickPhoto = { bytes ->
                    viewModel.updateStudentPhoto(student!!.remoteId, bytes)
                    // close immediately: the upload result arrives as a snackbar
                    studentDialog = null
                },
                onSave = { name, levelId, phone, guardian, guardianPhone ->
                    if (mode == "add") {
                        viewModel.addStudent(name, levelId, phone, guardian, guardianPhone)
                    } else {
                        viewModel.updateStudent(student!!.remoteId, name, levelId, phone, guardian, guardianPhone)
                    }
                    // close immediately: the result (success or server error) is emitted
                    // as a one-shot snackbar message by the ViewModel
                    studentDialog = null
                },
                onDismiss = { studentDialog = null },
            )
        }
    }

    teacherDialog?.let { (mode, teacher) ->
        LZFormDialog(onDismiss = { teacherDialog = null }) {
            ManageTeacherForm(
                mode = mode,
                teacher = teacher,
                onPickPhoto = { bytes ->
                    viewModel.updateTeacherPhoto(teacher!!.remoteId, bytes)
                    // close immediately: the upload result arrives as a snackbar
                    teacherDialog = null
                },
                onSave = { name, description, years ->
                    if (mode == "add") {
                        viewModel.addTeacher(name, description, years)
                    } else {
                        viewModel.updateTeacher(teacher!!.remoteId, name, description, years)
                    }
                    // close immediately: the result (success or server error) is emitted
                    // as a one-shot snackbar message by the ViewModel
                    teacherDialog = null
                },
                onDismiss = { teacherDialog = null },
            )
        }
    }

    groupDialog?.let { (mode, group) ->
        LZFormDialog(onDismiss = { groupDialog = null }) {
            ManageGroupForm(
                mode = mode,
                group = group,
                data = (state as? UiState.Success)?.data,
                onSave = { name, levelId, teacherId, roomId, capacity ->
                    if (mode == "add") {
                        viewModel.addGroup(name, levelId, teacherId, roomId, capacity)
                    } else {
                        viewModel.updateGroup(group!!.remoteId, name, levelId, teacherId, roomId, capacity)
                    }
                    // close immediately: the result (success or server error) is emitted
                    // as a one-shot snackbar message by the ViewModel
                    groupDialog = null
                },
                onDismiss = { groupDialog = null },
            )
        }
    }

    roomDialog?.let { (mode, room) ->
        LZFormDialog(onDismiss = { roomDialog = null }) {
            ManageRoomForm(
                mode = mode,
                room = room,
                onSave = { name, building, capacity, active ->
                    if (mode == "add") {
                        viewModel.addRoom(name, building, capacity)
                    } else {
                        viewModel.updateRoom(room!!.remoteId, name, building, capacity, active)
                    }
                    // close immediately: the result (success or server error) is emitted
                    // as a one-shot snackbar message by the ViewModel
                    roomDialog = null
                },
                onDismiss = { roomDialog = null },
            )
        }
    }

    slotDialog?.let { (mode, slot) ->
        LZFormDialog(onDismiss = { slotDialog = null }) {
            ManageSlotForm(
                mode = mode,
                slot = slot,
                data = (state as? UiState.Success)?.data,
                onSave = { groupId, weekday, start, end, roomId ->
                    if (mode == "add") {
                        viewModel.addSlot(groupId, weekday, start, end, roomId)
                    } else {
                        viewModel.updateSlot(slot!!.remoteId, groupId, weekday, start, end, roomId)
                    }
                    // close immediately: the result (success or server error) is emitted
                    // as a one-shot snackbar message by the ViewModel
                    slotDialog = null
                },
                onDismiss = { slotDialog = null },
            )
        }
    }

    sessionDialog?.let { (mode, session) ->
        LZFormDialog(onDismiss = { sessionDialog = null }) {
            ManageSessionForm(
                mode = mode,
                session = session,
                data = (state as? UiState.Success)?.data,
                onSave = { groupId, teacherId, subjectId, roomId, dateIso, start, end, topic, status ->
                    if (mode == "add") {
                        viewModel.addSession(groupId, teacherId, subjectId, roomId, dateIso, start, end, topic, status)
                    } else {
                        viewModel.updateSession(session!!.remoteId, groupId, teacherId, subjectId, roomId, dateIso, start, end, topic, status)
                    }
                    // close immediately: the result (success or server error) is emitted
                    // as a one-shot snackbar message by the ViewModel
                    sessionDialog = null
                },
                onDismiss = { sessionDialog = null },
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
                        val id = key.substringAfterLast("|")
                        val name = key.substringBeforeLast("|")
                        when (selectedTab) {
                            0 -> viewModel.deleteStudent(id, name)
                            1 -> viewModel.deleteTeacher(id, name)
                            2 -> viewModel.deleteGroup(id, name)
                            3 -> viewModel.deleteRoom(id, name)
                            else -> {
                                // "slot:<uuid>" / "session:<uuid>" disambiguates the Planning tab
                                val realId = id.substringAfter(':')
                                if (id.startsWith("slot:")) {
                                    viewModel.deleteSlot(realId, name)
                                } else {
                                    viewModel.deleteSession(realId, name)
                                }
                            }
                        }
                        // close immediately: the result (success or server error) is emitted
                        // as a one-shot snackbar message by the ViewModel
                        deleteTarget = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LZSecondaryButton("Annuler", { deleteTarget = null }, Modifier.fillMaxWidth())
        }
    }
}

/** deleteTarget encodes "name|id" so the confirm dialog knows what it deletes. */
internal fun deleteKey(name: String, id: String) = "$name|$id"

/** Weekly planning day labels, indexed like group_schedule_slots.weekday (1 = lundi). */
private val planningDayLabels = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

private fun slotDayLabel(weekday: Int): String =
    planningDayLabels.getOrElse(weekday - 1) { "Jour $weekday" }

/** "16:00:00" → "16:00". */
private fun slotTime(value: String): String = value.take(5)

/** "2026-11-25T15:00:00+00:00" → "Mer. 25 nov." (appareil timezone). */
private fun sessionDayLabel(iso: String): String = runCatching {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH))
        .replaceFirstChar { it.uppercase() }
}.getOrDefault("—")

/** "2026-11-25T15:00:00+00:00" → 15:00 (appareil timezone). */
private fun sessionTimeLabel(iso: String): String = runCatching {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("—")

/** Local calendar day of a session (used to place it on the admin week strip). */
private fun sessionLocalDate(iso: String): LocalDate? = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
}.recoverCatching {
    LocalDateTime.parse(iso).toLocalDate()
}.getOrNull()

private val sessionStatusLabels = listOf("Planifiée", "Terminée", "Annulée")

private fun sessionStatusLabel(status: String): String = when (status) {
    "completed" -> "Terminée"
    "cancelled" -> "Annulée"
    else -> "Planifiée"
}

private fun sessionStatusValue(label: String): String = when (label) {
    "Terminée" -> "completed"
    "Annulée" -> "cancelled"
    else -> "scheduled"
}

private val isoDatePattern = Regex("""\d{4}-\d{2}-\d{2}""")
private val isoTimePattern = Regex("""([01]\d|2[0-3]):[0-5]\d""")

@Composable
private fun ManageContent(
    data: AdminManageViewModel.ManageData,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAdd: () -> Unit,
    onEditStudent: (RemoteStudent) -> Unit,
    onEditTeacher: (RemoteTeacher) -> Unit,
    onEditGroup: (RemoteGroup) -> Unit,
    onEditRoom: (SupabaseApi.RemoteRoom) -> Unit,
    onAddSlot: () -> Unit,
    onAddSession: () -> Unit,
    onEditSlot: (RemoteSlot) -> Unit,
    onEditSession: (RemoteSession) -> Unit,
    onDelete: (String) -> Unit,
    viewModel: AdminManageViewModel,
) {
    var teacherFilter by rememberSaveable { mutableIntStateOf(0) }

    // Multi-selection: long-press a card to enter, tap toggles, "Tous" selects the whole
    // filtered list. Switching tab or acting clears it — nothing persistent, nothing extra.
    val selectedIds = remember { mutableStateListOf<String>() }
    LaunchedEffect(selectedTab) { selectedIds.clear() }
    val selecting = selectedIds.isNotEmpty()
    val toggle: (String) -> Unit = { id ->
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
    }

    var bulkAssignOpen by remember { mutableStateOf(false) }

    // teacher payments: record dialog + history dialog (real public.teacher_payments rows)
    var paymentTeacher by remember { mutableStateOf<RemoteTeacher?>(null) }
    var historyTeacher by remember { mutableStateOf<RemoteTeacher?>(null) }

    // teacher details: cards stay compact, tapping one opens this window
    var teacherDetail by remember { mutableStateOf<RemoteTeacher?>(null) }

    // planning: same day-centric view as the student space, plus a group filter.
    // 0 = Jour, 1 = Semaine; day index 0..6 (Monday first); null = all groups.
    var planningMode by rememberSaveable { mutableIntStateOf(0) }
    var planningDay by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value - 1) }
    var planningGroupId by rememberSaveable { mutableStateOf<String?>(null) }

    // student detail sheet (blurred bottom sheet): tap a student card when not selecting
    var studentSheet by remember { mutableStateOf<RemoteStudent?>(null) }
    var sheetAddCourse by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 16.dp)) {
            LZCategoryTabs(
                options = manageTabs,
                selectedIndex = selectedTab,
                onSelect = onTabSelected,
                icons = listOf(
                    Icons.Rounded.People,
                    Icons.Rounded.School,
                    Icons.Rounded.Groups,
                    Icons.Rounded.MeetingRoom,
                    Icons.Rounded.CalendarMonth,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZSearchBar(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Rechercher dans ${manageTabs[selectedTab].lowercase()}...",
            )
        }
        if (selecting && (selectedTab == 0 || selectedTab == 1)) {
            Spacer(Modifier.height(8.dp))
            SelectionBar(
                count = selectedIds.size,
                onSelectAll = {
                    selectedIds.clear()
                    when (selectedTab) {
                        0 -> data.students
                            .filter {
                                it.fullName.contains(searchQuery, ignoreCase = true) ||
                                    it.matricule.contains(searchQuery, ignoreCase = true)
                            }
                            .forEach { selectedIds.add(it.remoteId) }
                        else -> data.teachers
                            .filter {
                                searchQuery.isBlank() || it.fullName.contains(searchQuery, ignoreCase = true)
                            }
                            .filter { teacherMatchesFilter(it, teacherFilter) }
                            .forEach { selectedIds.add(it.remoteId) }
                    }
                },
                onClearSelection = { selectedIds.clear() },
                onCloseSelection = { selectedIds.clear() },
            ) {
                when (selectedTab) {
                    0 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LZPrimaryButton(
                            text = "Affecter…",
                            onClick = { bulkAssignOpen = true },
                            modifier = Modifier.weight(1f),
                            leadingIcon = Icons.Rounded.GroupAdd,
                        )
                        LZSecondaryButton(
                            text = "Désactiver",
                            onClick = {
                                viewModel.bulkSetStudentActive(selectedIds.toList(), false)
                                selectedIds.clear()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LZSecondaryButton(
                            text = "Retirer de l'accueil",
                            onClick = {
                                viewModel.bulkSetTeacherFeatured(selectedIds.toList(), false)
                                selectedIds.clear()
                            },
                            modifier = Modifier.weight(1f),
                        )
                        LZSecondaryButton(
                            text = "Désactiver",
                            onClick = {
                                viewModel.bulkSetTeacherActive(selectedIds.toList(), false)
                                selectedIds.clear()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (selectedTab) {
                0 -> studentsSection(data, searchQuery, selecting, selectedIds, toggle, onAdd, onEditStudent, onDelete, viewModel, onOpenStudent = { studentSheet = it })
                1 -> teachersSection(
                    data, searchQuery, teacherFilter, { teacherFilter = it }, selecting, selectedIds, toggle,
                    onAdd, onDelete,
                    onOpen = { teacherDetail = it },
                )
                2 -> groupsSection(data, searchQuery, onAdd, onEditGroup, onDelete)
                3 -> roomsSection(data, searchQuery, onAdd, onEditRoom, onDelete)
                else -> planningSection(
                    data, searchQuery,
                    mode = planningMode,
                    onModeSelected = { planningMode = it },
                    dayIndex = planningDay,
                    onDaySelected = { planningDay = it },
                    groupId = planningGroupId,
                    onGroupSelected = { planningGroupId = it },
                    onAddSlot = onAddSlot,
                    onAddSession = onAddSession,
                    onEditSlot = onEditSlot,
                    onEditSession = onEditSession,
                    onDelete = onDelete,
                )
            }
        }
    }

    if (bulkAssignOpen) {
        LZFormDialog(onDismiss = { bulkAssignOpen = false }) {
            BulkAssignDialog(
                count = selectedIds.size,
                levels = data.levels,
                groups = data.groups,
                onApplyLevel = { levelId, levelName ->
                    viewModel.bulkAssignLevel(selectedIds.toList(), levelId, levelName)
                    selectedIds.clear()
                    bulkAssignOpen = false
                },
                onApplyGroup = { groupId, groupName ->
                    viewModel.bulkAddToGroup(selectedIds.toList(), groupId, groupName)
                    selectedIds.clear()
                    bulkAssignOpen = false
                },
                onDismiss = { bulkAssignOpen = false },
            )
        }
    }

    paymentTeacher?.let { teacher ->
        LZFormDialog(onDismiss = { paymentTeacher = null }) {
            ManageTeacherPaymentForm(
                teacher = teacher,
                onSave = { amount, method, note, paidAtIso ->
                    viewModel.addTeacherPayment(teacher.remoteId, amount, method, note, paidAtIso)
                    // close immediately: the result arrives as a one-shot snackbar
                    paymentTeacher = null
                },
                onDismiss = { paymentTeacher = null },
            )
        }
    }

    historyTeacher?.let { teacher ->
        LZFormDialog(onDismiss = { historyTeacher = null }) {
            TeacherPaymentHistoryDialog(
                teacher = teacher,
                payments = data.teacherPayments.filter { it.teacherId == teacher.remoteId },
                onDelete = { viewModel.deleteTeacherPayment(it) },
                onDismiss = { historyTeacher = null },
            )
        }
    }

    teacherDetail?.let { selected ->
        // Re-read the row from the live data so a feature toggle is reflected instantly.
        val teacher = data.teachers.firstOrNull { it.remoteId == selected.remoteId } ?: selected
        LZFormDialog(onDismiss = { teacherDetail = null }) {
            TeacherDetailDialog(
                teacher = teacher,
                lastPayment = lastPaymentLabel(data.teacherPayments, teacher.remoteId),
                onToggleFeatured = { viewModel.toggleTeacherFeatured(teacher.remoteId, !teacher.isFeatured) },
                onEdit = {
                    teacherDetail = null
                    onEditTeacher(teacher)
                },
                onRecordPayment = {
                    teacherDetail = null
                    paymentTeacher = teacher
                },
                onShowHistory = {
                    teacherDetail = null
                    historyTeacher = teacher
                },
                onDelete = {
                    teacherDetail = null
                    onDelete(deleteKey(teacher.fullName, teacher.remoteId))
                },
                onDismiss = { teacherDetail = null },
            )
        }
    }

    studentSheet?.let { student ->
        StudentDetailSheet(
            student = student,
            data = data,
            onDismiss = { studentSheet = null; sheetAddCourse = false },
            onEdit = {
                studentSheet = null
                sheetAddCourse = false
                onEditStudent(it)
            },
            onToggleActive = { id, active -> viewModel.toggleStudentActive(id, active) },
            onAddCourse = { studentId, groupId, groupName ->
                viewModel.addEnrollment(studentId, groupId, groupName)
                sheetAddCourse = false
            },
            onRemoveCourse = { enrollmentId, groupName ->
                viewModel.removeEnrollment(enrollmentId, groupName)
            },
        )
    }
}

// ------------------------------------------------------------ students

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.studentsSection(
    data: AdminManageViewModel.ManageData,
    searchQuery: String,
    selecting: Boolean,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (RemoteStudent) -> Unit,
    onDelete: (String) -> Unit,
    viewModel: AdminManageViewModel,
    onOpenStudent: (RemoteStudent) -> Unit,
) {
    val filtered = data.students.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) || it.matricule.contains(searchQuery, ignoreCase = true)
    }
    item {
        LZSectionHeader(
            title = "Élèves inscrits",
            actionLabel = addLabel(0),
            onAction = onAdd,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
        LZText(
            "${data.students.size} élèves dans la base • ${data.students.count { !it.isActive }} désactivés" +
                if (selecting) " • appui long pour sélectionner" else "",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 2.dp),
        )
    }
    if (filtered.isEmpty()) {
        item {
            LZEmptyState(
                title = "Aucun élève",
                message = "Aucune donnée — la base ne contient pas encore d'élève correspondant.",
                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 16.dp),
            )
        }
    }
    items(filtered, key = { it.remoteId }) { student ->
        val selected = student.remoteId in selectedIds
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZCard(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (selecting) onToggle(student.remoteId) else onOpenStudent(student)
                        },
                        onLongClick = { onToggle(student.remoteId) },
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (selecting) SelectionCheck(selected)
                    RemoteCardAvatar(
                        initials = initialsOf(student.fullName),
                        url = student.avatarPath?.let { SupabaseApi().avatarUrl(it) },
                        size = 40.dp,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LZText(student.fullName, style = LZ.type.cardTitle, maxLines = 1)
                        LZText(
                            "${student.matricule} • ${student.levelName ?: "Sans niveau"}",
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    ManagePill(
                        if (student.isActive) "Actif" else "Désactivé",
                        if (student.isActive) LZ.colors.success else LZ.colors.textSecondary,
                        if (student.isActive) LZ.colors.successSoft else LZ.colors.surfaceSoft,
                    )
                    if (!selecting) {
                        ManageIconButton(Icons.Rounded.Edit, "Modifier", LZ.colors.textSecondary) { onEdit(student) }
                        ManageIconButton(Icons.Rounded.Delete, "Supprimer", LZ.colors.error) {
                            onDelete(deleteKey(student.fullName, student.remoteId))
                        }
                    }
                }
                if (!selecting && !student.isActive) {
                    Spacer(Modifier.height(8.dp))
                    LZSecondaryButton(
                        text = "Réactiver",
                        onClick = { viewModel.toggleStudentActive(student.remoteId, true) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------ teachers

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.teachersSection(
    data: AdminManageViewModel.ManageData,
    searchQuery: String,
    teacherFilter: Int,
    onFilterSelected: (Int) -> Unit,
    selecting: Boolean,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (RemoteTeacher) -> Unit,
) {
    val filtered = data.teachers.filter { teacher ->
        val matchesQuery = searchQuery.isBlank() || teacher.fullName.contains(searchQuery, ignoreCase = true)
        matchesQuery && teacherMatchesFilter(teacher, teacherFilter)
    }
    item {
        LZCard(
            Modifier
                .padding(horizontal = LZSize.ScreenPadding)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OverviewStat("${data.teachers.count { it.isFeatured }}", "Sur l'accueil", LZ.colors.primary)
                OverviewStat("${data.teachers.count { !it.isFeatured }}", "Retirés", LZ.colors.textSecondary)
                OverviewStat("${data.teachers.count { it.acceptsMessages }}", "Disponibles", LZ.colors.success)
                OverviewStat("${data.teachers.count { !it.isActive }}", "Hors ligne", LZ.colors.warning)
            }
            Spacer(Modifier.height(8.dp))
            LZDivider()
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(adminTeacherFilters.size) { index ->
                    LZFilterChip(
                        label = adminTeacherFilters[index],
                        selected = teacherFilter == index,
                        onClick = { onFilterSelected(index) },
                    )
                }
            }
        }
    }
    items(filtered, key = { it.remoteId }) { teacher ->
        val selected = teacher.remoteId in selectedIds
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZCard(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (selecting) onToggle(teacher.remoteId) else onOpen(teacher) },
                        onLongClick = { onToggle(teacher.remoteId) },
                    ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (selecting) SelectionCheck(selected)
                    RemoteCardAvatar(
                        initials = initialsOf(teacher.fullName),
                        url = teacher.photoUrl?.let { SupabaseApi().avatarUrl(it) },
                        size = 44.dp,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        LZText(teacher.fullName, style = LZ.type.bodyStrong, maxLines = 1)
                        LZText(
                            teacher.subjects.joinToString(" • ").ifBlank { "Matières non définies" },
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    if (!selecting) {
                        if (teacher.isFeatured) {
                            LZStatusPill("Accueil", LZ.colors.primary, LZ.colors.surfaceSoft)
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "Détails",
                            tint = LZ.colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
    if (filtered.isEmpty()) {
        item {
            LZEmptyState(
                title = "Aucun enseignant",
                message = "Aucune donnée — l'équipe se remplit après ajout ou synchronisation.",
                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 16.dp),
            )
        }
    }
    item {
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZSecondaryButton(text = addLabel(1), onClick = onAdd, modifier = Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.PersonAdd)
        }
    }
}

/**
 * Details window of one teacher: the compact card only shows the essentials, everything
 * else (payments, visibility, edit, delete) lives here so the list stays readable.
 */
@Composable
private fun TeacherDetailDialog(
    teacher: RemoteTeacher,
    lastPayment: String,
    onToggleFeatured: () -> Unit,
    onEdit: () -> Unit,
    onRecordPayment: () -> Unit,
    onShowHistory: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    LZText(teacher.fullName, style = LZ.type.pageTitle)
    Spacer(Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RemoteCardAvatar(
            initials = initialsOf(teacher.fullName),
            url = teacher.photoUrl?.let { SupabaseApi().avatarUrl(it) },
            size = 84.dp,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LZText(
                teacher.subjects.joinToString(" • ").ifBlank { "Matières non définies" },
                style = LZ.type.bodyStrong,
            )
            LZText(
                "${teacher.yearsOfExperience} an(s) d'expérience",
                style = LZ.type.secondary,
                color = LZ.colors.textSecondary,
            )
            LZText(
                if (teacher.isActive) "Compte actif" else "Compte hors ligne",
                style = LZ.type.secondary,
                color = if (teacher.isActive) LZ.colors.success else LZ.colors.warning,
            )
        }
    }
    if (!teacher.description.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        LZText(teacher.description, style = LZ.type.secondary, color = LZ.colors.textSecondary)
    }
    Spacer(Modifier.height(14.dp))
    LZDivider()
    Spacer(Modifier.height(12.dp))
    DetailRow(
        icon = Icons.Rounded.Visibility,
        label = if (teacher.isFeatured) "Visible sur l'accueil élèves" else "Retiré de l'accueil élèves",
        tint = if (teacher.isFeatured) LZ.colors.primary else LZ.colors.textSecondary,
    )
    Spacer(Modifier.height(10.dp))
    DetailRow(
        icon = Icons.Rounded.Payments,
        label = lastPayment,
        tint = LZ.colors.textSecondary,
    )
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LZSecondaryButton(
            text = if (teacher.isFeatured) "Retirer de l'accueil" else "Mettre sur l'accueil",
            onClick = onToggleFeatured,
            modifier = Modifier.weight(1f),
        )
        LZSecondaryButton(
            text = "Paiement…",
            onClick = onRecordPayment,
            modifier = Modifier.weight(1f),
            leadingIcon = Icons.Rounded.Payments,
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LZSecondaryButton(
            text = "Historique",
            onClick = onShowHistory,
            modifier = Modifier.weight(1f),
            leadingIcon = Icons.Rounded.History,
        )
        LZSecondaryButton(
            text = "Modifier",
            onClick = onEdit,
            modifier = Modifier.weight(1f),
            leadingIcon = Icons.Rounded.Edit,
        )
    }
    Spacer(Modifier.height(8.dp))
    LZSecondaryButton(
        text = "Fermer",
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    LZOutlinedButton(
        text = "Supprimer de la base",
        onClick = onDelete,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = Icons.Rounded.Delete,
    )
    Spacer(Modifier.height(4.dp))
    LZText(
        "La suppression est définitive et demande une confirmation.",
        style = LZ.type.labelSmall,
        color = LZ.colors.textSecondary,
    )
}

/** Icon + text row used by the teacher details window. */
@Composable
private fun DetailRow(icon: ImageVector, label: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        LZText(label, style = LZ.type.secondary, color = LZ.colors.textSecondary, modifier = Modifier.weight(1f))
    }
}

// ------------------------------------------------------------ groups

private fun LazyListScope.groupsSection(
    data: AdminManageViewModel.ManageData,
    searchQuery: String,
    onAdd: () -> Unit,
    onEdit: (RemoteGroup) -> Unit,
    onDelete: (String) -> Unit,
) {
    val filtered = data.groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
    item {
        LZSectionHeader(
            title = "Groupes de l'académie",
            actionLabel = addLabel(2),
            onAction = onAdd,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
        val totalStudents = data.enrollmentCountByGroup.values.sum()
        LZText(
            "${data.groups.size} groupes • $totalStudents inscriptions actives • chaque groupe porte sa salle et son enseignant",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 2.dp),
        )
    }
    if (filtered.isEmpty()) {
        item {
            LZEmptyState(
                title = "Aucun groupe",
                message = "Aucune donnée — aucun groupe enregistré dans la base.",
                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 16.dp),
            )
        }
    }
    items(filtered, key = { it.remoteId }) { group ->
        val students = data.enrollmentCountByGroup[group.remoteId] ?: 0
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZCard(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Groups, null,
                        tint = LZ.colors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LZText(group.name, style = LZ.type.cardTitle, maxLines = 2)
                        LZText(
                            "${group.levelName ?: "Sans niveau"} • ${group.teacherName ?: "Sans enseignant"}",
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                            maxLines = 1,
                        )
                        LZText(
                            "Salle : ${group.roomName ?: "—"} • $students élève(s) • capacité ${group.capacity}",
                            style = LZ.type.labelSmall,
                            color = LZ.colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    ManageIconButton(Icons.Rounded.Edit, "Modifier", LZ.colors.textSecondary) { onEdit(group) }
                    ManageIconButton(Icons.Rounded.Delete, "Supprimer", LZ.colors.error) {
                        onDelete(deleteKey(group.name, group.remoteId))
                    }
                }
            }
        }
    }
    item {
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZSecondaryButton(text = addLabel(2), onClick = onAdd, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ManageGroupForm(
    mode: String,
    group: RemoteGroup?,
    data: AdminManageViewModel.ManageData?,
    onSave: (name: String, levelId: String?, teacherId: String?, roomId: String?, capacity: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(group?.name.orEmpty()) }
    var levelName by remember { mutableStateOf(group?.levelName.orEmpty()) }
    var teacherName by remember { mutableStateOf(group?.teacherName.orEmpty()) }
    var roomName by remember { mutableStateOf(group?.roomName.orEmpty()) }
    var capacity by remember { mutableStateOf(group?.capacity?.takeIf { it > 0 }?.toString().orEmpty()) }
    var error by remember { mutableStateOf("") }
    val levels = data?.levels.orEmpty()
    val teachers = data?.teachers.orEmpty()
    val rooms = data?.rooms.orEmpty()

    Column(Modifier.fillMaxWidth()) {
        LZText(if (mode == "add") "Créer un groupe" else "Modifier le groupe", style = LZ.type.pageTitle)
        Spacer(Modifier.height(12.dp))
        LZTextInput(value = name, onValueChange = { name = it }, placeholder = "Nom du groupe *", leadingIcon = Icons.Rounded.Groups)
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = listOf("(aucun niveau)") + levels.map { it.name },
            selected = levelName.ifBlank { "(aucun niveau)" },
            onSelected = { levelName = if (it == "(aucun niveau)") "" else it },
            placeholder = "Niveau scolaire",
        )
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = listOf("(aucun enseignant)") + teachers.map { it.fullName },
            selected = teacherName.ifBlank { "(aucun enseignant)" },
            onSelected = { teacherName = if (it == "(aucun enseignant)") "" else it },
            placeholder = "Enseignant du groupe",
        )
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = listOf("(aucune salle)") + rooms.map { it.name },
            selected = roomName.ifBlank { "(aucune salle)" },
            onSelected = { roomName = if (it == "(aucune salle)") "" else it },
            placeholder = "Salle par défaut",
        )
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = capacity, onValueChange = { capacity = it }, placeholder = "Capacité (places)", leadingIcon = Icons.Rounded.MeetingRoom)
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(14.dp))
        LZPrimaryButton(
            text = if (mode == "add") "Créer le groupe" else "Enregistrer les modifications",
            onClick = {
                if (name.isBlank()) {
                    error = "Le nom du groupe est obligatoire."
                } else {
                    onSave(
                        name.trim(),
                        levels.firstOrNull { it.name == levelName }?.remoteId,
                        teachers.firstOrNull { it.fullName == teacherName }?.remoteId,
                        rooms.firstOrNull { it.name == roomName }?.remoteId,
                        parseDigits(capacity),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}

/**
 * Student detail — blurred bottom sheet sliding from the bottom, like a profile:
 * real identity, quick icon actions (edit, activate/deactivate, add a course)
 * and every course the student is enrolled in. Borders on all sides except the
 * top edge, rounded top corners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailSheet(
    student: RemoteStudent,
    data: AdminManageViewModel.ManageData,
    onDismiss: () -> Unit,
    onEdit: (RemoteStudent) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onAddCourse: (studentId: String, groupId: String, groupName: String) -> Unit,
    onRemoveCourse: (enrollmentId: String, groupName: String) -> Unit,
) {
    var addCourseOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val edgeColor = LZ.colors.textSecondary
    val courses = data.enrollments
        .filter { it.studentId == student.remoteId && it.leftOn == null }
        .mapNotNull { enrollment -> data.groups.firstOrNull { it.remoteId == enrollment.groupId }?.let { enrollment to it } }
    val enrolledGroupIds = courses.map { it.second.remoteId }.toSet()
    val joinable = data.groups.filter { it.remoteId !in enrolledGroupIds }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = LZ.colors.surface,
        dragHandle = null,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // borders on left / right / bottom only — the top edge stays clean
                val stroke = 1.dp.toPx()
                drawLine(edgeColor, Offset(0f, 0f), Offset(0f, size.height), stroke)
                drawLine(edgeColor, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
                drawLine(edgeColor, Offset(0f, size.height), Offset(size.width, size.height), stroke)
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LZSize.ScreenPadding)
                .padding(top = 18.dp, bottom = 28.dp),
        ) {
            // identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                RemoteCardAvatar(
                    initials = initialsOf(student.fullName),
                    url = student.avatarPath?.let { SupabaseApi().avatarUrl(it) },
                    size = 52.dp,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LZText(student.fullName, style = LZ.type.pageTitle, maxLines = 1)
                    LZText(
                        "${student.matricule} • ${student.levelName ?: "Sans niveau"}",
                        style = LZ.type.secondary,
                        color = LZ.colors.textSecondary,
                        maxLines = 1,
                    )
                }
                ManagePill(
                    if (student.isActive) "Actif" else "Désactivé",
                    if (student.isActive) LZ.colors.success else LZ.colors.textSecondary,
                    if (student.isActive) LZ.colors.successSoft else LZ.colors.surfaceSoft,
                )
            }
            Spacer(Modifier.height(12.dp))
            LZDivider()
            Spacer(Modifier.height(12.dp))

            // quick icon actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ManageIconButton(Icons.Rounded.Edit, "Modifier l'élève", LZ.colors.textSecondary) { onEdit(student) }
                ManageIconButton(
                    if (student.isActive) Icons.Rounded.Lock else Icons.Rounded.Check,
                    if (student.isActive) "Désactiver l'élève" else "Réactiver l'élève",
                    if (student.isActive) LZ.colors.error else LZ.colors.success,
                ) { onToggleActive(student.remoteId, !student.isActive) }
                ManageIconButton(Icons.Rounded.GroupAdd, "Ajouter un cours", LZ.colors.primary) { addCourseOpen = true }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))

            // contact + course count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Phone, null, tint = LZ.colors.textSecondary, modifier = Modifier.size(16.dp))
                LZText(
                    student.phone ?: "Téléphone non renseigné",
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Groups, null, tint = LZ.colors.textSecondary, modifier = Modifier.size(16.dp))
                LZText(
                    if (courses.isEmpty()) "Aucun cours" else "${courses.size} cours suivi(s)",
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                )
            }

            // add-a-course dropdown (multiple enrollments are supported)
            if (addCourseOpen) {
                Spacer(Modifier.height(10.dp))
                LZDropdownField(
                    options = if (joinable.isEmpty()) listOf("(aucun autre cours disponible)") else joinable.map { it.name },
                    selected = "Choisir un cours…",
                    onSelected = { chosen ->
                        addCourseOpen = false
                        joinable.firstOrNull { it.name == chosen }
                            ?.let { onAddCourse(student.remoteId, it.remoteId, it.name) }
                    },
                    placeholder = "Inscrire à un cours",
                )
            }

            Spacer(Modifier.height(12.dp))
            LZDivider()
            Spacer(Modifier.height(12.dp))
            LZText("Ses cours", style = LZ.type.cardTitle)
            Spacer(Modifier.height(8.dp))
            if (courses.isEmpty()) {
                LZText(
                    "Aucune donnée — l'élève n'est inscrit à aucun cours pour l'instant.",
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                )
            }
            courses.forEach { (enrollment, group) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Groups, null, tint = LZ.colors.primary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        LZText(group.name, style = LZ.type.bodyStrong, maxLines = 1)
                        LZText(
                            "${group.roomName ?: "Salle —"} • ${group.teacherName ?: "Enseignant —"} • ${group.levelName ?: "Sans niveau"}",
                            style = LZ.type.labelSmall,
                            color = LZ.colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    ManageIconButton(Icons.Rounded.Delete, "Retirer du cours", LZ.colors.error) {
                        onRemoveCourse(enrollment.remoteId, group.name)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ------------------------------------------------------------ rooms

private fun LazyListScope.roomsSection(
    data: AdminManageViewModel.ManageData,
    searchQuery: String,
    onAdd: () -> Unit,
    onEdit: (SupabaseApi.RemoteRoom) -> Unit,
    onDelete: (String) -> Unit,
) {
    val filtered = data.rooms.filter { it.name.contains(searchQuery, ignoreCase = true) }
    item {
        LZSectionHeader(
            title = "Salles de l'académie",
            actionLabel = addLabel(3),
            onAction = onAdd,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
        LZText(
            "${data.rooms.size} salles • capacité totale ${data.rooms.sumOf { it.capacity }} places",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 2.dp),
        )
    }
    if (filtered.isEmpty()) {
        item {
            LZEmptyState(
                title = "Aucune salle",
                message = "Aucune donnée — aucune salle enregistrée dans la base.",
                modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 16.dp),
            )
        }
    }
    items(filtered, key = { it.remoteId }) { room ->
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZCard(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.MeetingRoom, null,
                        tint = if (room.isActive) LZ.colors.primary else LZ.colors.textSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LZText(room.name, style = LZ.type.cardTitle, maxLines = 2)
                        LZText("${room.building} • ${room.capacity} places", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                    }
                    ManagePill(
                        if (room.isActive) "Active" else "Fermée",
                        if (room.isActive) LZ.colors.success else LZ.colors.textSecondary,
                        if (room.isActive) LZ.colors.successSoft else LZ.colors.surfaceSoft,
                    )
                    ManageIconButton(Icons.Rounded.Edit, "Modifier", LZ.colors.textSecondary) { onEdit(room) }
                    ManageIconButton(Icons.Rounded.Delete, "Supprimer", LZ.colors.error) {
                        onDelete(deleteKey(room.name, room.remoteId))
                    }
                }
            }
        }
    }
    item {
        Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
            LZSecondaryButton(text = addLabel(3), onClick = onAdd, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ------------------------------------------------------------ helpers

@Composable
private fun OverviewStat(value: String, label: String, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LZText(value, style = LZ.type.stat, color = tint)
        LZText(label, style = LZ.type.labelSmall, color = LZ.colors.textSecondary, maxLines = 1)
    }
}

// ------------------------------------------------------------ helpers

/** Card avatar with the remote photo when available, initials fallback otherwise. */
@Composable
private fun RemoteCardAvatar(initials: String, url: String?, size: androidx.compose.ui.unit.Dp) {
    LZAvatar(initials, size = size, photo = rememberRemoteAvatar(url))
}

/** Decodes a picked image and compresses it to a JPEG byte array (max 1024 px side). */
private fun readJpegBytes(context: Context, uri: Uri): ByteArray? = runCatching {
    val source = context.contentResolver.openInputStream(uri) ?: return null
    val decoded = BitmapFactory.decodeStream(source) ?: return null
    source.close()
    val ratio = maxOf(decoded.width, decoded.height).toFloat() / 1024f
    val scaled = if (ratio > 1f) {
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width / ratio).toInt().coerceAtLeast(1),
            (decoded.height / ratio).toInt().coerceAtLeast(1),
            true,
        )
    } else decoded
    val out = java.io.ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
    out.toByteArray()
}.getOrNull()

/** Which teachers match the active filter chip (shared by the list and the "Tous" button). */
private fun teacherMatchesFilter(teacher: RemoteTeacher, filterIndex: Int): Boolean = when (filterIndex) {
    0 -> true
    1 -> teacher.isFeatured
    2 -> !teacher.isFeatured
    3 -> teacher.acceptsMessages
    else -> !teacher.acceptsMessages
}

/** Action bar shown while a multi-selection is active (students/teachers tabs). */
@Composable
private fun SelectionBar(
    count: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCloseSelection: () -> Unit,
    actions: @Composable () -> Unit,
) {
    LZCard(
        Modifier
            .padding(horizontal = LZSize.ScreenPadding)
            .fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.DoneAll, null, tint = LZ.colors.primary, modifier = Modifier.size(18.dp))
            LZText(
                "$count sélectionné${if (count > 1) "s" else ""}",
                style = LZ.type.bodyStrong,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            ManageIconButton(Icons.Rounded.DoneAll, "Tout sélectionner", LZ.colors.primary, onSelectAll)
            ManageIconButton(Icons.Rounded.Clear, "Vider la sélection", LZ.colors.textSecondary, onClearSelection)
            ManageIconButton(Icons.Rounded.Close, "Quitter la sélection", LZ.colors.textSecondary, onCloseSelection)
        }
        Spacer(Modifier.height(10.dp))
        LZDivider()
        Spacer(Modifier.height(10.dp))
        actions()
    }
}

/** 24dp selection indicator shown at the left of a card in multi-selection mode. */
@Composable
private fun SelectionCheck(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) LZ.colors.primary else Color.Transparent)
            .border(2.dp, if (selected) LZ.colors.primary else LZ.colors.textSecondary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, "Sélectionné", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Bulk assignment, built on the real hierarchy: students belong to groups, and each group
 * carries its room, teacher and planning. Two simple targets only — the student's level
 * (direct column) or a group (real rows in public.enrollments).
 */
@Composable
private fun BulkAssignDialog(
    count: Int,
    levels: List<SupabaseApi.RemoteLevel>,
    groups: List<RemoteGroup>,
    onApplyLevel: (levelId: String, levelName: String) -> Unit,
    onApplyGroup: (groupId: String, groupName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Niveau, 1 = Groupe
    var levelName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val selectedGroup = groups.firstOrNull { it.name == groupName }

    Column(Modifier.fillMaxWidth()) {
        LZText("Affecter $count élève(s)", style = LZ.type.pageTitle)
        Spacer(Modifier.height(10.dp))
        LZSegmentedControl(options = listOf("Niveau", "Groupe"), selectedIndex = mode, onSelect = { mode = it; error = "" })
        Spacer(Modifier.height(8.dp))
        if (mode == 0) {
            LZDropdownField(
                options = levels.map { it.name },
                selected = levelName.ifBlank { "Choisir un niveau..." },
                onSelected = { levelName = it },
                placeholder = "Niveau scolaire",
            )
        } else {
            LZDropdownField(
                options = groups.map { it.name },
                selected = groupName.ifBlank { "Choisir un groupe..." },
                onSelected = { groupName = it },
                placeholder = "Groupe",
            )
            selectedGroup?.let { g ->
                Spacer(Modifier.height(6.dp))
                LZText(
                    "Salle : ${g.roomName ?: "—"} • Enseignant : ${g.teacherName ?: "—"} • Niveau : ${g.levelName ?: "—"}",
                    style = LZ.type.labelSmall,
                    color = LZ.colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LZText(
            if (mode == 0) "Le niveau est enregistré directement sur chaque élève."
            else "Les élèves appartiennent au groupe : salle et enseignant du groupe s'appliquent. Les élèves déjà inscrits sont ignorés.",
            style = LZ.type.labelSmall,
            color = LZ.colors.textSecondary,
        )
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(12.dp))
        LZPrimaryButton(
            text = "Affecter $count élève(s)",
            onClick = {
                if (mode == 0) {
                    if (levelName.isBlank()) {
                        error = "Choisissez un niveau."
                    } else {
                        val level = levels.first { it.name == levelName }
                        onApplyLevel(level.remoteId, level.name)
                    }
                } else {
                    if (groupName.isBlank()) {
                        error = "Choisissez un groupe."
                    } else {
                        val group = groups.first { it.name == groupName }
                        onApplyGroup(group.remoteId, group.name)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}

/** "M. Karim Lebbah" → "KL" (the honorifics are skipped). */
private fun initialsOf(fullName: String): String =
    fullName.split(" ")
        .filter { it.isNotBlank() && it != "M." && it != "Mme" }
        .take(2)
        .map { it.firstOrNull()?.uppercaseChar() ?: ' ' }
        .joinToString("")
        .ifBlank { "—" }

/** 124000.0 → "124 000" */
internal fun formatDzd(value: Double): String =
    "%,.0f".format(value).replace(",", " ")

/** Compact status pill used by the Gestion cards (leaves room for long names). */
@Composable
internal fun ManagePill(text: String, color: Color, background: Color) {
    Row(
        modifier = Modifier
            .background(background, LZShapes.pill)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LZText(text, style = LZ.type.labelSmall, color = color, maxLines = 1)
    }
}

/** Compact 32dp icon button used by the Gestion cards (edit / delete). */
@Composable
internal fun ManageIconButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

internal fun parseDigits(text: String): Int = text.filter { it.isDigit() }.toIntOrNull() ?: 0

// ------------------------------------------------------------ forms

@Composable
private fun ManageStudentForm(
    mode: String,
    student: RemoteStudent?,
    levels: List<SupabaseApi.RemoteLevel>,
    onPickPhoto: (ByteArray) -> Unit,
    onSave: (name: String, levelId: String?, phone: String?, guardian: String?, guardianPhone: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(student?.fullName.orEmpty()) }
    var phone by remember { mutableStateOf(student?.phone.orEmpty()) }
    var guardian by remember { mutableStateOf("") }
    var guardianPhone by remember { mutableStateOf("") }
    var levelName by remember { mutableStateOf(student?.levelName.orEmpty()) }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val bytes = uri?.let { readJpegBytes(context, it) }
        if (bytes != null) onPickPhoto(bytes)
    }

    Column(Modifier.fillMaxWidth()) {
        LZText(if (mode == "add") "Inscrire un élève" else "Modifier l'élève", style = LZ.type.pageTitle)
        student?.let {
            LZText(
                "${it.matricule} (matricule non modifiable)",
                style = LZ.type.labelSmall,
                color = LZ.colors.textSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (mode == "edit" && student != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RemoteCardAvatar(
                    initials = initialsOf(student.fullName),
                    url = student.avatarPath?.let { SupabaseApi().avatarUrl(it) },
                    size = 52.dp,
                )
                LZSecondaryButton(
                    "Changer la photo",
                    onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        LZTextInput(value = name, onValueChange = { name = it }, placeholder = "Nom complet *", leadingIcon = Icons.Rounded.Person)
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = listOf("(aucun niveau)") + levels.map { it.name },
            selected = levelName.ifBlank { "(aucun niveau)" },
            onSelected = { levelName = if (it == "(aucun niveau)") "" else it },
            placeholder = "Niveau scolaire",
        )
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = phone, onValueChange = { phone = it }, placeholder = "Téléphone élève", leadingIcon = Icons.Rounded.Phone)
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = guardian, onValueChange = { guardian = it }, placeholder = "Nom du tuteur")
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = guardianPhone, onValueChange = { guardianPhone = it }, placeholder = "Téléphone tuteur")
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(14.dp))
        LZPrimaryButton(
            text = if (mode == "add") "Valider l'inscription" else "Enregistrer les modifications",
            onClick = {
                if (name.isBlank()) {
                    error = "Le nom complet est obligatoire."
                } else {
                    val levelId = levels.firstOrNull { it.name == levelName }?.remoteId
                    onSave(
                        name.trim(),
                        levelId,
                        phone.trim().ifBlank { null },
                        guardian.trim().ifBlank { null },
                        guardianPhone.trim().ifBlank { null },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ManageTeacherForm(
    mode: String,
    teacher: RemoteTeacher?,
    onPickPhoto: (ByteArray) -> Unit,
    onSave: (name: String, description: String, years: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(teacher?.fullName.orEmpty()) }
    var description by remember { mutableStateOf(teacher?.description.orEmpty()) }
    var years by remember { mutableStateOf(teacher?.yearsOfExperience?.takeIf { it > 0 }?.toString().orEmpty()) }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val bytes = uri?.let { readJpegBytes(context, it) }
        if (bytes != null) onPickPhoto(bytes)
    }

    Column(Modifier.fillMaxWidth()) {
        LZText(if (mode == "add") "Ajouter un enseignant" else "Modifier l'enseignant", style = LZ.type.pageTitle)
        Spacer(Modifier.height(12.dp))
        if (mode == "edit" && teacher != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RemoteCardAvatar(
                    initials = initialsOf(teacher.fullName),
                    url = teacher.photoUrl?.let { SupabaseApi().avatarUrl(it) },
                    size = 52.dp,
                )
                LZSecondaryButton(
                    "Changer la photo",
                    onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        LZTextInput(value = name, onValueChange = { name = it }, placeholder = "Nom complet *", leadingIcon = Icons.Rounded.Person)
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = description, onValueChange = { description = it }, placeholder = "Présentation professionnelle")
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = years, onValueChange = { years = it }, placeholder = "Années d'expérience", leadingIcon = Icons.Rounded.School)
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(14.dp))
        LZPrimaryButton(
            text = if (mode == "add") "Ajouter à l'équipe" else "Enregistrer les modifications",
            onClick = {
                if (name.isBlank()) {
                    error = "Le nom complet est obligatoire."
                } else {
                    onSave(name.trim(), description.trim(), parseDigits(years))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}

/** Payment method choices (public payment_method enum values). */
private val paymentMethods = listOf("Espèces" to "cash", "CCP" to "ccp", "BaridiMob" to "baridimob", "Banque" to "bank")

private val dateInputFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")

/** "Dernier paiement : 1 nov • 25 000 DZD" or an honest empty state. */
private fun lastPaymentLabel(payments: List<RemoteTeacherPayment>, teacherId: String): String {
    val last = payments.filter { it.teacherId == teacherId }.maxByOrNull { it.paidAtIso }
    return if (last == null) {
        "Aucun paiement enregistré"
    } else {
        val date = runCatching { OffsetDateTime.parse(last.paidAtIso).toLocalDate() }.getOrNull()
        val when2 = date?.format(lastFeaturedFormat) ?: "—"
        "Dernier paiement : $when2 • ${dzd(last.amountDzd)}"
    }
}

/** "25 000 DZD". */
private fun dzd(amount: Double): String = "%,.0f".format(amount).replace(',', ' ') + " DZD"

/** Record a teacher payment: amount, method, optional note, real date (defaults to today). */
@Composable
private fun ManageTeacherPaymentForm(
    teacher: RemoteTeacher,
    onSave: (amountDzd: Double, method: String, note: String?, paidAtIso: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var methodIndex by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().format(dateInputFormat)) }
    var error by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        LZText("Paiement — ${teacher.fullName}", style = LZ.type.pageTitle, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        LZTextInput(value = amount, onValueChange = { amount = it }, placeholder = "Montant (DZD) *", leadingIcon = Icons.Rounded.Payments)
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = paymentMethods.map { it.first },
            selected = paymentMethods[methodIndex].first,
            onSelected = { selected -> methodIndex = paymentMethods.indexOfFirst { it.first == selected } },
            placeholder = "Méthode",
        )
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = date, onValueChange = { date = it }, placeholder = "Date de paiement (JJ/MM/AAAA) *")
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = note, onValueChange = { note = it }, placeholder = "Note (salaire, matériel…)")
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(14.dp))
        LZPrimaryButton(
            text = "Enregistrer le paiement",
            onClick = {
                val amountValue = amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                val paidDate = runCatching { LocalDate.parse(date.trim(), dateInputFormat) }.getOrNull()
                when {
                    amountValue == null || amountValue <= 0.0 -> error = "Saisissez un montant valide."
                    paidDate == null -> error = "Date invalide — format JJ/MM/AAAA."
                    else -> onSave(
                        amountValue,
                        paymentMethods[methodIndex].second,
                        note.trim().ifBlank { null },
                        paidDate.atTime(12, 0).atOffset(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}

/** Payment history of one teacher, newest first — a mistake can be deleted. */
@Composable
private fun TeacherPaymentHistoryDialog(
    teacher: RemoteTeacher,
    payments: List<RemoteTeacherPayment>,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        LZText("Paiements — ${teacher.fullName}", style = LZ.type.pageTitle, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        LZText(
            if (payments.isEmpty()) "Aucune donnée — aucun paiement enregistré."
            else "${payments.size} paiement(s) • total ${dzd(payments.sumOf { it.amountDzd })}",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        payments.forEach { payment ->
            val date = runCatching { OffsetDateTime.parse(payment.paidAtIso).toLocalDate() }.getOrNull()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    LZText(dzd(payment.amountDzd), style = LZ.type.bodyStrong)
                    LZText(
                        "${date?.format(lastFeaturedFormat) ?: "—"} • ${payment.method} • ${payment.note ?: ""}",
                        style = LZ.type.labelSmall,
                        color = LZ.colors.textSecondary,
                        maxLines = 1,
                    )
                }
                ManageIconButton(Icons.Rounded.Delete, "Supprimer", LZ.colors.error) { onDelete(payment.remoteId) }
            }
            Spacer(Modifier.height(8.dp))
            LZDivider()
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Fermer", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ManageRoomForm(
    mode: String,
    room: SupabaseApi.RemoteRoom?,
    onSave: (name: String, building: String, capacity: Int, active: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(room?.name.orEmpty()) }
    var building by remember { mutableStateOf(room?.building.orEmpty()) }
    var capacity by remember { mutableStateOf(room?.capacity?.takeIf { it > 0 }?.toString().orEmpty()) }
    var active by remember { mutableStateOf(room?.isActive ?: true) }
    var error by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        LZText(if (mode == "add") "Ajouter une salle" else "Modifier la salle", style = LZ.type.pageTitle)
        Spacer(Modifier.height(12.dp))
        LZTextInput(value = name, onValueChange = { name = it }, placeholder = "Nom de la salle *", leadingIcon = Icons.Rounded.MeetingRoom)
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = building, onValueChange = { building = it }, placeholder = "Bâtiment")
        Spacer(Modifier.height(8.dp))
        LZTextInput(value = capacity, onValueChange = { capacity = it }, placeholder = "Capacité (places)")
        if (mode == "edit") {
            Spacer(Modifier.height(8.dp))
            LZFilterChip(
                label = if (active) "Salle active" else "Salle fermée",
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
            text = if (mode == "add") "Créer la salle" else "Enregistrer les modifications",
            onClick = {
                val cap = parseDigits(capacity)
                when {
                    name.isBlank() -> error = "Le nom de la salle est obligatoire."
                    cap <= 0 -> error = "Indiquez une capacité valide."
                    else -> onSave(name.trim(), building.trim(), cap, active)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}

// ------------------------------------------------------------ planning (créneaux + séances)

/**
 * Onglet Planning : la même lecture que l'espace élève (bande de semaine + jour choisi),
 * avec deux options d'administration en plus :
 *  - filtre « Tous les groupes » ou un groupe précis ;
 *  - bascule Jour / Semaine.
 * Les créneaux récurrents et les séances datées partagent la timeline. Les actions
 * Modifier / Supprimer n'apparaissent qu'après un appui long sur la carte.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.planningSection(
    data: AdminManageViewModel.ManageData,
    searchQuery: String,
    mode: Int,
    onModeSelected: (Int) -> Unit,
    dayIndex: Int,
    onDaySelected: (Int) -> Unit,
    groupId: String?,
    onGroupSelected: (String?) -> Unit,
    onAddSlot: () -> Unit,
    onAddSession: () -> Unit,
    onEditSlot: (RemoteSlot) -> Unit,
    onEditSession: (RemoteSession) -> Unit,
    onDelete: (String) -> Unit,
) {
    val today = LocalDate.now()
    val weekStart = today.with(DayOfWeek.MONDAY)
    val todayIndex = today.dayOfWeek.value - 1
    val groupById = data.groups.associateBy { it.remoteId }
    val teacherById = data.teachers.associateBy { it.remoteId }

    fun matches(values: List<String?>): Boolean =
        searchQuery.isBlank() ||
            values.any { it?.contains(searchQuery, ignoreCase = true) == true }

    val slots = data.slots
        .filter { groupId == null || it.groupId == groupId }
        .filter { matches(listOf(it.groupName, it.roomName, groupById[it.groupId]?.teacherName)) }
        .sortedWith(compareBy({ it.weekday }, { it.start }))
    val sessions = data.sessions
        .filter { groupId == null || it.groupId == groupId }
        .filter { matches(listOf(it.groupName, it.teacherName, it.subjectName, it.roomName)) }
        .sortedBy { it.startsAtIso }

    fun slotsOf(index: Int) = slots.filter { it.weekday == index + 1 }
    fun sessionsOf(index: Int): List<RemoteSession> {
        val date = weekStart.plusDays(index.toLong())
        return sessions.filter { sessionLocalDate(it.startsAtIso) == date }
    }

    // 1. Titre + création
    item(key = "planning-header") {
        LZSectionHeader(
            title = "Planning",
            actionLabel = "Créneau",
            onAction = onAddSlot,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
        LZText(
            "La même vue que les élèves • appui long sur une carte pour modifier ou supprimer.",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 2.dp),
        )
    }

    // 2. Jour / Semaine
    item(key = "planning-mode") {
        LZSegmentedControl(
            options = listOf("Jour", "Semaine"),
            selectedIndex = mode,
            onSelect = onModeSelected,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
    }

    // 3. Bande de la semaine, identique à l'espace élève
    item(key = "planning-week") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LZSize.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (0..6).forEach { index ->
                val date = weekStart.plusDays(index.toLong())
                LZCalendarDayItem(
                    dayLabel = FrenchDates.dayShort[index],
                    dayNumber = date.dayOfMonth,
                    selected = mode == 0 && dayIndex == index,
                    hasLesson = slotsOf(index).isNotEmpty() || sessionsOf(index).isNotEmpty(),
                    onClick = {
                        onDaySelected(index)
                        onModeSelected(0)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp),
                )
            }
        }
    }

    // 4. Filtre de groupe (« Tous les groupes » par défaut)
    item(key = "planning-groups") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "planning-group-all") {
                LZFilterChip(
                    label = "Tous les groupes",
                    selected = groupId == null,
                    onClick = { onGroupSelected(null) },
                )
            }
            items(data.groups.sortedBy { it.name }, key = { "planning-group-${it.remoteId}" }) { group ->
                LZFilterChip(
                    label = group.name,
                    selected = groupId == group.remoteId,
                    onClick = { onGroupSelected(group.remoteId) },
                )
            }
        }
    }

    if (mode == 0) {
        val daySlots = slotsOf(dayIndex)
        val daySessions = sessionsOf(dayIndex)
        val date = weekStart.plusDays(dayIndex.toLong())

        item(key = "planning-day-summary") {
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
                            "${FrenchDates.dayLong[dayIndex]} ${date.dayOfMonth} ${FrenchDates.monthShort[date.monthValue - 1]}",
                            style = LZ.type.cardTitle,
                        )
                        LZText(
                            "${daySlots.size} créneau(x) • ${daySessions.size} séance(s)",
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                        )
                    }
                    if (dayIndex == todayIndex) {
                        LZStatusPill(
                            text = "Aujourd'hui",
                            color = LZ.colors.primary,
                            background = LZ.colors.surfaceSoft,
                            icon = Icons.Rounded.CalendarMonth,
                        )
                    } else {
                        LZSecondaryButton("Aujourd'hui", { onDaySelected(todayIndex) })
                    }
                }
            }
        }

        if (daySlots.isEmpty() && daySessions.isEmpty()) {
            item(key = "planning-day-empty") {
                LZEmptyState(
                    title = "Rien de prévu",
                    message = if (groupId == null) {
                        "Aucun créneau ni séance ce jour."
                    } else {
                        "Aucun créneau ni séance ce jour pour ce groupe."
                    },
                    icon = Icons.Rounded.CalendarMonth,
                    actionLabel = "Ajouter un créneau",
                    onAction = onAddSlot,
                    modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 8.dp),
                )
            }
        } else {
            if (daySlots.isNotEmpty()) {
                item(key = "planning-day-slots-h") {
                    LZSectionHeader(
                        title = "Créneaux • ${daySlots.size}",
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                }
                adminSlotRows(daySlots, groupById, teacherById, "ps-$dayIndex", onEditSlot, onDelete)
            }
            if (daySessions.isNotEmpty()) {
                item(key = "planning-day-sessions-h") {
                    LZSectionHeader(
                        title = "Séances datées • ${daySessions.size}",
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                }
                adminSessionRows(daySessions, teacherById, "pd-$dayIndex", onEditSession, onDelete)
            }
        }

        item(key = "planning-day-add-session") {
            Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                LZSecondaryButton(
                    text = "Créer une séance",
                    onClick = onAddSession,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Rounded.Add,
                )
            }
        }
    } else {
        val days = (0..6).filter { slotsOf(it).isNotEmpty() || sessionsOf(it).isNotEmpty() }
        if (days.isEmpty()) {
            item(key = "planning-week-empty") {
                LZEmptyState(
                    title = "Semaine libre",
                    message = "Aucun créneau ni séance cette semaine pour ce filtre.",
                    icon = Icons.Rounded.CalendarMonth,
                    modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 8.dp),
                )
            }
        } else {
            days.forEach { index ->
                val date = weekStart.plusDays(index.toLong())
                item(key = "planning-week-h-$index") {
                    LZSectionHeader(
                        title = "${FrenchDates.dayLong[index]} ${date.dayOfMonth}",
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                }
                val daySlots = slotsOf(index)
                val daySessions = sessionsOf(index)
                if (daySlots.isNotEmpty()) {
                    adminSlotRows(daySlots, groupById, teacherById, "ws-$index", onEditSlot, onDelete)
                }
                if (daySessions.isNotEmpty()) {
                    adminSessionRows(daySessions, teacherById, "wd-$index", onEditSession, onDelete)
                }
            }
        }
    }
}

/** Timeline row of one weekly slot (recurring schedule). */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.adminSlotRows(
    slots: List<RemoteSlot>,
    groupById: Map<String, RemoteGroup>,
    teacherById: Map<String, RemoteTeacher>,
    keyPrefix: String,
    onEdit: (RemoteSlot) -> Unit,
    onDelete: (String) -> Unit,
) {
    items(slots, key = { "$keyPrefix-${it.remoteId}" }) { slot ->
        val group = groupById[slot.groupId]
        val teacher = group?.teacherId?.let { teacherById[it] }
        val label = "${slotDayLabel(slot.weekday)} ${slotTime(slot.start)} • ${slot.groupName ?: "Groupe"}"
        AdminTimelineRow(
            start = slotTime(slot.start),
            end = slotTime(slot.end),
            accent = LZ.colors.primary,
            title = slot.groupName ?: "Créneau",
            subtitle = listOfNotNull(
                slot.roomName ?: "Salle non définie",
                teacher?.fullName ?: "Enseignant non défini",
            ).joinToString(" • "),
            avatarInitials = null,
            avatarUrl = null,
            statusLabel = null,
            statusColor = LZ.colors.textSecondary,
            statusBackground = LZ.colors.surfaceSoft,
            onEdit = { onEdit(slot) },
            onDelete = { onDelete(deleteKey(label, "slot:${slot.remoteId}")) },
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
    }
}

/** Timeline row of one dated session, with the photo of the teacher who runs it. */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.adminSessionRows(
    sessions: List<RemoteSession>,
    teacherById: Map<String, RemoteTeacher>,
    keyPrefix: String,
    onEdit: (RemoteSession) -> Unit,
    onDelete: (String) -> Unit,
) {
    items(sessions, key = { "$keyPrefix-${it.remoteId}" }) { session ->
        val teacher = session.teacherId?.let { teacherById[it] }
        val teacherName = teacher?.fullName ?: session.teacherName
        val label = "${sessionDayLabel(session.startsAtIso)} • ${session.groupName ?: "Séance"}"
        val statusColor = when (session.status) {
            "completed" -> LZ.colors.textSecondary
            "cancelled" -> LZ.colors.error
            else -> LZ.colors.success
        }
        val statusBackground = when (session.status) {
            "completed" -> LZ.colors.surfaceSoft
            "cancelled" -> LZ.colors.errorSoft
            else -> LZ.colors.successSoft
        }
        AdminTimelineRow(
            start = sessionTimeLabel(session.startsAtIso),
            end = sessionTimeLabel(session.endsAtIso),
            accent = if (session.status == "cancelled") LZ.colors.error else LZ.colors.success,
            title = session.subjectName ?: session.groupName ?: "Séance",
            subtitle = listOfNotNull(
                session.groupName,
                session.roomName ?: "Sans salle",
                teacherName ?: "Sans enseignant",
            ).joinToString(" • "),
            avatarInitials = teacherName?.let { initialsOf(it) },
            avatarUrl = teacher?.photoUrl?.let { SupabaseApi().avatarUrl(it) },
            statusLabel = sessionStatusLabel(session.status),
            statusColor = statusColor,
            statusBackground = statusBackground,
            onEdit = { onEdit(session) },
            onDelete = { onDelete(deleteKey(label, "session:${session.remoteId}")) },
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
    }
}

/**
 * One timeline card: hour on the left, dot + line, then the card. Edit/Delete are hidden
 * until a long press opens the context menu — the list therefore stays clean.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdminTimelineRow(
    start: String,
    end: String,
    accent: Color,
    title: String,
    subtitle: String,
    avatarInitials: String?,
    avatarUrl: String?,
    statusLabel: String?,
    statusColor: Color,
    statusBackground: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.width(46.dp),
            horizontalAlignment = Alignment.End,
        ) {
            LZText(start, style = LZ.type.cardTitle)
            LZText(end, style = LZ.type.secondary, color = LZ.colors.textSecondary)
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
                    .background(accent),
            )
            Box(
                Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(LZ.colors.surfaceSoft),
            )
        }
        Box(Modifier.weight(1f).padding(bottom = 6.dp)) {
            LZCard(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { menuOpen = true },
                    ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (avatarInitials != null) {
                        RemoteCardAvatar(initials = avatarInitials, url = avatarUrl, size = 40.dp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LZText(title, style = LZ.type.bodyStrong, maxLines = 1)
                        LZText(
                            subtitle,
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                            maxLines = 2,
                        )
                    }
                    if (statusLabel != null) {
                        LZStatusPill(statusLabel, statusColor, statusBackground)
                    }
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { LZText("Modifier", style = LZ.type.bodyStrong) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Edit, null, tint = LZ.colors.textSecondary)
                    },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { LZText("Supprimer", style = LZ.type.bodyStrong, color = LZ.colors.error) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Delete, null, tint = LZ.colors.error)
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun ManageSlotForm(
    mode: String,
    slot: RemoteSlot?,
    data: AdminManageViewModel.ManageData?,
    onSave: (groupId: String, weekday: Int, start: String, end: String, roomId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val groups = data?.groups.orEmpty()
    val rooms = data?.rooms.orEmpty()
    var groupName by remember { mutableStateOf(slot?.groupName.orEmpty()) }
    var weekdayLabel by remember {
        mutableStateOf(
            slot?.let { planningDayLabels.getOrElse(it.weekday - 1) { "Lundi" } }
                ?: planningDayLabels.first(),
        )
    }
    var start by remember { mutableStateOf(slot?.start?.take(5) ?: "16:00") }
    var end by remember { mutableStateOf(slot?.end?.take(5) ?: "18:00") }
    var roomName by remember { mutableStateOf(slot?.roomName.orEmpty()) }
    var error by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        LZText(if (mode == "add") "Ajouter un créneau" else "Modifier le créneau", style = LZ.type.pageTitle)
        LZText(
            "Le créneau se répète chaque semaine dans le planning des élèves.",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        LZDropdownField(
            options = groups.map { it.name },
            selected = groupName,
            onSelected = { picked ->
                groupName = picked
                if (roomName.isBlank()) {
                    groups.firstOrNull { it.name == picked }?.roomName?.let { roomName = it }
                }
            },
            placeholder = "Groupe *",
        )
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = planningDayLabels,
            selected = weekdayLabel,
            onSelected = { weekdayLabel = it },
            placeholder = "Jour de la semaine",
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LZTextInput(
                value = start,
                onValueChange = { start = it },
                placeholder = "Début (HH:mm)",
                modifier = Modifier.weight(1f),
            )
            LZTextInput(
                value = end,
                onValueChange = { end = it },
                placeholder = "Fin (HH:mm)",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = listOf("(aucune salle)") + rooms.map { it.name },
            selected = roomName.ifBlank { "(aucune salle)" },
            onSelected = { roomName = if (it == "(aucune salle)") "" else it },
            placeholder = "Salle",
        )
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(14.dp))
        LZPrimaryButton(
            text = if (mode == "add") "Ajouter au planning" else "Enregistrer",
            onClick = {
                val groupId = groups.firstOrNull { it.name == groupName }?.remoteId
                val weekday = planningDayLabels.indexOf(weekdayLabel) + 1
                when {
                    groupId == null -> error = "Choisissez un groupe."
                    !isoTimePattern.matches(start.trim()) || !isoTimePattern.matches(end.trim()) ->
                        error = "Heures au format HH:mm (ex. 16:00)."
                    end.trim() <= start.trim() -> error = "L'heure de fin doit suivre l'heure de début."
                    else -> onSave(
                        groupId,
                        weekday,
                        start.trim(),
                        end.trim(),
                        rooms.firstOrNull { it.name == roomName }?.remoteId,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ManageSessionForm(
    mode: String,
    session: RemoteSession?,
    data: AdminManageViewModel.ManageData?,
    onSave: (
        groupId: String,
        teacherId: String?,
        subjectId: String?,
        roomId: String?,
        dateIso: String,
        start: String,
        end: String,
        topic: String?,
        status: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val groups = data?.groups.orEmpty()
    val teachers = data?.teachers.orEmpty()
    val subjects = data?.subjects.orEmpty()
    val rooms = data?.rooms.orEmpty()

    val existingStart = remember(session) {
        session?.startsAtIso?.let {
            runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull()
        }
    }
    val existingEnd = remember(session) {
        session?.endsAtIso?.let {
            runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull()
        }
    }

    var groupName by remember { mutableStateOf(session?.groupName.orEmpty()) }
    var teacherName by remember { mutableStateOf(session?.teacherName.orEmpty()) }
    var subjectName by remember { mutableStateOf(session?.subjectName.orEmpty()) }
    var roomName by remember { mutableStateOf(session?.roomName.orEmpty()) }
    var date by remember { mutableStateOf(existingStart?.toLocalDate()?.toString() ?: LocalDate.now().toString()) }
    var start by remember { mutableStateOf(existingStart?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "16:00") }
    var end by remember { mutableStateOf(existingEnd?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "18:00") }
    var topic by remember { mutableStateOf(session?.topic.orEmpty()) }
    var statusLabel by remember { mutableStateOf(sessionStatusLabel(session?.status ?: "scheduled")) }
    var error by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth()) {
        LZText(if (mode == "add") "Créer une séance" else "Modifier la séance", style = LZ.type.pageTitle)
        LZText(
            "Séance datée : elle apparaît dans le planning de l'enseignant et comme prochain cours de l'élève.",
            style = LZ.type.secondary,
            color = LZ.colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        LZDropdownField(
            options = groups.map { it.name },
            selected = groupName,
            onSelected = { picked ->
                groupName = picked
                groups.firstOrNull { it.name == picked }?.let { group ->
                    if (teacherName.isBlank()) group.teacherName?.let { teacherName = it }
                    if (roomName.isBlank()) group.roomName?.let { roomName = it }
                }
            },
            placeholder = "Groupe *",
        )
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = listOf("(aucun enseignant)") + teachers.map { it.fullName },
            selected = teacherName.ifBlank { "(aucun enseignant)" },
            onSelected = { teacherName = if (it == "(aucun enseignant)") "" else it },
            placeholder = "Enseignant",
        )
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = listOf("(aucune matière)") + subjects.map { it.name },
            selected = subjectName.ifBlank { "(aucune matière)" },
            onSelected = { subjectName = if (it == "(aucune matière)") "" else it },
            placeholder = "Matière",
        )
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = listOf("(aucune salle)") + rooms.map { it.name },
            selected = roomName.ifBlank { "(aucune salle)" },
            onSelected = { roomName = if (it == "(aucune salle)") "" else it },
            placeholder = "Salle",
        )
        Spacer(Modifier.height(8.dp))
        LZTextInput(
            value = date,
            onValueChange = { date = it },
            placeholder = "Date (AAAA-MM-JJ) *",
            leadingIcon = Icons.Rounded.CalendarMonth,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LZTextInput(
                value = start,
                onValueChange = { start = it },
                placeholder = "Début (HH:mm)",
                modifier = Modifier.weight(1f),
            )
            LZTextInput(
                value = end,
                onValueChange = { end = it },
                placeholder = "Fin (HH:mm)",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        LZTextInput(
            value = topic,
            onValueChange = { topic = it },
            placeholder = "Sujet / thème (optionnel)",
        )
        Spacer(Modifier.height(8.dp))
        LZDropdownField(
            options = sessionStatusLabels,
            selected = statusLabel,
            onSelected = { statusLabel = it },
            placeholder = "Statut",
        )
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            LZText(error, style = LZ.type.labelSmall, color = LZ.colors.error)
        }
        Spacer(Modifier.height(14.dp))
        LZPrimaryButton(
            text = if (mode == "add") "Créer la séance" else "Enregistrer",
            onClick = {
                val groupId = groups.firstOrNull { it.name == groupName }?.remoteId
                val validDate = isoDatePattern.matches(date.trim()) &&
                    runCatching { LocalDate.parse(date.trim()) }.isSuccess
                when {
                    groupId == null -> error = "Choisissez un groupe."
                    !validDate -> error = "Date au format AAAA-MM-JJ."
                    !isoTimePattern.matches(start.trim()) || !isoTimePattern.matches(end.trim()) ->
                        error = "Heures au format HH:mm (ex. 16:00)."
                    end.trim() <= start.trim() -> error = "L'heure de fin doit suivre l'heure de début."
                    else -> onSave(
                        groupId,
                        teachers.firstOrNull { it.fullName == teacherName }?.remoteId,
                        subjects.firstOrNull { it.name == subjectName }?.remoteId,
                        rooms.firstOrNull { it.name == roomName }?.remoteId,
                        date.trim(),
                        start.trim(),
                        end.trim(),
                        topic.trim().ifBlank { null },
                        sessionStatusValue(statusLabel),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LZSecondaryButton("Annuler", onDismiss, Modifier.fillMaxWidth())
    }
}


