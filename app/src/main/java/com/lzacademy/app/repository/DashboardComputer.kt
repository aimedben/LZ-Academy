package com.lzacademy.app.repository

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
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.models.TimelineState
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Pure, testable computations behind the real dashboards. Every function consumes ONLY the
 * Supabase mirror entities and returns plain numbers — no Compose, no Android, no demo data.
 * Mapping to UI models (icons, colours) happens in [SupabaseDashboardRepository].
 */
object DashboardComputer {

    // ---------------------------------------------------------------- shared

    private val ACCENTS = listOf(LessonAccent.Green, LessonAccent.Blue, LessonAccent.Violet, LessonAccent.Orange)

    /** Parses PostgREST timestamps ("2026-11-09 15:00:00+00", ISO "…T…:00Z", "…+00:00"). */
    internal fun parseIso(iso: String?): ZonedDateTime? {
        if (iso.isNullOrBlank()) return null
        return runCatching {
            var text = iso.trim().replace(" ", "T")
            if (text.endsWith("Z")) text = text.dropLast(1) + "+00:00"
            if (Regex("\\+\\d\\d$").containsMatchIn(text)) text += ":00"
            OffsetDateTime.parse(text).toZonedDateTime()
        }.getOrNull()
    }

    private fun hhmm(t: ZonedDateTime): String =
        "%02d:%02d".format(t.hour, t.minute)

    /** "15:00:00" → "15:00" (schedule slots store time-without-time-zone). */
    private fun shortTime(raw: String): String =
        if (raw.length >= 5) raw.take(5) else raw

    // ---------------------------------------------------------------- admin

    data class PaymentRow(
        val id: String,
        val studentName: String?,
        val amountDzd: Double,
        val status: String,
        val createdAtIso: String,
        val reference: String?,
    )

    data class AdminNumbers(
        val activeStudents: Int,
        val teachers: Int,
        val groups: Int,
        val activeSubscriptions: Int,
        val exhaustedSubscriptions: Int,
        val pendingPayments: Int,
        val monthRevenue: Double,
        val prevMonthRevenue: Double,
        /** Paid sums of the last 6 months, oldest first. */
        val monthlyRevenue: List<Double>,
        /** Cumulative headcount of students created, evaluated at each of the last 6 months. */
        val monthlyNewStudents: List<Int>,
        /** Real students per level (null level → « Non affecté »), count descending. */
        val studentsByLevel: List<Pair<String, Int>>,
        val recentPayments: List<PaymentRow>,
        val sessionsToday: Int,
    )

    fun admin(
        students: List<SyncedStudentEntity>,
        subscriptions: List<SyncedSubscriptionEntity>,
        teachers: List<SyncedTeacherEntity>,
        groups: List<SyncedGroupEntity>,
        payments: List<SyncedPaymentEntity>,
        sessions: List<SyncedSessionEntity>,
        enrollments: List<SyncedEnrollmentEntity>,
        today: LocalDate,
    ): AdminNumbers {
        val months = (11 downTo 0).map { YearMonth.from(today.minusMonths(it.toLong())) }

        val monthlyRevenue = months.map { month ->
            payments.filter { p ->
                p.status == "paid" && monthOf(p.paidAtIso ?: p.createdAtIso) == month
            }.sumOf { it.amountDzd }
        }
        val monthlyNewStudents = months.map { month ->
            students.count { s ->
                val created = parseIso(s.createdAt)?.toInstant() ?: return@count true
                !YearMonth.from(created.atZone(ZoneOffset.UTC)).isAfter(month)
            }
        }
        val byLevel = students.groupingBy { it.levelName ?: "Non affecté" }
            .eachCount()
            .map { it.key to it.value }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

        val names = students.associate { it.remoteId to it.fullName }
        val recentPayments = payments
            .sortedByDescending { it.createdAtIso }
            .take(5)
            .map {
                PaymentRow(it.remoteId, names[it.studentId], it.amountDzd, it.status, it.createdAtIso, it.reference)
            }

        return AdminNumbers(
            activeStudents = students.count { it.isActive },
            teachers = teachers.count { it.isActive },
            groups = groups.size,
            activeSubscriptions = subscriptions.count { it.status == "active" },
            exhaustedSubscriptions = subscriptions.count { it.isExhausted },
            pendingPayments = payments.count { it.status == "pending" },
            monthRevenue = monthlyRevenue.last(),
            prevMonthRevenue = monthlyRevenue.dropLast(1).lastOrNull() ?: 0.0,
            monthlyRevenue = monthlyRevenue,
            monthlyNewStudents = monthlyNewStudents,
            studentsByLevel = byLevel,
            recentPayments = recentPayments,
            sessionsToday = sessions.count { s ->
                parseIso(s.startsAtIso)?.withZoneSameInstant(ZoneOffset.UTC)?.toLocalDate() == today
            },
        )
    }

