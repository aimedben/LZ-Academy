package com.lzacademy.app.ui.previews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import com.lzacademy.app.data.FrenchDates
import com.lzacademy.app.models.AdminDashboardData
import com.lzacademy.app.models.AlertUi
import com.lzacademy.app.models.AnnouncementUi
import com.lzacademy.app.models.ChartSeriesUi
import com.lzacademy.app.models.DonutSegmentUi
import com.lzacademy.app.models.FileKind
import com.lzacademy.app.models.FileUi
import com.lzacademy.app.models.GroupAttendanceUi
import com.lzacademy.app.models.HeaderStatUi
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.models.LessonUi
import com.lzacademy.app.models.NextLessonUi
import com.lzacademy.app.models.PaymentStatus
import com.lzacademy.app.models.PaymentUi
import com.lzacademy.app.models.QuickActionUi
import com.lzacademy.app.models.RevenueUi
import com.lzacademy.app.models.ScheduleChangeUi
import com.lzacademy.app.models.SessionUi
import com.lzacademy.app.models.StatUi
import com.lzacademy.app.models.StudentDashboardData
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.models.TeacherDashboardData
import com.lzacademy.app.models.TimelineLessonUi
import com.lzacademy.app.models.TimelineState
import com.lzacademy.app.models.WeekDayUi
import com.lzacademy.app.theme.LZPalette
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * FICTIONAL data used ONLY by `@Preview` composables (Android Studio layout rendering).
 * Never referenced by production screens or repositories: the app shows the real academy
 * database, this file exists so previews stay visible without a server connection.
 */
object PreviewSamples {

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    // ------------------------------------------------------------------ student

    fun student(today: LocalDate, now: LocalTime): StudentDashboardData {
        val rawIndex = today.dayOfWeek.value - 1 // 0 = Monday ... 6 = Sunday
        val todayIndex = rawIndex.coerceAtMost(5)
        val nowText = now.format(timeFormat)
        val weekStart = today.minusDays(rawIndex.toLong())

        fun lesson(
            day: Int, id: String, subject: String, start: String, end: String,
            room: String, person: String, accent: LessonAccent,
        ) = LessonUi(
            id = id, subject = subject, start = start, end = end, room = room, person = person, accent = accent,
            done = day < rawIndex || (day == rawIndex && end < nowText),
        )

        val lessons: Map<Int, List<LessonUi>> = mapOf(
            0 to listOf(
                lesson(0, "l1", "Mathématiques", "16:00", "18:00", "Salle A1", "M. Lebbah", LessonAccent.Green),
                lesson(0, "l2", "Physique", "18:15", "20:15", "Salle B2", "M. Said", LessonAccent.Violet),
            ),
            1 to listOf(
                lesson(1, "l3", "Français", "14:00", "16:00", "Salle A1", "Mme Haddad", LessonAccent.Orange),
            ),
            2 to listOf(
                lesson(2, "l4", "Mathématiques", "16:00", "18:00", "Salle A1", "M. Lebbah", LessonAccent.Green),
                lesson(2, "l5", "Informatique", "18:30", "20:00", "Salle 103", "M. Karim", LessonAccent.Blue),
            ),
            3 to listOf(
                lesson(3, "l6", "Physique", "16:00", "18:00", "Salle B2", "M. Said", LessonAccent.Violet),
            ),
            4 to listOf(
                lesson(4, "l7", "Français", "15:00", "17:00", "Salle A1", "Mme Haddad", LessonAccent.Orange),
            ),
            5 to listOf(
                lesson(5, "l8", "Mathématiques", "10:00", "12:00", "Salle A1", "M. Lebbah", LessonAccent.Green),
                lesson(5, "l9", "Physique", "13:00", "15:00", "Salle B2", "M. Said", LessonAccent.Violet),
            ),
        )

        return StudentDashboardData(
            firstName = "Amine",
            initials = "AB",
            unreadNotifications = 2,
            remainingSessions = 3,
            totalSessions = 4,
            activeSubjects = 4,
            nextLesson = nextLesson(lessons, rawIndex, nowText),
            todayIndex = todayIndex,
            week = (0..5).map { i ->
                WeekDayUi(i, FrenchDates.dayShort[i], weekStart.plusDays(i.toLong()).dayOfMonth)
            },
            lessonsByDay = lessons,
            scheduleChange = ScheduleChangeUi(
                title = "Séance modifiée",
                subtitle = "Physique • 18:15",
                detail = "Le cours de physique de M. Said commence désormais à 18:15 en salle B2.",
            ),
            materials = listOf(
                FileUi("f1", "Chapitre 1 - Fonctions", "PDF • 2,4 Mo", FileKind.Pdf),
                FileUi("f2", "TD - Dérivées", "Document • 1,1 Mo", FileKind.Doc),
                FileUi("f3", "Résumé - Intégrales", "Présentation • 890 Ko", FileKind.Slides),
            ),
            announcements = listOf(
                AnnouncementUi(
                    "a1", "Nouveau résumé disponible", "Mathématiques • Aujourd'hui",
                    "M. Lebbah a publié le résumé du chapitre « Fonctions et dérivées ». " +
                        "Retrouvez-le dans la rubrique Cours, dossier Résumés.",
                    Icons.Rounded.Description, LessonAccent.Blue, unread = true,
                ),
                AnnouncementUi(
                    "a2", "Séance de physique décalée", "Physique • Hier",
                    "La séance de physique de lundi débutera à 18:15 au lieu de 18:00. " +
                        "Merci d'arriver quelques minutes à l'avance.",
                    Icons.Rounded.Event, LessonAccent.Orange, unread = true,
                ),
                AnnouncementUi(
                    "a3", "Votre forfait arrive à échéance", "Abonnement • Il y a 2 jours",
                    "Il vous reste 3 séances sur 4. Chaque présence décompte automatiquement une " +
                        "séance : demandez le renouvellement à l'administration pour garder votre place dans le groupe.",
                    Icons.Rounded.Payments, LessonAccent.Violet, unread = false,
                ),
                AnnouncementUi(
                    "a4", "Bienvenue sur LZ Academy", "Information générale",
                    "Retrouvez ici votre planning, vos cours, vos paiements et toutes les annonces du centre.",
                    Icons.Rounded.Campaign, LessonAccent.Green, unread = false,
                ),
            ),
        )
    }

