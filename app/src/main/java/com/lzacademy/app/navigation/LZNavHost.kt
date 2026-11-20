package com.lzacademy.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lzacademy.app.models.AuthUser
import com.lzacademy.app.theme.ThemeMode
import com.lzacademy.app.ui.admin.AdminCommunicationScreen
import com.lzacademy.app.ui.admin.AdminDashboardRoute
import com.lzacademy.app.ui.admin.AdminManagementScreen
import com.lzacademy.app.ui.admin.AdminMoreScreen
import com.lzacademy.app.ui.admin.AdminReportsScreen
import com.lzacademy.app.ui.student.StudentCoursesScreen
import com.lzacademy.app.ui.student.StudentDashboardRoute
import com.lzacademy.app.ui.student.StudentNotificationsScreen
import com.lzacademy.app.ui.student.StudentPlanningScreen
import com.lzacademy.app.ui.student.StudentPlanningPdfScreen
import com.lzacademy.app.ui.student.StudentProfileScreen
import com.lzacademy.app.ui.student.StudentTeacherProfileRoute
import com.lzacademy.app.ui.student.TeacherContactRoute
import com.lzacademy.app.ui.student.TeacherDirectoryRoute
import com.lzacademy.app.ui.teacher.TeacherDashboardRoute
import com.lzacademy.app.ui.teacher.TeacherAttendanceScreen
import com.lzacademy.app.ui.teacher.TeacherGroupsScreen
import com.lzacademy.app.ui.teacher.TeacherPlanningScreen
import com.lzacademy.app.ui.teacher.TeacherProfileScreen
import com.lzacademy.app.ui.teacher.TeacherResourcesScreen

