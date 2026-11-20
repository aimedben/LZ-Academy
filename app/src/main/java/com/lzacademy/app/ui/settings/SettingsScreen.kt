package com.lzacademy.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZSegmentedControl
import com.lzacademy.app.components.LZSettingsActionRow
import com.lzacademy.app.components.LZSettingsSection
import com.lzacademy.app.components.LZSettingsSwitchRow
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTopBar
import com.lzacademy.app.components.TeacherAvailabilityBadge
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.settings.SettingsSection
import com.lzacademy.app.data.settings.notificationSwitchesFor
import com.lzacademy.app.models.AuthUser
import com.lzacademy.app.models.UserRole
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.theme.ThemeMode
import com.lzacademy.app.viewmodel.TeachersViewModel

/**
 * The one settings surface of the app, reached from every profile page and from any header
 * avatar. Every switch here has a real effect (see `notificationSwitchesFor`); nothing is
 * decorative, and nothing is fetched from the network beyond the teacher row the teacher
 * already owns.
 */
@Composable
fun SettingsScreen(
    user: AuthUser,
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeachersViewModel = viewModel(),
) {
    val context = LocalContext.current
    val preferences = remember(context.applicationContext) { AppPreferences(context.applicationContext) }

    val localSwitches = remember(user.role) { notificationSwitchesFor(user.role) }
    val alerts = localSwitches.filter { it.section == SettingsSection.Notifications }
    val dataSwitches = localSwitches.filter { it.section == SettingsSection.Data }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("settings_screen"),
    ) {
        LZTopBar("Paramètres", onBack = onNavigateBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = LZSize.ScreenPadding, end = LZSize.ScreenPadding, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Compte ----
            item {
                LZSettingsSection(title = "Compte", icon = Icons.Rounded.Person) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LZAvatar(initials = user.initials, size = 54.dp, background = LZ.colors.primaryBrush)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            LZText(user.fullName, style = LZ.type.cardTitle, maxLines = 1)
                            LZText(user.email, style = LZ.type.secondary, color = LZ.colors.textSecondary, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            LZStatusPill(user.role.label, LZ.colors.primary, LZ.colors.surfaceSoft)
                        }
                    }
                    LZDivider()
                    LZSettingsActionRow(
                        title = "Se déconnecter",
                        description = "La session se ferme ; les données locales (présences, abonnements) sont conservées.",
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        tint = LZ.colors.error,
                        onClick = onLogout,
                    )
                }
            }

            // ---- Apparence ----
            item {
                LZSettingsSection(title = "Apparence", icon = Icons.Rounded.Palette) {
                    Spacer(Modifier.height(6.dp))
                    LZSegmentedControl(
                        options = ThemeMode.entries.map { it.label },
                        selectedIndex = themeMode.ordinal,
                        onSelect = { onThemeModeSelected(ThemeMode.entries[it]) },
                    )
                    Spacer(Modifier.height(8.dp))
                    LZText(
                        "Le thème clair est appliqué par défaut, même si le téléphone est en mode sombre.",
                        style = LZ.type.label,
                        color = LZ.colors.textSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            // ---- Notifications (local switches with a real effect) ----
            if (alerts.isNotEmpty()) {
                item {
                    LZSettingsSection(title = "Notifications", icon = Icons.Rounded.NotificationsActive) {
                        alerts.forEachIndexed { index, switch ->
                            LocalSwitchRow(switch, preferences)
                            if (index < alerts.lastIndex) LZDivider()
                        }
                    }
                }
            }

            // ---- Données & synchronisation ----
            if (dataSwitches.isNotEmpty()) {
                item {
                    LZSettingsSection(title = "Données & synchronisation", icon = Icons.Rounded.CloudSync) {
                        dataSwitches.forEachIndexed { index, switch ->
                            LocalSwitchRow(switch, preferences)
                            if (index < dataSwitches.lastIndex) LZDivider()
                        }
                    }
                }
            }

            // ---- Teacher-only: the two columns he owns server-side ----
            if (user.role == UserRole.Teacher) {
                item {
                    TeacherServerSettings(
                        preferences = preferences,
                        viewModel = viewModel,
                        onShowMessage = onShowMessage,
                    )
                }
            }
        }
    }
}

/** One local preference switch: read on entry, written on toggle, honoured by the app. */
@Composable
private fun LocalSwitchRow(
    switch: com.lzacademy.app.data.settings.NotificationSwitch,
    preferences: AppPreferences,
) {
    var checked by remember(switch.id) { mutableStateOf(preferences.notificationEnabled(switch.id, switch.default)) }
    LZSettingsSwitchRow(
        title = switch.title,
        description = switch.description,
        checked = checked,
        onCheckedChange = { value ->
            checked = value
            preferences.setNotificationEnabled(switch.id, value)
        },
    )
}

/**
 * The signed-in teacher's two server settings. They live here — not on the profile page — so
 * there is a single control surface; they are written to `public.teachers` and the student app
 * reflects them immediately.
 */
@Composable
private fun TeacherServerSettings(
    preferences: AppPreferences,
    viewModel: TeachersViewModel,
    onShowMessage: (String) -> Unit,
) {
    var profileId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { profileId = preferences.ownProfileId }

    val teacher by remember(profileId) { viewModel.teacherByProfile(profileId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val teacherId = teacher?.id

    LZSettingsSection(title = "Diffusion & contact", icon = Icons.Rounded.Tune) {
        LZSettingsSwitchRow(
            title = "Afficher mon profil sur l'accueil des élèves",
            description = "Visible dans « Nos enseignants ».",
            checked = teacher?.isFeaturedOnStudentHome ?: true,
            enabled = teacher != null,
            icon = Icons.Rounded.Visibility,
            onCheckedChange = { value ->
                teacherId?.let {
                    viewModel.setShowcaseVisibility(it, value)
                    onShowMessage(if (value) "Profil visible sur l'accueil des élèves." else "Profil masqué de l'accueil des élèves.")
                }
            },
        )
        LZDivider()
        LZSettingsSwitchRow(
            title = "Recevoir les messages des élèves",
            description = "Les élèves peuvent vous écrire depuis la fiche enseignant.",
            checked = teacher?.acceptsStudentMessages ?: true,
            enabled = teacher != null,
            icon = Icons.AutoMirrored.Rounded.Chat,
            onCheckedChange = { value ->
                teacherId?.let {
                    viewModel.setAcceptsMessages(it, value)
                    onShowMessage(if (value) "Les élèves peuvent vous écrire." else "Les messages des élèves sont désactivés.")
                }
            },
            trailing = { TeacherAvailabilityBadge(teacher?.acceptsStudentMessages ?: true) },
        )
    }
}
