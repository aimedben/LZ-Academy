package com.lzacademy.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lzacademy.app.models.AttendanceStatus
import com.lzacademy.app.models.DonutSegmentUi
import com.lzacademy.app.models.FileKind
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.models.PaymentStatus
import com.lzacademy.app.models.TimelineState
import com.lzacademy.app.navigation.LZRoutes
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZPalette
import com.lzacademy.app.theme.LZTheme

@Composable
private fun Gallery(dark: Boolean, content: @Composable () -> Unit) {
    LZTheme(darkTheme = dark) {
        Column(
            Modifier
                .background(LZ.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = { content() },
        )
    }
}

@Composable
private fun ButtonsAndInputs() {
    var text by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf(0) }
    var chip by remember { mutableStateOf(0) }
    LZText("Boutons", style = LZ.type.sectionTitle)
    LZPrimaryButton("Se connecter", {}, Modifier.fillMaxWidth(), trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
    LZSecondaryButton("Renouveler mon abonnement", {}, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Autorenew)
    LZOutlinedButton("Créer un compte", {}, Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LZIconButton(Icons.Rounded.Notifications, "Notifications", {}, badge = 2)
        LZFloatingActionButton(Icons.Rounded.Add, "Ajouter", {})
        LZLinkButton("Voir tout", {})
    }
    LZText("Champs", style = LZ.type.sectionTitle)
    LZSearchBar(search, { search = it }, placeholder = "Rechercher un document...")
    LZTextInput(text, { text = it }, "Email ou téléphone")
    LZPasswordInput(pwd, { pwd = it })
    LZDropdownField(listOf("Terminale", "2ème AS", "1ère AS"), "Terminale", {})
    LZSegmentedControl(listOf("Semaine", "Mois"), segment, { segment = it })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Tous", "Maths", "Physique").forEachIndexed { i, s -> LZFilterChip(s, chip == i, { chip = i }) }
    }
}

@Composable
private fun CardsGallery() {
    LZText("Cartes", style = LZ.type.sectionTitle)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LZStatCard(Icons.Rounded.CalendarMonth, "3", "Séances restantes", Modifier.weight(1f), containerColor = LZ.colors.surfaceSoft)
        LZStatCard(Icons.Rounded.MenuBook, "2", "Matières actives", Modifier.weight(1f), accent = LessonAccent.Violet)
    }
    LZHeroCard(
        "Prochain cours", "Mathématiques", "Avec M. Lebbah • Salle A1", "Lundi · 16:00 – 18:00",
        LZ.colors.heroGreenBrush,
        illustration = { LZGraduationIllustration(Modifier.align(Alignment.CenterEnd)) },
    )
    LZLessonCard("Physique", "18:15 – 20:15", "Salle B2", LZPalette.Violet, person = "M. Said")
    LZCard {
        LZTimelineItem("09:00", "1ère AS – Mathématiques", "Salle 101", LZ.colors.primary, TimelineState.Done, false)
        LZTimelineItem("14:00", "Terminale – Mathématiques", "Salle B2", LZPalette.Violet, TimelineState.Current, false)
        LZTimelineItem("17:00", "3ème AM – Mathématiques", "Salle 103", LZPalette.Info, TimelineState.Upcoming, true)
    }
    LZAnnouncementCard("Nouveau résumé disponible", "Mathématiques • Aujourd'hui", "Le résumé du chapitre 3 est en ligne.", Icons.Rounded.Campaign, LessonAccent.Blue, true, true, {})
    LZNotificationCard("Rappel de paiement", "Votre forfait expire bientôt", Icons.Rounded.Notifications, LessonAccent.Orange, time = "Il y a 1 h", unread = true)
    LZPaymentCard("Abonnement 4 séances", "05/10/2026", "6 000 DA", PaymentStatus.Paid)
    LZCourseCard("Physique", "M. Said", 0.64f, Icons.Rounded.Science, LessonAccent.Violet)
    LZSubscriptionCard(3, 4, "Actif")
    LZCircularProgressCard(3, 4, caption = "3 restantes • 1 consommée")
    LZStudentCard("Amine Bensaci", "Terminale", trailing = { LZAttendanceStatusBadge(AttendanceStatus.Present) })
    LZTeacherCard("Karim Benali", "Mathématiques", 6, 42)
    LZAvatarGroup(listOf("AB", "SM", "YK", "LA", "MH", "RB"))
    LZCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
        LZFolderItem("Mathématiques", "12 fichiers • 245 Mo")
        LZDivider()
        LZFileItem("Chapitre 1 - Fonctions", "PDF • 2,4 Mo", FileKind.Pdf, onMore = {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LZAttendanceStatusBadge(AttendanceStatus.Absent)
        LZAttendanceStatusBadge(AttendanceStatus.Late)
        LZAttendanceStatusBadge(AttendanceStatus.Justified)
    }
}