    private fun nextLesson(lessons: Map<Int, List<LessonUi>>, rawToday: Int, nowText: String): NextLessonUi {
        for (offset in 0..7) {
            val day = (rawToday + offset) % 7
            val list = lessons[day].orEmpty()
            val candidate = if (offset == 0) list.firstOrNull { it.start > nowText } else list.firstOrNull()
            if (candidate != null) {
                val label = when (offset) {
                    0 -> "Aujourd'hui"
                    1 -> "Demain"
                    else -> FrenchDates.dayLong[day]
                }
                return NextLessonUi(
                    subject = candidate.subject,
                    teacher = candidate.person,
                    whenLabel = "$label · ${candidate.start} – ${candidate.end}",
                    room = candidate.room,
                )
            }
        }
        return NextLessonUi("Mathématiques", "M. Lebbah", "Lundi · 16:00 – 18:00", "Salle A1")
    }

    // ------------------------------------------------------------------ teacher

    fun teacher(): TeacherDashboardData = TeacherDashboardData(
        name = "M. Karim",
        initials = "KB",
        stats = listOf(
            StatUi(Icons.Rounded.Groups, "42", "Élèves suivis", LessonAccent.Green),
            StatUi(Icons.Rounded.Class, "6", "Groupes actifs", LessonAccent.Violet),
            StatUi(Icons.Rounded.EventAvailable, "18", "Séances effectuées", LessonAccent.Orange),
            StatUi(Icons.Rounded.Event, "4", "Cours aujourd'hui", LessonAccent.Blue),
            StatUi(Icons.Rounded.Timer, "36 h", "Heures ce mois", LessonAccent.Violet),
            StatUi(Icons.Rounded.HowToReg, "92 %", "Taux de présence", LessonAccent.Green),
        ),
        nextSession = SessionUi(
            title = "Terminale – Mathématiques",
            subject = "Mathématiques",
            timeRange = "14:00 – 16:00",
            room = "Salle B2",
            studentCount = 24,
        ),
        today = listOf(
            TimelineLessonUi("t1", "09:00", "1ère AS – Mathématiques", "Salle 101 • 22 élèves", TimelineState.Done, LessonAccent.Green),
            TimelineLessonUi("t2", "11:00", "2ème AS – Mathématiques", "Salle 102 • 20 élèves", TimelineState.Done, LessonAccent.Green),
            TimelineLessonUi("t3", "14:00", "Terminale – Mathématiques", "Salle B2 • 24 élèves", TimelineState.Current, LessonAccent.Violet),
            TimelineLessonUi("t4", "17:00", "3ème AM – Mathématiques", "Salle 103 • 18 élèves", TimelineState.Upcoming, LessonAccent.Blue),
        ),
        sessionsByDay = emptyMap(),
        groupAttendance = listOf(
            GroupAttendanceUi("Terminale", 24, 0.94f),
            GroupAttendanceUi("2ème AS", 20, 0.91f),
            GroupAttendanceUi("1ère AS", 22, 0.88f),
            GroupAttendanceUi("3ème AM", 18, 0.96f),
        ),
        weeklyHours = ChartSeriesUi(
            values = listOf(4.5f, 3f, 5f, 2.5f, 4f, 6f),
            labels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"),
        ),
        publications = listOf(
            FileUi("p1", "TD - Mécanique", "PDF • 2,1 Mo • Il y a 1 h", FileKind.Pdf),
            FileUi("p2", "Exercices sur les intégrales", "Document • Il y a 2 h", FileKind.Doc),
            FileUi("p3", "Résumé - Suites numériques", "Présentation • Hier", FileKind.Slides),
        ),
    )

