package com.lzacademy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.AdminBottomNavigation
import com.lzacademy.app.components.LZLogo
import com.lzacademy.app.components.LZSnackbarHost
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.StudentBottomNavigation
import com.lzacademy.app.components.TeacherBottomNavigation
import com.lzacademy.app.models.AuthState
import com.lzacademy.app.models.AuthUser
import com.lzacademy.app.models.UserRole
import com.lzacademy.app.navigation.LZNavHost
import com.lzacademy.app.navigation.LZRoutes
import com.lzacademy.app.navigation.navigateToTab
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.ThemeMode
import com.lzacademy.app.ui.auth.LoginScreen
import com.lzacademy.app.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * App root: session gate, then scaffold, role-aware bottom navigation and snackbar host.
 * The role is always the one of the signed-in account; the avatar opens "Mon compte".
 */
@Composable
fun LZApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    when (val state = authState) {
        AuthState.Restoring -> SplashScreen(modifier)
        AuthState.SignedOut -> LoginScreen(viewModel = authViewModel, modifier = modifier)
        is AuthState.SignedIn -> key(state.user.id) {
            SignedInApp(
                user = state.user,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onLogout = authViewModel::logout,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SignedInApp(
    user: AuthUser,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val role = user.role

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            snackbarHost.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = LZ.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { LZSnackbarHost(snackbarHost) },
        bottomBar = {
            when (role) {
                UserRole.Student -> StudentBottomNavigation(
                    selectedRoute = route,
                    onSelect = navController::navigateToTab,
                )
                UserRole.Teacher -> TeacherBottomNavigation(route, navController::navigateToTab)
                UserRole.Admin -> AdminBottomNavigation(route, navController::navigateToTab)
            }
        },
    ) { inner ->
        LZNavHost(
            navController = navController,
            currentUser = user,
            onShowMessage = showMessage,
            onAvatarClick = { navController.navigate(LZRoutes.Settings) },
            themeMode = themeMode,
            onThemeModeSelected = onThemeModeChange,
            onLogout = onLogout,
            modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
        )
    }
}

/** Very short local session restore. No network call, so it is only visible for a few frames. */
@Composable
private fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(LZ.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            LZLogo(size = 84.dp)
        }
        LZText(
            "LZ Academy",
            style = LZ.type.cardTitle,
            color = LZ.colors.textSecondary,
            modifier = Modifier.padding(top = 150.dp),
        )
    }
}