    private fun monthOf(iso: String?): YearMonth? = parseIso(iso)?.let { YearMonth.from(it) }

    // ---------------------------------------------------------------- student

    data class NextLesson(val subject: String, val teacher: String, val whenLabel: String, val room: String)

    data class Lesson(val subject: String, val start: String, val end: String, val room: String, val person: String)

    data class StudentNumbers(
        val firstName: String,
        val initials: String,
        val remaining: Int,
        val total: Int,
        val activeSubjects: Int,
        val nextLesson: NextLesson?,
        val lessonsByDay: Map<Int, List<Lesson>>,
    )

    fun student(
        own: SyncedStudentEntity?,
        subscription: SyncedSubscriptionEntity?,
        subjects: List<SyncedSubjectEntity>,
        materials: List<SyncedMaterialEntity>,
        teachers: List<SyncedTeacherEntity>,
        slots: List<SyncedSlotEntity>,
        sessions: List<SyncedSessionEntity>,
        enrollments: List<SyncedEnrollmentEntity>,
        now: ZonedDateTime,
    ): StudentNumbers {
        val firstName = own?.fullName?.trim()?.split(Regex("\\s+"))?.firstOrNull() ?: "Élève"
        val initials = initialsOf(own?.fullName ?: "")
        val groupIds = enrollments
            .filter { it.studentId == own?.remoteId && it.leftOn == null }
            .map { it.groupId }
            .toSet()
        val mySessions = sessions.filter { it.groupId in groupIds }

        val nextLesson = mySessions
            .filter { it.status == "scheduled" }
            .mapNotNull { s -> parseIso(s.startsAtIso)?.let { s to it } }
            .filter { (_, start) -> start.isAfter(now) }
            .minByOrNull { (_, start) -> start }
            ?.let { (s, start) ->
                NextLesson(
                    subject = s.subjectName ?: "Séance",
                    teacher = s.teacherName ?: "—",
                    whenLabel = whenLabel(start, now),
                    room = s.roomName ?: "—",
                )
            }

        val lessonsByDay = slots
            .filter { it.groupId in groupIds }
            .groupBy { it.weekday }
            .mapValues { (_, daySlots) ->
                daySlots.sortedBy { it.start }.map { slot ->
                    Lesson(
                        subject = slot.groupName ?: "Séance",
                        start = shortTime(slot.start),
                        end = shortTime(slot.end),
                        room = slot.roomName ?: "—",
                        // the group's teacher, resolved from the sessions mirror
                        person = sessions.firstOrNull { it.groupId == slot.groupId }?.teacherName ?: "—",
                    )
                }
            }

        return StudentNumbers(
            firstName = firstName,
            initials = initials,
            remaining = subscription?.sessionsRemaining ?: 0,
            total = subscription?.sessionsTotal ?: 0,
            activeSubjects = materials.mapNotNull { it.subjectId }.distinct().size,
            nextLesson = nextLesson,
            lessonsByDay = lessonsByDay,
        )
    }

    private fun whenLabel(start: ZonedDateTime, now: ZonedDateTime): String {
        val date = start.withZoneSameInstant(now.zone).toLocalDate()
        val time = hhmm(start.withZoneSameInstant(now.zone))
        return when (date) {
            now.toLocalDate() -> "Aujourd'hui à $time"
            now.toLocalDate().plusDays(1) -> "Demain à $time"
            else -> "${date.format(DateTimeFormatter.ofPattern("d/M"))} à $time"
        }
    }

    // ---------------------------------------------------------------- teacher

    data class NextSession(
        val title: String,
        val subject: String,
        val timeRange: String,
        val room: String,
        val studentCount: Int,
    )

    data class TodaySession(
        val id: String,
        val start: String,
        val title: String,
        val subtitle: String,
        val state: TimelineState,
        val accent: LessonAccent,
    )