/** Root navigation graph: every tab of every role has a fully functional destination. */
@Composable
fun LZNavHost(
    navController: NavHostController,
    currentUser: AuthUser,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = LZRoutes.homeFor(currentUser.role),
) {
    val goTab: (String) -> Unit = { navController.navigateToTab(it) }
    val openSettings: () -> Unit = { navController.navigate(LZRoutes.Settings) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(240)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        // ==================== Student Routes ====================
        composable(LZRoutes.StudentHome) {
            StudentDashboardRoute(
                onNavigate = goTab,
                onShowMessage = onShowMessage,
                onAvatarClick = onAvatarClick,
                studentId = currentUser.linkedStudentId,
            )
        }
        composable(LZRoutes.StudentTeacherDirectory) {
            TeacherDirectoryRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenProfile = { navController.navigate(LZRoutes.studentTeacherProfile(it)) },
                onContact = { navController.navigate(LZRoutes.studentTeacherContact(it)) },
            )
        }
        composable(
            route = LZRoutes.StudentTeacherProfilePattern,
            arguments = listOf(navArgument(LZRoutes.TeacherIdArg) { type = NavType.StringType }),
        ) { entry ->
            StudentTeacherProfileRoute(
                teacherId = entry.arguments?.getString(LZRoutes.TeacherIdArg).orEmpty(),
                onNavigateBack = { navController.popBackStack() },
                onContact = { navController.navigate(LZRoutes.studentTeacherContact(it)) },
            )
        }
        composable(
            route = LZRoutes.StudentTeacherContactPattern,
            arguments = listOf(navArgument(LZRoutes.TeacherIdArg) { type = NavType.StringType }),
        ) { entry ->
            TeacherContactRoute(
                teacherId = entry.arguments?.getString(LZRoutes.TeacherIdArg).orEmpty(),
                onNavigateBack = { navController.popBackStack() },
                onShowMessage = onShowMessage,
            )
        }
        composable(LZRoutes.StudentPlanning) {
            StudentPlanningScreen(
                onNavigateBack = { goTab(LZRoutes.StudentHome) },
                onShowMessage = onShowMessage,
                onAvatarClick = onAvatarClick,
                onOpenPdf = { levelId -> navController.navigate(LZRoutes.studentPlanningPdf(levelId)) },
            )
        }
        composable(
            route = LZRoutes.StudentPlanningPdfPattern,
            arguments = listOf(navArgument(LZRoutes.LevelIdArg) { type = NavType.StringType }),
        ) { entry ->
            StudentPlanningPdfScreen(
                levelId = entry.arguments?.getString(LZRoutes.LevelIdArg).orEmpty(),
                onNavigateBack = { navController.popBackStack() },
                onShowMessage = onShowMessage,
            )
        }
        composable(LZRoutes.StudentCourses) {
            StudentCoursesScreen(onNavigateBack = { goTab(LZRoutes.StudentHome) }, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.StudentNotifications) {
            StudentNotificationsScreen(onNavigateBack = { goTab(LZRoutes.StudentHome) }, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.StudentProfile) {
            com.lzacademy.app.ui.student.StudentProfileRoute(
                studentId = currentUser.linkedStudentId,
                fullName = currentUser.fullName,
                initials = currentUser.initials,
                email = currentUser.email,
                onNavigateBack = { goTab(LZRoutes.StudentHome) },
                onShowMessage = onShowMessage,
                onAvatarClick = onAvatarClick,
                onOpenSettings = openSettings,
            )
        }

        // ==================== Teacher Routes ====================
        composable(LZRoutes.TeacherHome) {
            TeacherDashboardRoute(onNavigate = goTab, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.TeacherGroups) {
            TeacherGroupsScreen(
                onNavigateBack = { goTab(LZRoutes.TeacherHome) },
                onShowMessage = onShowMessage,
                onAvatarClick = onAvatarClick,
                onTakeAttendance = { groupId -> navController.navigate(LZRoutes.teacherAttendance(groupId)) },
            )
        }
        composable(LZRoutes.TeacherPlanning) {
            TeacherPlanningScreen(onNavigateBack = { goTab(LZRoutes.TeacherHome) }, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.TeacherResources) {
            TeacherResourcesScreen(onNavigateBack = { goTab(LZRoutes.TeacherHome) }, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.TeacherProfile) {
            TeacherProfileScreen(
                onNavigateBack = { goTab(LZRoutes.TeacherHome) },
                onShowMessage = onShowMessage,
                onAvatarClick = onAvatarClick,
                onOpenSettings = openSettings,
            )
        }
        composable(
            route = "${LZRoutes.TeacherAttendance}/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
        ) { entry ->
            TeacherAttendanceScreen(
                groupId = entry.arguments?.getString("groupId").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
                onShowMessage = onShowMessage,
            )
        }

        // ==================== Administration Routes ====================
        composable(LZRoutes.AdminDashboard) {
            AdminDashboardRoute(onNavigate = goTab, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.AdminManagement) {
            AdminManagementScreen(onNavigateBack = { goTab(LZRoutes.AdminDashboard) }, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.AdminCommunication) {
            AdminCommunicationScreen(onNavigateBack = { goTab(LZRoutes.AdminDashboard) }, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.AdminReports) {
            AdminReportsScreen(onNavigateBack = { goTab(LZRoutes.AdminDashboard) }, onShowMessage = onShowMessage, onAvatarClick = onAvatarClick)
        }
        composable(LZRoutes.AdminMore) {
            AdminMoreScreen(
                onNavigateBack = { goTab(LZRoutes.AdminProfile) },
                onShowMessage = onShowMessage,
                onAvatarClick = onAvatarClick,
                onOpenSubscriptions = { navController.navigate(LZRoutes.AdminSubscriptions) },
                onOpenPacks = { navController.navigate(LZRoutes.AdminPacks) },
            )
        }
        composable(LZRoutes.AdminPacks) {
            com.lzacademy.app.ui.admin.AdminPacksScreen(
                onNavigateBack = { navController.popBackStack() },
                onShowMessage = onShowMessage,
            )
        }
        composable(LZRoutes.AdminProfile) {
            com.lzacademy.app.ui.admin.AdminProfileScreen(
                onShowMessage = onShowMessage,
                onOpenSettings = openSettings,
                onOpenAcademy = { goTab(LZRoutes.AdminMore) },
            )
        }
        composable(LZRoutes.AdminSubscriptions) {
            com.lzacademy.app.ui.admin.AdminSubscriptionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onShowMessage = onShowMessage,
            )
        }

        // ==================== Shared ====================
        composable(LZRoutes.Settings) {
            com.lzacademy.app.ui.settings.SettingsScreen(
                user = currentUser,
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                onLogout = onLogout,
                onNavigateBack = { navController.popBackStack() },
                onShowMessage = onShowMessage,
            )
        }
    }
}