@Composable
private fun ChartsGallery() {
    LZText("Graphiques", style = LZ.type.sectionTitle)
    LZChartCard("Évolution des inscriptions") {
        LZBarChart(listOf(12f, 18f, 15f, 22f, 28f, 25f, 34f, 30f, 38f, 41f, 36f, 48f), listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"), selectedIndex = 9)
    }
    LZChartCard("Inscriptions") {
        LZLineChart(listOf(20f, 28f, 25f, 38f, 34f, 46f, 52f), listOf("Sep", "Oct", "Nov", "Déc", "Jan", "Fév", "Mar"))
    }
    LZChartCard("Répartition par matière") {
        LZDonutChart(
            listOf(
                DonutSegmentUi("Maths", 35f, LZPalette.PrimaryGreen),
                DonutSegmentUi("Physique", 30f, LZPalette.Gold),
                DonutSegmentUi("Info", 25f, LZPalette.Violet),
                DonutSegmentUi("Autres", 10f, LZPalette.Turquoise),
            ),
        )
    }
    LZCard { LZLinearProgressCard("Terminale", "94 %", 0.94f, subtitle = "24 élèves") }
}

@Composable
private fun NavigationGallery() {
    LZText("Navigation", style = LZ.type.sectionTitle)
    StudentBottomNavigation(LZRoutes.StudentHome, {})
    TeacherBottomNavigation(LZRoutes.TeacherGroups, {})
    AdminBottomNavigation(LZRoutes.AdminDashboard, {})
}

@Composable
private fun FeedbackGallery() {
    LZText("États", style = LZ.type.sectionTitle)
    LZCard { LZEmptyState("Aucun cours ce jour", "Profitez-en pour réviser vos derniers chapitres.") }
    LZSkeleton(Modifier.fillMaxWidth().height(76.dp))
}

@Preview(name = "Composants – boutons & champs", showBackground = true, widthDp = 393, heightDp = 900)
@Composable
private fun PreviewButtons() = Gallery(false) { ButtonsAndInputs() }

@Preview(name = "Composants – cartes", showBackground = true, widthDp = 393, heightDp = 2600)
@Composable
private fun PreviewCards() = Gallery(false) { CardsGallery() }

@Preview(name = "Composants – cartes (sombre)", showBackground = true, widthDp = 393, heightDp = 2600)
@Composable
private fun PreviewCardsDark() = Gallery(true) { CardsGallery() }

@Preview(name = "Composants – graphiques", showBackground = true, widthDp = 393, heightDp = 1000)
@Composable
private fun PreviewCharts() = Gallery(false) { ChartsGallery() }

@Preview(name = "Composants – navigation", showBackground = true, widthDp = 393, heightDp = 420)
@Composable
private fun PreviewNavigation() = Gallery(false) { NavigationGallery() }

@Preview(name = "Composants – navigation (sombre)", showBackground = true, widthDp = 393, heightDp = 420)
@Composable
private fun PreviewNavigationDark() = Gallery(true) { NavigationGallery() }

@Preview(name = "Composants – états", showBackground = true, widthDp = 393, heightDp = 360)
@Composable
private fun PreviewFeedback() = Gallery(false) { FeedbackGallery() }