    data class TeacherNumbers(
        val name: String,
        val initials: String,
        val sessionsToday: Int,
        val groups: Int,
        val students: Int,
        val nextSession: NextSession?,
        val todaySessions: List<TodaySession>,
        /** Billable hours per weekday, Monday first. */
        val weeklyHours: List<Float>,
        /** Real sessions of the week by ISO weekday (1 = Monday … 7 = Sunday). */
        val sessionsByWeekDay: Map<Int, List<TodaySession>>,
    )

    fun teacher(
        teacher: SyncedTeacherEntity?,
        groups: List<SyncedGroupEntity>,
        sessions: List<SyncedSessionEntity>,
        enrollments: List<SyncedEnrollmentEntity>,
        attendance: List<SyncedAttendanceEntity>,
        now: ZonedDateTime,
    ): TeacherNumbers {
        val myGroups = groups.filter { teacher != null && it.teacherId == teacher.remoteId }
        val myGroupIds = myGroups.map { it.remoteId }.toSet()
        val mySessions = sessions.filter { teacher != null && it.teacherId == teacher.remoteId }
        val todayDate = now.toLocalDate()

        val dated = mySessions.mapNotNull { s ->
            parseIso(s.startsAtIso)?.let { start ->
                parseIso(s.endsAtIso)?.let { end -> Triple(s, start, end) }
            }
        }
        val todaySessions = dated.filter { (_, start, _) ->
            start.withZoneSameInstant(now.zone).toLocalDate() == todayDate
        }
        val next = dated
            .filter { (s, start, _) -> s.status == "scheduled" && start.isAfter(now) }
            .minByOrNull { (_, start, _) -> start }
            ?.let { (s, start, end) ->
                NextSession(
                    title = s.topic ?: s.subjectName ?: "Séance",
                    subject = s.subjectName ?: "—",
                    timeRange = "${hhmm(start.withZoneSameInstant(now.zone))} – " +
                        hhmm(end.withZoneSameInstant(now.zone)),
                    room = s.roomName ?: "—",
                    studentCount = enrollments.count { it.groupId == s.groupId && it.leftOn == null },
                )
            }

        val monday = todayDate.minusDays(((todayDate.dayOfWeek.value + 6) % 7).toLong())
        val weeklyHours = (0..6).map { i ->
            val day = monday.plusDays(i.toLong())
            dated.filter { (_, start, _) -> start.withZoneSameInstant(now.zone).toLocalDate() == day }
                .sumOf { (_, start, end) -> Duration.between(start, end).toMinutes() }
                .toFloat() / 60f
        }

        return TeacherNumbers(
            name = teacher?.fullName ?: "Enseignant",
            initials = initialsOf(teacher?.fullName ?: ""),
            sessionsToday = todaySessions.size,
            groups = myGroups.size,
            students = enrollments
                .filter { it.groupId in myGroupIds && it.leftOn == null }
                .map { it.studentId }
                .distinct()
                .size,
            nextSession = next,
            todaySessions = todaySessions
                .sortedBy { (_, start, _) -> start }
                .mapIndexed { i, (s, start, _) -> toTodaySession(s, start, i, now) },
            weeklyHours = weeklyHours,
            sessionsByWeekDay = dated
                .groupBy(
                    { (_, start, _) -> start.withZoneSameInstant(now.zone).dayOfWeek.value },
                    { (s, start, _) -> toTodaySession(s, start, 0, now) },
                )
                .mapValues { (_, sessions) -> sessions.sortedBy { it.start } },
        )
    }

    private fun toTodaySession(
        s: SyncedSessionEntity,
        start: ZonedDateTime,
        accentIndex: Int,
        now: ZonedDateTime,
    ) = TodaySession(
        id = s.remoteId,
        start = hhmm(start.withZoneSameInstant(now.zone)),
        title = s.topic ?: s.subjectName ?: "Séance",
        subtitle = s.groupName ?: "—",
        state = when {
            s.status == "completed" -> TimelineState.Done
            start.isAfter(now) -> TimelineState.Upcoming
            else -> TimelineState.Current
        },
        accent = ACCENTS[accentIndex % ACCENTS.size],
    )

    // ---------------------------------------------------------------- shared helpers

    internal fun initialsOf(fullName: String): String =
        fullName.trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "—" }
}