    // ------------------------------------------------------------------ admin

    fun admin(today: LocalDate): AdminDashboardData {
        val baseEnrollment = listOf(18f, 22f, 19f, 27f, 31f, 28f, 36f, 33f, 41f, 38f, 45f, 48f)
        val periods = listOf(3, 6, 12)
        val enrollment = periods.associateWith { n ->
            ChartSeriesUi(
                values = baseEnrollment.takeLast(n),
                labels = (0 until n).map { k ->
                    val month = ((today.monthValue - 1 - (n - 1) + k) % 12 + 12) % 12
                    FrenchDates.monthShort[month]
                },
            )
        }
        return AdminDashboardData(
            adminName = "Admin",
            headerStats = listOf(
                HeaderStatUi(Icons.Rounded.Groups, "320", "Élèves", LZPalette.PastelMint, LZPalette.PrimaryGreen),
                HeaderStatUi(Icons.Rounded.School, "25", "Enseignants", LZPalette.PastelLavender, LZPalette.DeepPurple),
                HeaderStatUi(Icons.Rounded.Class, "12", "Groupes", LZPalette.PastelPeach, LZPalette.Orange),
                HeaderStatUi(Icons.Rounded.Verified, "98 %", "Présence", LZPalette.PastelRose, LZPalette.Violet),
            ),
            quickActions = listOf(
                QuickActionUi("add_student", Icons.Rounded.PersonAdd, "Nouvel élève", LessonAccent.Green),
                QuickActionUi("record_payment", Icons.Rounded.Payments, "Paiement", LessonAccent.Orange),
                QuickActionUi("send_sms", Icons.Rounded.Sms, "SMS", LessonAccent.Blue),
                QuickActionUi("reports", Icons.Rounded.Analytics, "Rapports", LessonAccent.Violet),
            ),
            revenue = RevenueUi(
                amount = "1 240 000 DA",
                delta = "+8,4 %",
                caption = "Revenu mensuel • ${FrenchDates.monthLong[today.monthValue - 1]} ${today.year}",
                trend = ChartSeriesUi(
                    values = listOf(820f, 910f, 880f, 1020f, 990f, 1140f, 1240f),
                    labels = emptyList(),
                ),
            ),
            kpis = listOf(
                StatUi(Icons.Rounded.Groups, "287", "Élèves actifs", LessonAccent.Green),
                StatUi(Icons.Rounded.MenuBook, "8", "Matières", LessonAccent.Violet),
                StatUi(Icons.Rounded.Event, "14", "Séances aujourd'hui", LessonAccent.Blue),
                StatUi(Icons.Rounded.EventBusy, "9", "Absences", LessonAccent.Orange),
            ),
            alerts = listOf(
                AlertUi(
                    "exhausted", Icons.Rounded.Autorenew, "14 abonnements épuisés",
                    "Élèves sans séance restante", "Relancer", LessonAccent.Orange,
                    dialogTitle = "Préparer un rappel ?",
                    dialogMessage = "Un message de rappel sera préparé pour les 14 élèves dont l'abonnement est épuisé.",
                ),
                AlertUi(
                    "renewals", Icons.Rounded.Payments, "23 renouvellements en attente",
                    "Paiements à valider ce mois-ci", "Voir", LessonAccent.Blue,
                    dialogTitle = "Renouvellements en attente",
                    dialogMessage = "23 renouvellements attendent une validation. La gestion complète arrive avec le module Finances.",
                ),
                AlertUi(
                    "absences", Icons.Rounded.EventBusy, "5 absences non justifiées",
                    "À contacter aujourd'hui", "Contacter", LessonAccent.Violet,
                    dialogTitle = "Contacter les familles ?",
                    dialogMessage = "Un message d'absence sera préparé pour les 5 élèves concernés.",
                ),
            ),
            periods = periods,
            enrollment = enrollment,
            subjectDistribution = listOf(
                DonutSegmentUi("Mathématiques", 35f, LZPalette.PrimaryGreen),
                DonutSegmentUi("Physique", 30f, LZPalette.Gold),
                DonutSegmentUi("Informatique", 25f, LZPalette.Violet),
                DonutSegmentUi("Autres", 10f, LZPalette.Turquoise),
            ),
            recentPayments = listOf(
                PaymentUi("pay1", "Sarah Madi", "Abonnement 4 séances", "Aujourd'hui", "6 000 DA", PaymentStatus.Paid),
                PaymentUi("pay2", "Yanis Khelafi", "Abonnement 4 séances", "Hier", "6 000 DA", PaymentStatus.Pending),
                PaymentUi("pay3", "Lina Ait Ahmed", "Abonnement 4 séances", "Hier", "6 000 DA", PaymentStatus.Paid),
                PaymentUi("pay4", "Mohamed Hadj", "Abonnement 4 séances", "Il y a 3 jours", "6 000 DA", PaymentStatus.Overdue),
            ),
        )
    }

