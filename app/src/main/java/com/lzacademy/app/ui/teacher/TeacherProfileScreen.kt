package com.lzacademy.app.ui.teacher

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.rememberRemoteAvatar
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZHeaderIconButton
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.TeachersViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun TeacherProfileScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeachersViewModel = viewModel(),
) {
    // The signed-in profile resolves its OWN teacher row (profile_id link), never a seed id.
    val context = LocalContext.current
    val preferences = remember(context.applicationContext) {
        AppPreferences(context.applicationContext)
    }
    var profileId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { profileId = preferences.ownProfileId }

    val teacher by remember(profileId) {
        viewModel.teacherByProfile(profileId)
    }.collectAsStateWithLifecycle(initialValue = null)

    // real per-teacher counts (groups / students) from the mirror-driven dashboard VM
    val dashboardViewModel: com.lzacademy.app.viewmodel.TeacherDashboardViewModel = viewModel()
    val dashState by dashboardViewModel.state.collectAsStateWithLifecycle()
    val dash = (dashState as? com.lzacademy.app.viewmodel.UiState.Success<*>)?.data
        as? com.lzacademy.app.models.TeacherDashboardData

    fun statValue(label: String): String =
        dash?.stats?.firstOrNull { it.label == label }?.value ?: "—"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("teacher_profile_screen"),
    ) {
        // Teacher gradient header
        LZGradientHeader(brush = LZ.colors.teacherHeaderBrush) {
            LZGreetingRow(
                initials = teacher?.initials ?: "LZ",
                title = "Mon profil",
                subtitle = teacher?.subjects?.joinToString(" • ")?.takeIf { it.isNotBlank() }
                    ?: "Espace enseignant",
                onAvatarClick = onAvatarClick,
                photo = rememberRemoteAvatar(teacher?.photoUrl?.let { SupabaseApi().avatarUrl(it) }),
            ) {
                LZHeaderIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Paramètres",
                    onClick = onOpenSettings,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Teacher Card — real identity from the mirror, honest empty state when unlinked
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LZAvatar(
                            teacher?.initials ?: "—",
                            size = 56.dp,
                            photo = rememberRemoteAvatar(teacher?.photoUrl?.let { SupabaseApi().avatarUrl(it) }),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LZText(
                                    teacher?.fullName ?: "Aucune donnée — profil non lié",
                                    style = LZ.type.cardTitle,
                                )
                                if (teacher != null) {
                                    Icon(Icons.Rounded.Verified, "Certifié", tint = LZ.colors.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                            val subjects = teacher?.subjects.orEmpty()
                            LZText(
                                when {
                                    subjects.isNotEmpty() -> subjects.joinToString(" • ")
                                    teacher != null -> "Enseignant"
                                    else -> "Connectez-vous avec un compte enseignant lié."
                                },
                                style = LZ.type.secondary,
                                color = LZ.colors.textSecondary,
                            )
                            if (teacher != null && teacher!!.yearsOfExperience > 0) {
                                LZStatusPill(
                                    "${teacher!!.yearsOfExperience} ans d'expérience",
                                    LZ.colors.primary,
                                    LZ.colors.surfaceSoft,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    LZDivider()
                    Spacer(Modifier.height(10.dp))
                    if (teacher?.professionalDescription?.isNotBlank() == true) {
                        LZText(teacher!!.professionalDescription, style = LZ.type.body)
                    } else {
                        LZText(
                            "Aucune donnée — aucune présentation enregistrée.",
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                        )
                    }
                }
            }

            // Group summary — real numbers from the signed-in teacher's mirror data
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    LZText("Mes groupes", style = LZ.type.cardTitle)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            LZText(statValue("Groupes actifs"), style = LZ.type.stat, color = LZ.colors.primary)
                            LZText("Groupes", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            LZText(statValue("Élèves suivis"), style = LZ.type.stat, color = LZ.colors.primary)
                            LZText("Élèves", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                        }
                    }
                }
            }

            // Settings — the one control surface: appearance, notifications, account,
            // plus the teacher's own two server settings (diffusion & contact).
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    LZText("Paramètres", style = LZ.type.cardTitle)
                    Spacer(Modifier.height(6.dp))
                    LZText(
                        "Apparence, notifications, diffusion & contact et compte connecté.",
                        style = LZ.type.secondary,
                        color = LZ.colors.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    LZSecondaryButton(
                        text = "Ouvrir les paramètres",
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Settings,
                    )
                }
            }
        }
    }
}
