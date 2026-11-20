package com.lzacademy.app.navigation

import androidx.navigation.NavHostController
import com.lzacademy.app.models.UserRole

/** All navigation destinations of the app. Delivery 1 ships the three role home screens. */
object LZRoutes {
    // Student
    const val StudentHome = "student/home"
    const val StudentPlanning = "student/planning"
    const val StudentCourses = "student/courses"
    const val StudentNotifications = "student/notifications"
    const val StudentProfile = "student/profile"

    // Student → planning PDF viewer (level argument: BAC, 2AS, 1AS, BEM)
    const val StudentPlanningPdfPattern = "student/planning/pdf/{levelId}"
    const val LevelIdArg = "levelId"

    fun studentPlanningPdf(levelId: String): String = "student/planning/pdf/$levelId"

    // Student → teacher showcase
    const val StudentTeacherDirectory = "student/teachers"
    const val StudentTeacherProfilePattern = "student/teachers/{teacherId}"
    const val StudentTeacherContactPattern = "student/teachers/{teacherId}/contact"

    fun studentTeacherProfile(teacherId: String): String = "student/teachers/$teacherId"

    fun studentTeacherContact(teacherId: String): String = "student/teachers/$teacherId/contact"

    /** Navigation argument shared by the profile and contact destinations. */
    const val TeacherIdArg = "teacherId"

    // Teacher
    const val TeacherHome = "teacher/home"
    const val TeacherGroups = "teacher/groups"
    const val TeacherPlanning = "teacher/planning"
    const val TeacherResources = "teacher/resources"
    const val TeacherProfile = "teacher/profile"
    const val TeacherAttendance = "teacher/attendance"

    /** Roll-call of ONE group: the roster is filtered on this group's enrollments. */
    fun teacherAttendance(groupId: String) = "$TeacherAttendance/$groupId"

    // Administration
    const val AdminDashboard = "admin/home"
    const val AdminManagement = "admin/management"
    const val AdminCommunication = "admin/communication"
    const val AdminReports = "admin/reports"
    const val AdminMore = "admin/more"

    const val AdminPacks = "admin/packs"
    const val AdminProfile = "admin/profile"
    const val AdminSubscriptions = "admin/subscriptions"

    // Shared by every role
    const val Settings = "settings"

    fun homeFor(role: UserRole): String = when (role) {
        UserRole.Student -> StudentHome
        UserRole.Teacher -> TeacherHome
        UserRole.Admin -> AdminDashboard
    }
}

/** Switches between the tabs of a role, keeping each tab's state. */
fun NavHostController.navigateToTab(route: String) {
    val role = UserRole.fromRoute(route) ?: return
    navigate(route) {
        popUpTo(LZRoutes.homeFor(role)) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
