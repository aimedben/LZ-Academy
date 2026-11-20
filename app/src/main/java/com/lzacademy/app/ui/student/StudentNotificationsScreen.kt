package com.lzacademy.app.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lzacademy.app.components.LZAnnouncementCard
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.models.AnnouncementUi
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize

@Composable
fun StudentNotificationsScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialAnnouncements = remember {
        listOf(
            AnnouncementUi(
                id = "n1",
                title = "BAC Blanc Session Mai 2026",
                subtitle = "Examens blancs du 17 au 22 mai • Planning complet",
                body = "Les épreuves blanches débuteront le dimanche 17 mai à 08h30. Veuillez vous présenter 20 minutes avant le début muni de votre convocation et de votre carte d'étudiant LZ Academy.",
                icon = Icons.Rounded.School,
                accent = LessonAccent.Green,
                unread = true,
            ),
            AnnouncementUi(
                id = "n2",
                title = "Séance de rattrapage de Physique",
                subtitle = "Jeudi 14 mai à 14h00 en Salle B2",
                body = "La séance annulée le mardi précédent est reportée à ce jeudi de 14h à 16h avec M. Said. Le polycopié du chapitre 'Ondes et vibrations' sera distribué sur place.",
                icon = Icons.Rounded.CalendarMonth,
                accent = LessonAccent.Violet,
                unread = true,
            ),
            AnnouncementUi(
                id = "n3",
                title = "Reçu de paiement confirmé",
                subtitle = "Abonnement Pack BAC Mai 2026 • 12 000 DZD",
                body = "Votre règlement pour le mois en cours a été validé par l'administration. Votre carte d'accès numérique a été actualisée pour l'ensemble des séances programmées.",
                icon = Icons.Rounded.Payments,
                accent = LessonAccent.Blue,
                unread = false,
            ),
            AnnouncementUi(
                id = "n4",
                title = "Atelier Méthodologie & Gestion du stress",
                subtitle = "Samedi 24 mai animé par nos conseillers d'orientation",
                body = "Participez à notre atelier exclusif de 2 heures pour apprendre à structurer votre temps de révision et aborder les épreuves du baccalauréat avec sérénité.",
                icon = Icons.Rounded.Campaign,
                accent = LessonAccent.Orange,
                unread = false,
            ),
        )
    }

    val announcements = remember { mutableStateListOf<AnnouncementUi>().apply { addAll(initialAnnouncements) } }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf("Toutes") }

    val unreadCount = announcements.count { it.unread }
    val filters = listOf("Toutes", "Non lues", "Planning", "Paiements")

    val filteredList = remember(announcements.toList(), selectedFilter) {
        when (selectedFilter) {
            "Non lues" -> announcements.filter { it.unread }
            "Planning" -> announcements.filter { it.accent == LessonAccent.Violet || it.icon == Icons.Rounded.CalendarMonth }
            "Paiements" -> announcements.filter { it.icon == Icons.Rounded.Payments }
            else -> announcements
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("student_notifications_screen"),
    ) {
        // Gradient Header
        LZGradientHeader(brush = LZ.colors.studentHeaderBrush) {
            LZGreetingRow(
                initials = "RB",
                title = "Notifications & Annonces",
                subtitle = if (unreadCount > 0) "$unreadCount non lue${if (unreadCount > 1) "s" else ""}" else "Toutes lues",
                onAvatarClick = onAvatarClick,
                photo = rememberOwnStudentPhoto(),
                trailing = {
                    IconButton(
                        onClick = {
                            announcements.indices.forEach { idx ->
                                announcements[idx] = announcements[idx].copy(unread = false)
                            }
                            onShowMessage("Toutes les notifications sont marquées comme lues")
                        },
                        modifier = Modifier.testTag("mark_all_read_button"),
                    ) {
                        Icon(Icons.Rounded.DoneAll, "Tout marquer lu", tint = Color.White)
                    }
                },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Filters row
            item {
                LazyRow(
                    modifier = Modifier.padding(top = 16.dp),
                    contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filters) { filter ->
                        LZFilterChip(
                            label = if (filter == "Non lues" && unreadCount > 0) "$filter ($unreadCount)" else filter,
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    LZEmptyState(
                        title = "Aucune notification",
                        message = "Vous êtes à jour ! Aucune nouvelle annonce dans cette catégorie.",
                        icon = Icons.Rounded.NotificationsNone,
                        actionLabel = "Afficher toutes les notifications",
                        onAction = { selectedFilter = "Toutes" },
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 28.dp),
                    )
                }
            } else {
                item {
                    LZSectionHeader(
                        title = "Dernières actualités",
                        actionLabel = "${filteredList.size} annonces",
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                }
                items(filteredList) { item ->
                    Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                        LZAnnouncementCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            body = item.body,
                            icon = item.icon,
                            accent = item.accent,
                            unread = item.unread,
                            expanded = expandedId == item.id,
                            onToggle = {
                                expandedId = if (expandedId == item.id) null else item.id
                                if (item.unread) {
                                    val index = announcements.indexOfFirst { it.id == item.id }
                                    if (index != -1) announcements[index] = item.copy(unread = false)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