    // ------------------------------------------------------------------ teachers (directory previews)

    /** Fictional but realistic teachers for previews only. */
    private val sample: List<Teacher> = listOf(
        Teacher(
            id = "s-karim",
            fullName = "M. Karim Lebbah",
            subjects = listOf("Mathématiques"),
            professionalDescription = "Agrégé de l'ENS, spécialiste de l'analyse et des probabilités pour le BAC.",
            isFeaturedOnStudentHome = true,
            acceptsStudentMessages = true,
            featuredRotationPriority = 100,
            levels = listOf("3AS Scientifique", "3AS Math-Technique", "2AS Sciences"),
            yearsOfExperience = 12,
            groups = 4,
        ),
        Teacher(
            id = "s-amina",
            fullName = "Mme Amina Boudjema",
            subjects = listOf("Physique-Chimie"),
            professionalDescription = "Docteure en physique appliquée, pédagogie orientée expériences et schémas.",
            isFeaturedOnStudentHome = true,
            acceptsStudentMessages = true,
            levels = listOf("3AS", "2AS"),
            yearsOfExperience = 9,
            groups = 3,
        ),
        Teacher(
            id = "s-yacine",
            fullName = "M. Yacine Merad",
            subjects = listOf("Sciences Naturelles"),
            professionalDescription = "Professeur de SVT, spécialiste de la préparation aux examens régionaux.",
            isFeaturedOnStudentHome = true,
            acceptsStudentMessages = true,
            levels = listOf("3AM", "4AM"),
            yearsOfExperience = 6,
            groups = 2,
        ),
        Teacher(
            id = "s-lina",
            fullName = "Mme Lina Ait Ahmed",
            subjects = listOf("Français"),
            professionalDescription = "Lettres modernes, méthodologie de dissertation et expression écrite.",
            isFeaturedOnStudentHome = true,
            acceptsStudentMessages = false,
            levels = listOf("3AS", "1AS"),
            yearsOfExperience = 8,
            groups = 3,
        ),
        Teacher(
            id = "s-said",
            fullName = "M. Said Hammadi",
            subjects = listOf("Anglais"),
            professionalDescription = "Anglais académique et préparation à l'oral du BAC.",
            isFeaturedOnStudentHome = false,
            acceptsStudentMessages = true,
            yearsOfExperience = 5,
            groups = 2,
        ),
        Teacher(
            id = "s-nadia",
            fullName = "Mme Nadia Cherif",
            subjects = listOf("Informatique"),
            professionalDescription = "Algorithmique et programmation, projets pratiques en classe.",
            isFeaturedOnStudentHome = false,
            acceptsStudentMessages = true,
            yearsOfExperience = 4,
            groups = 2,
        ),
    )

    /** 4 teachers for the showcase previews. */
    val featured: List<Teacher> = sample.take(4)

    /** The whole sample directory, directory previews only. */
    val teachers: List<Teacher> = sample
}
