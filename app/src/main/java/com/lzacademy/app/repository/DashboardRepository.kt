package com.lzacademy.app.repository

import androidx.room.withTransaction
import com.lzacademy.app.data.FrenchDates
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.supabase.SyncedAttendanceEntity
import com.lzacademy.app.data.supabase.SyncedEnrollmentEntity
import com.lzacademy.app.data.supabase.SyncedGroupEntity
import com.lzacademy.app.data.supabase.SyncedMaterialEntity
import com.lzacademy.app.data.supabase.SyncedPaymentEntity
import com.lzacademy.app.data.supabase.SyncedSessionEntity
import com.lzacademy.app.data.supabase.SyncedSlotEntity
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.data.supabase.SyncedSubjectEntity
import com.lzacademy.app.data.supabase.SyncedSubscriptionEntity
import com.lzacademy.app.data.supabase.SyncedTeacherEntity
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.AdminDashboardData
import com.lzacademy.app.models.AlertUi
import com.lzacademy.app.models.AnnouncementUi
import com.lzacademy.app.models.ChartSeriesUi
import com.lzacademy.app.models.DonutSegmentUi
import com.lzacademy.app.models.FileKind
import com.lzacademy.app.models.FileUi
import com.lzacademy.app.models.HeaderStatUi
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.models.LessonUi
import com.lzacademy.app.models.NextLessonUi
import com.lzacademy.app.models.PaymentStatus
import com.lzacademy.app.models.PaymentUi
import com.lzacademy.app.models.QuickActionUi
import com.lzacademy.app.models.RevenueUi
import com.lzacademy.app.models.ScheduleChangeUi
import com.lzacademy.app.models.SessionUi
import com.lzacademy.app.models.StatUi
import com.lzacademy.app.models.StudentDashboardData
import com.lzacademy.app.models.TeacherDashboardData
import com.lzacademy.app.models.TimelineLessonUi
import com.lzacademy.app.models.TimelineState
import com.lzacademy.app.models.WeekDayUi
import com.lzacademy.app.theme.LZPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Timer

/**
 * Source of dashboard data.
 */
interface DashboardRepository {
    suspend fun studentDashboard(): StudentDashboardData
    suspend fun teacherDashboard(): TeacherDashboardData
    suspend fun adminDashboard(): AdminDashboardData
}

/**
 * Real dashboards, computed from the Room mirror of the Supabase database ONLY.
 * When the mirror is empty (fresh install before the first sync) every screen shows
 * an honest empty state — no value is ever invented.
 */
