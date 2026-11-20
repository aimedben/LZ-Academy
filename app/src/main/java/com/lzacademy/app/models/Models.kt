package com.lzacademy.app.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class UserRole(val label: String, val prefix: String) {
    Student("Élève", "student"),
    Teacher("Enseignant", "teacher"),
    Admin("Administrateur", "admin");

    companion object {
        fun fromRoute(route: String?): UserRole? =
            entries.firstOrNull { route?.startsWith(it.prefix + "/") == true }
    }
}

/** Colour family of a subject / lesson. Mapped to concrete colours by the UI layer. */
enum class LessonAccent { Green, Blue, Violet, Orange }

enum class FileKind { Pdf, Doc, Slides, Image }

enum class AttendanceStatus(val label: String) {
    Present("Présent"),
    Absent("Absent"),
    Late("Retard"),
    Justified("Absence justifiée"),
}

enum class PaymentStatus(val label: String) {
    Paid("Payé"),
    Pending("En attente"),
    Overdue("En retard"),
}

enum class TimelineState { Done, Current, Upcoming }

// ---------------------------------------------------------------- shared

data class LessonUi(
    val id: String,
    val subject: String,
    val start: String,
    val end: String,
    val room: String,
    val person: String,
    val accent: LessonAccent,
    val done: Boolean = false,
) {
    val timeRange: String get() = "$start – $end"
}

data class FileUi(
    val id: String,
    val name: String,
    val meta: String,
    val kind: FileKind,
)

data class AnnouncementUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val body: String,
    val icon: ImageVector,
    val accent: LessonAccent,
    val unread: Boolean,
)

data class StatUi(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val accent: LessonAccent,
)

data class ChartSeriesUi(
    val values: List<Float>,
    val labels: List<String>,
)

data class DonutSegmentUi(
    val label: String,
    val value: Float,
    val color: Color,
)

// ---------------------------------------------------------------- student

data class WeekDayUi(val index: Int, val label: String, val number: Int)

data class NextLessonUi(
    val subject: String,
    val teacher: String,
    val whenLabel: String,
    val room: String,
)

data class ScheduleChangeUi(val title: String, val subtitle: String, val detail: String)

data class StudentDashboardData(
    val firstName: String,
    val initials: String,
    /** Public storage URL of the student's profile photo, or null (initials fallback). */
    val avatarUrl: String? = null,
    val unreadNotifications: Int,
    val remainingSessions: Int,
    val totalSessions: Int,
    val activeSubjects: Int,
    /** Next real lesson, or null when the student has no upcoming session. */
    val nextLesson: NextLessonUi?,
    val todayIndex: Int,
    val week: List<WeekDayUi>,
    val lessonsByDay: Map<Int, List<LessonUi>>,
    val scheduleChange: ScheduleChangeUi?,
    val materials: List<FileUi>,
    val announcements: List<AnnouncementUi>,
)

// ---------------------------------------------------------------- teacher

data class SessionUi(
    val title: String,
    val subject: String,
    val timeRange: String,
    val room: String,
    val studentCount: Int,
)

data class GroupAttendanceUi(val group: String, val studentCount: Int, val rate: Float)

data class TimelineLessonUi(
    val id: String,
    val start: String,
    val title: String,
    val subtitle: String,
    val state: TimelineState,
    val accent: LessonAccent,
)

data class TeacherDashboardData(
    val name: String,
    val initials: String,
    /** Public storage URL of the teacher's profile photo, or null (initials fallback). */
    val avatarUrl: String? = null,
    val stats: List<StatUi>,
    /** Next real session, or null when none is scheduled. */
    val nextSession: SessionUi?,
    val today: List<TimelineLessonUi>,
    /** Real sessions of the week by ISO weekday (1 = Monday … 7 = Sunday). */
    val sessionsByDay: Map<Int, List<TimelineLessonUi>>,
    val groupAttendance: List<GroupAttendanceUi>,
    val weeklyHours: ChartSeriesUi,
    val publications: List<FileUi>,
)

// ---------------------------------------------------------------- admin

data class HeaderStatUi(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val container: Color,
    val accent: Color,
)

data class RevenueUi(
    val amount: String,
    val delta: String,
    val caption: String,
    val trend: ChartSeriesUi,
)

data class AlertUi(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val accent: LessonAccent,
    val dialogTitle: String,
    val dialogMessage: String,
)

data class PaymentUi(
    val id: String,
    val student: String,
    val title: String,
    val date: String,
    val amount: String,
    val status: PaymentStatus,
)

data class QuickActionUi(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val accent: LessonAccent,
)

data class AdminDashboardData(
    val adminName: String,
    val headerStats: List<HeaderStatUi>,
    val quickActions: List<QuickActionUi>,
    val revenue: RevenueUi,
    val kpis: List<StatUi>,
    val alerts: List<AlertUi>,
    val periods: List<Int>,
    val enrollment: Map<Int, ChartSeriesUi>,
    val subjectDistribution: List<DonutSegmentUi>,
    val recentPayments: List<PaymentUi>,
)
