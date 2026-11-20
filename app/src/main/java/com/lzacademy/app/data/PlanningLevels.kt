package com.lzacademy.app.data

import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.models.LessonUi
import com.lzacademy.app.models.WeekDayUi

/**
 * One level of the academy with its own weekly schedule. The planning screen shows a level
 * filter (BAC • 3AS, 2AS, 1AS, BEM) and the PDF export renders the selected level.
 */
data class PlanningLevelUi(
    val id: String,
    val chipLabel: String,
    val sectionLabel: String,
    val days: List<WeekDayUi>,
    val lessonsByDay: Map<Int, List<LessonUi>>,
) {
    /** Linear list of the week's sessions, in day then time order — used by the PDF export. */
    val allLessons: List<LessonUi>
        get() = days.flatMap { day -> lessonsByDay[day.index].orEmpty().sortedBy { it.start } }

    val totalSessions: Int get() = allLessons.size
}

object PlanningLevels {

    fun all(today: java.time.LocalDate): List<PlanningLevelUi> {
        val weekStart = today.minusDays(((today.dayOfWeek.value - 1).toLong()))
        val days = (0..5).map { i ->
            WeekDayUi(i, FrenchDates.dayShort[i], weekStart.plusDays(i.toLong()).dayOfMonth)
        }

        fun lesson(
            day: Int, id: String, subject: String, start: String, end: String,
            room: String, person: String, accent: LessonAccent,
        ) = LessonUi(id, subject, start, end, room, person, accent)

        fun lessons(vararg pairs: Pair<Int, List<LessonUi>>): Map<Int, List<LessonUi>> =
            (0..5).associateWith { day -> pairs.firstOrNull { it.first == day }?.second ?: emptyList() }

        val bac = lessons(
            0 to listOf(
                lesson(0, "bac-l1", "Mathématiques", "16:00", "18:00", "Salle A1", "M. Lebbah", LessonAccent.Green),
                lesson(0, "bac-l2", "Physique-Chimie", "18:15", "20:15", "Salle B2", "M. Said", LessonAccent.Violet),
            ),
            1 to listOf(
                lesson(1, "bac-l3", "Français", "14:00", "16:00", "Salle A1", "Mme Haddad", LessonAccent.Orange),
            ),
            2 to listOf(
                lesson(2, "bac-l4", "Mathématiques", "16:00", "18:00", "Salle A1", "M. Lebbah", LessonAccent.Green),
                lesson(2, "bac-l5", "SVT", "18:15", "20:15", "Salle C3", "Mme Ouali", LessonAccent.Blue),
            ),
            3 to listOf(
                lesson(3, "bac-l6", "Physique-Chimie", "16:00", "18:00", "Salle B2", "M. Said", LessonAccent.Violet),
            ),
            5 to listOf(
                lesson(5, "bac-l7", "Mathématiques", "10:00", "12:00", "Salle A1", "M. Lebbah", LessonAccent.Green),
                lesson(5, "bac-l8", "Français", "13:00", "15:00", "Salle A1", "Mme Haddad", LessonAccent.Orange),
            ),
        )

        val deuxAs = lessons(
            0 to listOf(
                lesson(0, "2as-l1", "Mathématiques", "16:00", "18:00", "Salle D4", "M. Bellabès", LessonAccent.Green),
            ),
            1 to listOf(
                lesson(1, "2as-l2", "Physique-Chimie", "15:00", "17:00", "Salle B2", "M. Said", LessonAccent.Violet),
                lesson(1, "2as-l3", "Anglais", "17:15", "18:45", "Salle 103", "Mme Riad", LessonAccent.Blue),
            ),
            3 to listOf(
                lesson(3, "2as-l4", "SVT", "16:00", "18:00", "Salle C3", "Mme Ouali", LessonAccent.Orange),
            ),
            4 to listOf(
                lesson(4, "2as-l5", "Mathématiques", "16:00", "18:00", "Salle D4", "M. Bellabès", LessonAccent.Green),
            ),
            5 to listOf(
                lesson(5, "2as-l6", "Physique-Chimie", "10:00", "12:00", "Salle B2", "M. Said", LessonAccent.Violet),
            ),
        )

        val uneAs = lessons(
            1 to listOf(
                lesson(1, "1as-l1", "Mathématiques", "16:00", "18:00", "Salle 105", "M. Meziane", LessonAccent.Green),
            ),
            2 to listOf(
                lesson(2, "1as-l2", "Français", "14:00", "15:30", "Salle A1", "Mme Haddad", LessonAccent.Orange),
                lesson(2, "1as-l3", "Informatique", "16:00", "17:30", "Salle Info", "M. Karim", LessonAccent.Blue),
            ),
            4 to listOf(
                lesson(4, "1as-l4", "Physique-Chimie", "16:00", "18:00", "Salle B2", "M. Said", LessonAccent.Violet),
            ),
            5 to listOf(
                lesson(5, "1as-l5", "Mathématiques", "10:00", "12:00", "Salle 105", "M. Meziane", LessonAccent.Green),
                lesson(5, "1as-l6", "SVT", "13:00", "14:30", "Salle C3", "Mme Ouali", LessonAccent.Orange),
            ),
        )

        val bem = lessons(
            0 to listOf(
                lesson(0, "bem-l1", "Mathématiques", "14:00", "16:00", "Salle E2", "M. Meziane", LessonAccent.Green),
            ),
            2 to listOf(
                lesson(2, "bem-l2", "SVT", "14:00", "16:00", "Salle C3", "Mme Ouali", LessonAccent.Orange),
            ),
            3 to listOf(
                lesson(3, "bem-l3", "Français", "14:00", "15:30", "Salle A1", "Mme Haddad", LessonAccent.Orange),
            ),
            5 to listOf(
                lesson(5, "bem-l4", "Mathématiques", "09:00", "11:00", "Salle E2", "M. Meziane", LessonAccent.Green),
                lesson(5, "bem-l5", "Physique-Chimie", "11:15", "12:45", "Salle B2", "M. Said", LessonAccent.Violet),
            ),
        )

        return listOf(
            PlanningLevelUi("BAC", "BAC • 3AS", "Terminale • BAC 2026", days, bac),
            PlanningLevelUi("2AS", "2AS", "Deuxième année secondaire", days, deuxAs),
            PlanningLevelUi("1AS", "1AS", "Première année secondaire", days, uneAs),
            PlanningLevelUi("BEM", "BEM • 4AM", "Moyenne • BEM", days, bem),
        )
    }

    /** Human title of the level used by the PDF export and the viewer header. */
    fun titleOf(level: PlanningLevelUi): String = "${level.sectionLabel} — Semaine du ${level.days.firstOrNull()?.number ?: ""} ${FrenchDates.monthShort[java.time.LocalDate.now().monthValue - 1]}"
}