class SupabaseDashboardRepository(
    private val database: LZDatabase,
    private val preferences: AppPreferences,
) : DashboardRepository {

    private val accents = listOf(LessonAccent.Green, LessonAccent.Blue, LessonAccent.Violet, LessonAccent.Orange)
    private val announcementIcons = listOf(Icons.Rounded.Campaign, Icons.Rounded.Event, Icons.Rounded.Description, Icons.Rounded.Payments)
    private val donutColors = listOf(LZPalette.PrimaryGreen, LZPalette.Gold, LZPalette.Violet, LZPalette.Turquoise, LZPalette.Orange)

    // ---------------------------------------------------------------- student

    override suspend fun studentDashboard(): StudentDashboardData {
        val dao = database.syncDao()
        val students = dao.observeStudents().first()
        val own = preferences.ownMatricule?.let { dao.studentByMatricule(it) }
        val subscription = own?.let { dao.subscriptionByMatricule(it.matricule) }
        val numbers = DashboardComputer.student(
            own = own,
            subscription = subscription,
            subjects = dao.observeSubjects().first(),
            materials = dao.observeMaterials().first(),
            teachers = dao.observeTeachers().first(),
            slots = dao.observeSlots().first(),
            sessions = dao.observeSessions().first(),
            enrollments = dao.observeEnrollments().first(),
            now = java.time.ZonedDateTime.now(),
        )

        val today = LocalDate.now()
        val rawIndex = today.dayOfWeek.value - 1 // 0 = Monday
        val todayIndex = rawIndex.coerceAtMost(5)
        val nowText = "%02d:%02d".format(LocalDateTime.now().hour, LocalDateTime.now().minute)
        val weekStart = today.minusDays(rawIndex.toLong())

        val lessonsByDay = numbers.lessonsByDay
            .mapKeys { (weekday, _) -> weekday - 1 } // ISO 1..7 → 0..6
            .filterKeys { it <= 5 }
            .mapValues { (dayIndex, lessons) ->
                lessons.mapIndexed { i, lesson ->
                    LessonUi(
                        id = "real-$dayIndex-${lesson.start}-$i",
                        subject = lesson.subject,
                        start = lesson.start,
                        end = lesson.end,
                        room = lesson.room,
                        person = lesson.person,
                        accent = accents[i % accents.size],
                        done = dayIndex < rawIndex || (dayIndex == rawIndex && lesson.end < nowText),
                    )
                }
            }

        return StudentDashboardData(
            firstName = numbers.firstName,
            initials = numbers.initials,
            avatarUrl = own?.avatarPath
                ?.takeIf { it.isNotBlank() && it != "null" }
                ?.let { com.lzacademy.app.data.supabase.SupabaseApi().avatarUrl(it) },
            unreadNotifications = 0,
            remainingSessions = numbers.remaining,
            totalSessions = numbers.total,
            activeSubjects = numbers.activeSubjects,
            nextLesson = numbers.nextLesson?.let {
                NextLessonUi(subject = it.subject, teacher = it.teacher, whenLabel = it.whenLabel, room = it.room)
            },
            todayIndex = todayIndex,
            week = (0..5).map { i ->
                WeekDayUi(i, FrenchDates.dayShort[i], weekStart.plusDays(i.toLong()).dayOfMonth)
            },
            lessonsByDay = lessonsByDay,
            scheduleChange = null,
            materials = dao.observeMaterials().first().filter { it.isPublished }.map { it.toFileUi() },
            announcements = dao.observeAnnouncements().first()
                .filter { it.audience == "all" || it.audience == "students" }
                .filter { !isExpired(it.expiresAtIso) }
                .mapIndexed { i, a ->
                    AnnouncementUi(
                        id = a.remoteId,
                        title = a.title,
                        subtitle = relativeDate(parseLocal(a.publishedAtIso ?: a.createdAtIso)),
                        body = a.body,
                        icon = announcementIcons[i % announcementIcons.size],
                        accent = accents[i % accents.size],
                        unread = false,
                    )
                },
        )
    }

    // ---------------------------------------------------------------- teacher

    override suspend fun teacherDashboard(): TeacherDashboardData {
        val dao = database.syncDao()
        val teachers = dao.observeTeachers().first()
        val teacher = preferences.ownProfileId?.let { pid -> teachers.firstOrNull { it.profileId == pid } }
        val groups = dao.observeGroups().first()
        val numbers = DashboardComputer.teacher(
            teacher = teacher,
            groups = groups,
            sessions = dao.observeSessions().first(),
            enrollments = dao.observeEnrollments().first(),
            attendance = dao.observeAttendance().first(),
            now = java.time.ZonedDateTime.now(),
        )
        val myGroupNames = groups.filter { g -> teacher != null && g.teacherId == teacher.remoteId }
            .map { it.name }
            .toSet()

        val hoursWeek = numbers.weeklyHours.sum()
        val stats = listOf(
            StatUi(Icons.Rounded.Groups, "${numbers.students}", "Élèves suivis", LessonAccent.Green),
            StatUi(Icons.Rounded.Class, "${numbers.groups}", "Groupes actifs", LessonAccent.Violet),
            StatUi(Icons.Rounded.Event, "${numbers.sessionsToday}", "Cours aujourd'hui", LessonAccent.Blue),
            StatUi(Icons.Rounded.Timer, "${hoursWeek.toInt()} h", "Heures cette semaine", LessonAccent.Orange),
        )

        return TeacherDashboardData(
            name = numbers.name,
            initials = numbers.initials,
            avatarUrl = teacher?.photoUrl?.let { com.lzacademy.app.data.supabase.SupabaseApi().avatarUrl(it) },
            stats = stats,
            nextSession = numbers.nextSession?.let {
                SessionUi(
                    title = it.title,
                    subject = it.subject,
                    timeRange = it.timeRange,
                    room = it.room,
                    studentCount = it.studentCount,
                )
            },
            today = numbers.todaySessions.map {
                TimelineLessonUi(
                    id = it.id,
                    start = it.start,
                    title = it.title,
                    subtitle = it.subtitle,
                    state = it.state,
                    accent = it.accent,
                )
            },
            sessionsByDay = numbers.sessionsByWeekDay.mapValues { (_, sessions) ->
                sessions.map {
                    TimelineLessonUi(
                        id = it.id,
                        start = it.start,
                        title = it.title,
                        subtitle = it.subtitle,
                        state = it.state,
                        accent = it.accent,
                    )
                }
            },
            // too few real attendance rows today: the section shows an honest empty state
            groupAttendance = emptyList(),
            weeklyHours = ChartSeriesUi(values = numbers.weeklyHours, labels = FrenchDates.dayShort),
            publications = dao.observeMaterials().first()
                .filter { it.groupName in myGroupNames }
                .map { it.toFileUi() },
        )
    }

    // ---------------------------------------------------------------- admin

    override suspend fun adminDashboard(): AdminDashboardData {
        val dao = database.syncDao()
        val students = dao.observeStudents().first()
        val subscriptions = dao.observeSubscriptions().first()
        val payments = dao.observePayments().first()
        val sessions = dao.observeSessions().first()
        val numbers = DashboardComputer.admin(
            students = students,
            subscriptions = subscriptions,
            teachers = dao.observeTeachers().first(),
            groups = dao.observeGroups().first(),
            payments = payments,
            sessions = sessions,
            enrollments = dao.observeEnrollments().first(),
            today = LocalDate.now(),
        )
        val today = LocalDate.now()
        val studentNames = students.associate { it.remoteId to it.fullName }

        val delta = when {
            numbers.prevMonthRevenue <= 0.0 && numbers.monthRevenue <= 0.0 -> "—"
            numbers.prevMonthRevenue <= 0.0 -> "Nouveau"
            else -> {
                val pct = (numbers.monthRevenue - numbers.prevMonthRevenue) / numbers.prevMonthRevenue * 100.0
                (if (pct >= 0) "+" else "") + "%.1f %%".format(pct)
            }
        }

        val alerts = buildList {
            if (numbers.exhaustedSubscriptions > 0) {
                add(
                    AlertUi(
                        "exhausted", Icons.Rounded.Autorenew,
                        "${numbers.exhaustedSubscriptions} abonnement(s) épuisé(s)",
                        "Élèves sans séance restante", "Relancer", LessonAccent.Orange,
                        dialogTitle = "Préparer un rappel ?",
                        dialogMessage = "Un message de rappel sera préparé pour les ${numbers.exhaustedSubscriptions} élève(s) dont l'abonnement est épuisé.",
                    ),
                )
            }
            if (numbers.pendingPayments > 0) {
                add(
                    AlertUi(
                        "pending", Icons.Rounded.Payments,
                        "${numbers.pendingPayments} paiement(s) en attente",
                        "Paiements à valider", "Voir", LessonAccent.Blue,
                        dialogTitle = "Paiements en attente",
                        dialogMessage = "${numbers.pendingPayments} paiement(s) attendent une validation.",
                    ),
                )
            }
        }

        val periods = listOf(3, 6, 12)
        val cumulative = numbers.monthlyNewStudents
        val enrollment = periods.associateWith { n ->
            ChartSeriesUi(
                values = cumulative.takeLast(n).map { it.toFloat() },
                labels = cumulative.takeLast(n).mapIndexed { k, _ ->
                    val month = today.minusMonths((n - 1 - k).toLong()).monthValue - 1
                    FrenchDates.monthShort[month]
                },
            )
        }

        return AdminDashboardData(
            adminName = "Admin",
            headerStats = listOf(
                HeaderStatUi(Icons.Rounded.Groups, "${numbers.activeStudents}", "Élèves", LZPalette.PastelMint, LZPalette.PrimaryGreen),
                HeaderStatUi(Icons.Rounded.School, "${numbers.teachers}", "Enseignants", LZPalette.PastelLavender, LZPalette.DeepPurple),
                HeaderStatUi(Icons.Rounded.Class, "${numbers.groups}", "Groupes", LZPalette.PastelPeach, LZPalette.Orange),
                HeaderStatUi(Icons.Rounded.Event, "${numbers.sessionsToday}", "Séances du jour", LZPalette.PastelRose, LZPalette.Violet),
            ),
            quickActions = listOf(
                QuickActionUi("add_student", Icons.Rounded.PersonAdd, "Nouvel élève", LessonAccent.Green),
                QuickActionUi("record_payment", Icons.Rounded.Payments, "Paiement", LessonAccent.Orange),
                QuickActionUi("send_sms", Icons.Rounded.Sms, "SMS", LessonAccent.Blue),
                QuickActionUi("reports", Icons.Rounded.Analytics, "Rapports", LessonAccent.Violet),
            ),
            revenue = RevenueUi(
                amount = "${money(numbers.monthRevenue)} DA",
                delta = delta,
                caption = "Revenu mensuel • ${FrenchDates.monthLong[today.monthValue - 1]} ${today.year}",
                trend = ChartSeriesUi(values = numbers.monthlyRevenue.map { it.toFloat() }, labels = emptyList()),
            ),
            kpis = listOf(
                StatUi(Icons.Rounded.Groups, "${numbers.activeStudents}", "Élèves actifs", LessonAccent.Green),
                StatUi(Icons.Rounded.MenuBook, "${numbers.groups}", "Groupes", LessonAccent.Violet),
                StatUi(Icons.Rounded.Event, "${numbers.sessionsToday}", "Séances aujourd'hui", LessonAccent.Blue),
                StatUi(Icons.Rounded.Payments, "${numbers.pendingPayments}", "Paiements en attente", LessonAccent.Orange),
            ),
            alerts = alerts,
            periods = periods,
            enrollment = enrollment,
            subjectDistribution = numbers.studentsByLevel.mapIndexed { i, (label, count) ->
                DonutSegmentUi(label, count.toFloat(), donutColors[i % donutColors.size])
            },
            recentPayments = payments.take(6).map { p ->
                PaymentUi(
                    id = p.remoteId,
                    student = studentNames[p.studentId] ?: "Élève",
                    title = "Abonnement",
                    date = relativeDate(parseLocal(p.createdAtIso)),
                    amount = "${money(p.amountDzd)} DA",
                    status = when (p.status) {
                        "paid" -> PaymentStatus.Paid
                        "pending" -> PaymentStatus.Pending
                        else -> PaymentStatus.Overdue
                    },
                )
            },
        )
    }

    // ---------------------------------------------------------------- helpers

    private fun SyncedMaterialEntity.toFileUi(): FileUi {
        val kind = when (kind.lowercase()) {
            "pdf" -> FileKind.Pdf
            "doc", "docx" -> FileKind.Doc
            "slides", "presentation" -> FileKind.Slides
            "image", "photo" -> FileKind.Image
            else -> FileKind.Pdf
        }
        val kindLabel = when (kind) {
            FileKind.Pdf -> "PDF"
            FileKind.Doc -> "Document"
            FileKind.Slides -> "Présentation"
            FileKind.Image -> "Image"
        }
        return FileUi(
            id = remoteId,
            name = title,
            meta = "$kindLabel • ${humanSize(sizeBytes)}",
            kind = kind,
        )
    }

    private fun humanSize(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f Mo".format(bytes / 1_000_000.0).replace(".", ",")
        bytes >= 1_000 -> "${bytes / 1_000} Ko"
        bytes > 0 -> "$bytes o"
        else -> "—"
    }

    private fun money(amount: Double): String =
        NumberFormat.getIntegerInstance(Locale.FRANCE).format(amount.toLong())

    private fun isExpired(expiresAtIso: String?): Boolean {
        val expiry = parseInstant(expiresAtIso) ?: return false
        return expiry.isBefore(java.time.Instant.now())
    }

    private fun parseInstant(iso: String?): java.time.Instant? =
        DashboardComputer.parseIso(iso)?.toInstant()

    private fun parseLocal(iso: String?): LocalDateTime? =
        DashboardComputer.parseIso(iso)?.withZoneSameInstant(ZoneOffset.systemDefault())?.toLocalDateTime()

    private fun relativeDate(date: LocalDateTime?): String {
        if (date == null) return "—"
        val today = LocalDate.now()
        return when (date.toLocalDate()) {
            today -> "Aujourd'hui"
            today.minusDays(1) -> "Hier"
            else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH))
        }
    }
}
