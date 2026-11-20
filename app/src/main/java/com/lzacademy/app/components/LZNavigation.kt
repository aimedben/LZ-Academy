package com.lzacademy.app.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lzacademy.app.navigation.LZRoutes
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZSize

@Immutable
data class LZNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badge: Int = 0,
)

/** Generic rounded bottom bar. Use the role specific wrappers below. */
@Composable
fun LZBottomBar(
    items: List<LZNavItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = LZ.colors.primary,
) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .softShadow(14.dp, shape)
            .background(LZ.colors.surface, shape)
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(LZSize.BottomBarHeight)
                .padding(horizontal = 6.dp),
        ) {
            items.forEach { item ->
                LZBottomBarItem(
                    item = item,
                    selected = item.id == selectedId,
                    accent = accent,
                    onClick = { onSelect(item.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LZBottomBarItem(
    item: LZNavItem,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(if (selected) accent else LZ.colors.textSecondary, label = "navTint")
    val iconScale by animateFloatAsState(if (selected) 1.12f else 1f, label = "navScale")
    val indicator by animateDpAsState(if (selected) 22.dp else 0.dp, label = "navIndicator")
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier.size(25.dp).scale(iconScale),
            )
            if (item.badge > 0) {
                LZNotificationBadge(item.badge, Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-5).dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        LZText(item.label, style = LZ.type.labelSmall, color = tint, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(indicator).height(3.dp).background(accent, LZShapes.pill))
    }
}

/** Student tabs: Accueil, Planning, Cours, Enseignants, Profil. */
@Composable
fun StudentBottomNavigation(
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember {
        listOf(
            LZNavItem(LZRoutes.StudentHome, "Accueil", Icons.Outlined.Home, Icons.Rounded.Home),
            LZNavItem(LZRoutes.StudentPlanning, "Planning", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
            LZNavItem(LZRoutes.StudentCourses, "Cours", Icons.Outlined.MenuBook, Icons.Rounded.MenuBook),
            LZNavItem(LZRoutes.StudentTeacherDirectory, "Enseignants", Icons.Outlined.People, Icons.Rounded.People),
            LZNavItem(LZRoutes.StudentProfile, "Profil", Icons.Outlined.Person, Icons.Rounded.Person),
        )
    }
    LZBottomBar(items, selectedRoute, onSelect, modifier)
}

/** Teacher tabs: Accueil, Groupes, Planning, Ressources, Profil. */
@Composable
fun TeacherBottomNavigation(
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember {
        listOf(
            LZNavItem(LZRoutes.TeacherHome, "Accueil", Icons.Outlined.Home, Icons.Rounded.Home),
            LZNavItem(LZRoutes.TeacherGroups, "Groupes", Icons.Outlined.Groups, Icons.Rounded.Groups),
            LZNavItem(LZRoutes.TeacherPlanning, "Planning", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
            LZNavItem(LZRoutes.TeacherResources, "Ressources", Icons.Outlined.Folder, Icons.Rounded.Folder),
            LZNavItem(LZRoutes.TeacherProfile, "Profil", Icons.Outlined.Person, Icons.Rounded.Person),
        )
    }
    LZBottomBar(items, selectedRoute, onSelect, modifier)
}

/** Administration tabs: Dashboard, Gestion, Communiquer, Rapports, Plus. */
@Composable
fun AdminBottomNavigation(
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember {
        listOf(
            LZNavItem(LZRoutes.AdminDashboard, "Dashboard", Icons.Outlined.Dashboard, Icons.Rounded.Dashboard),
            LZNavItem(LZRoutes.AdminManagement, "Gestion", Icons.Outlined.ManageAccounts, Icons.Rounded.ManageAccounts),
            LZNavItem(LZRoutes.AdminCommunication, "Communiquer", Icons.Outlined.Chat, Icons.Rounded.Chat),
            LZNavItem(LZRoutes.AdminReports, "Rapports", Icons.Outlined.BarChart, Icons.Rounded.BarChart),
            LZNavItem(LZRoutes.AdminMore, "Plus", Icons.Outlined.Apps, Icons.Rounded.Apps),
        )
    }
    LZBottomBar(items, selectedRoute, onSelect, modifier, accent = LZ.colors.purple)
}

@Immutable
data class LZDrawerItem(val id: String, val label: String, val icon: ImageVector, val badge: Int = 0)

/** Modal side drawer with the LZ look. [header] defaults to the academy logo and name. */
@Composable
fun LZNavigationDrawer(
    drawerState: DrawerState,
    items: List<LZDrawerItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = { LZDrawerHeader() },
    content: @Composable () -> Unit,
) {
    val c = LZ.colors
    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = c.surface) {
                header()
                items.forEach { item ->
                    NavigationDrawerItem(
                        label = { LZText(item.label, style = LZ.type.bodyStrong, maxLines = 1) },
                        selected = item.id == selectedId,
                        onClick = { onSelect(item.id) },
                        icon = { Icon(item.icon, null) },
                        badge = if (item.badge > 0) ({ LZNotificationBadge(item.badge) }) else null,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = c.surfaceSoft,
                            selectedIconColor = c.primary,
                            selectedTextColor = c.primary,
                            unselectedIconColor = c.textSecondary,
                            unselectedTextColor = c.textPrimary,
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
fun LZDrawerHeader(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LZLogo(size = 56.dp)
        Column {
            LZText("LZ Academy", style = LZ.type.brand.copy(fontSize = 22.sp), maxLines = 1)
            LZText("Centre de formation et d'éducation", style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 2)
        }
    }
}
