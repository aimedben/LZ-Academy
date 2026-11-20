package com.lzacademy.app.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lzacademy.app.components.LZSegmentedControl
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.pressable
import com.lzacademy.app.models.UserRole
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.ThemeMode

/** Development-only sheet: switch between the three role experiences and the theme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevRoleSwitcherSheet(
    currentRole: UserRole,
    themeMode: ThemeMode,
    onRoleSelected: (UserRole) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LZ.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        shape = LZShapes.sheet,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LZText("Mode développement", style = LZ.type.sectionTitle)
            LZText(
                "Changez d'espace pour tester les trois expériences. Cet écran n'apparaît que dans les builds debug.",
                style = LZ.type.secondary,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            UserRole.entries.forEach { role ->
                val selected = role == currentRole
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (selected) c.surfaceSoft else c.surface, LZShapes.standard)
                        .border(1.dp, if (selected) c.primary else c.border, LZShapes.standard)
                        .pressable(LZShapes.standard) { onRoleSelected(role) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        when (role) {
                            UserRole.Student -> Icons.Rounded.Person
                            UserRole.Teacher -> Icons.Rounded.School
                            UserRole.Admin -> Icons.Rounded.AdminPanelSettings
                        },
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(26.dp),
                    )
                    LZText(role.label, style = LZ.type.cardTitle, modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Rounded.CheckCircle, "Sélectionné", tint = c.primary)
                }
            }
            Spacer(Modifier.height(4.dp))
            LZText("Thème", style = LZ.type.bodyStrong)
            LZSegmentedControl(
                options = ThemeMode.entries.map { it.label },
                selectedIndex = themeMode.ordinal,
                onSelect = { onThemeModeSelected(ThemeMode.entries[it]) },
            )
        }
    }
}
