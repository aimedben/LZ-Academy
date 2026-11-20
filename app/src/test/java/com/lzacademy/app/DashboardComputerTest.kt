package com.lzacademy.app

import com.lzacademy.app.data.supabase.SyncedAnnouncementEntity
import com.lzacademy.app.data.supabase.SyncedAttendanceEntity
import com.lzacademy.app.data.supabase.SyncedEnrollmentEntity
import com.lzacademy.app.data.supabase.SyncedGroupEntity
import com.lzacademy.app.data.supabase.SyncedMaterialEntity
import com.lzacademy.app.data.supabase.SyncedPaymentEntity
import com.lzacademy.app.data.supabase.SyncedSessionEntity
import com.lzacademy.app.data.supabase.SyncedSlotEntity
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.data.supabase.SyncedSubjectEntity
import com.lzacademy.app.data.supabase.SyncedSubscriptionEntity
import com.lzacademy.app.data.supabase.SyncedTeacherEntity
import com.lzacademy.app.models.TimelineState
import com.lzacademy.app.repository.DashboardComputer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Pure computations behind the real dashboards (no Compose, no Android): the numbers the
 * admin/student/teacher screens must show, derived ONLY from the Supabase mirror entities.
 */
class DashboardComputerTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 13) // vendredi
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 11, 13, 10, 0, 0, 0, ZoneOffset.UTC)

    private fun student(
        id: String,
        matricule: String = id,
        fullName: String = "Élève $id",
        levelName: String? = null,
        createdAt: String = "2026-07-10T10:00:00+00:00",
        profileId: String? = null,
    ) = SyncedStudentEntity(
        remoteId = id, matricule = matricule, fullName = fullName, phone = null,
        levelName = levelName, avatarPath = null, isActive = true, profileId = profileId,
        createdAt = createdAt,
    )

    private fun payment(
        id: String,
        studentId: String,
        amount: Double,
        status: String,
        createdAt: String,
        paidAt: String? = null,
    ) = SyncedPaymentEntity(
        remoteId = id, studentId = studentId, amountDzd = amount, method = "cash",
        status = status, reference = "REF-$id", dueOn = null, paidAtIso = paidAt,
        createdAtIso = createdAt,
    )

    private fun session(
        id: String,
        groupId: String,
        teacherId: String,
        startsAt: String,
        endsAt: String,
        status: String = "scheduled",
        subject: String = "Mathématiques",
    ) = SyncedSessionEntity(
        remoteId = id, groupId = groupId, groupName = "Groupe $groupId", teacherId = teacherId,
        teacherName = "Prof $teacherId", subjectName = subject, roomName = "Salle 1",
        startsAtIso = startsAt, endsAtIso = endsAt, status = status, topic = "Séance $id",
    )

    private fun subscription(id: String, remaining: Int, status: String = "active") =
        SyncedSubscriptionEntity(id, "M-$id", 48, 48 - remaining, remaining, status)

    // ------------------------------------------------------------ admin

    @Test
    fun `admin computes revenue, alerts and series from the mirror only`() {
        val students = listOf(
            student("s1", levelName = "3AS", createdAt = "2026-07-10T10:00:00+00:00"),
            student("s2", levelName = "3AS", createdAt = "2026-10-02T10:00:00+00:00"),
            student("s3", levelName = null, createdAt = "2025-05-01T10:00:00+00:00"),
        )
        val subscriptions = listOf(
            subscription("sub1", remaining = 10),
            subscription("sub2", remaining = 20),
            subscription("sub3", remaining = 0, status = "expired"),
        )
        val payments = listOf(
            payment("p1", "s1", 6000.0, "paid", "2026-11-05T10:00:00+00:00", paidAt = "2026-11-05T12:00:00+00:00"),
            payment("p2", "s2", 9000.0, "paid", "2026-10-01T10:00:00+00:00", paidAt = "2026-10-02T12:00:00+00:00"),
            payment("p3", "s3", 15000.0, "pending", "2026-11-01T10:00:00+00:00"),
        )
        val n = DashboardComputer.admin(
            students, subscriptions,
            teachers = listOf(teacher("t1"), teacher("t2")),
            groups = listOf(group("g1"), group("g2"), group("g3")),
            payments = payments,
            sessions = emptyList(), enrollments = emptyList(),
            today = today,
        )

        // revenus réels : payés du mois courant vs précédent
        assertEquals(6000.0, n.monthRevenue, 0.01)
        assertEquals(9000.0, n.prevMonthRevenue, 0.01)
        assertEquals(1, n.pendingPayments)
        assertEquals(2, n.activeSubscriptions)
        assertEquals(1, n.exhaustedSubscriptions)
        assertEquals(2, n.teachers)
        assertEquals(3, n.groups)
        // série 12 mois (déc. 2025..nov. 2026) : cumul réel des élèves créés (s2 créé le 02/10)
        assertEquals(
            listOf(1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3),
            n.monthlyNewStudents,
        )
        // série des payés : octobre 9000, novembre 6000, zéros ailleurs
        assertEquals(listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9000.0, 6000.0), n.monthlyRevenue)
        // répartition par niveau réelle, « Non affecté » pour level_id NULL
        assertEquals(listOf("3AS" to 2, "Non affecté" to 1), n.studentsByLevel)
        // paiements récents triés du plus récent
        assertEquals(listOf("p1", "p3", "p2"), n.recentPayments.map { it.id })
        assertEquals("Élève s1", n.recentPayments.first().studentName)
    }

    @Test
    fun `admin keeps at most five recent payments`() {
        val payments = (1..7).map { i ->
            payment("p$i", "s1", i * 1000.0, "paid", "2026-11-%02dT10:00:00+00:00".format(i))
        }
        val n = DashboardComputer.admin(
            listOf(student("s1")), emptyList(), teachers = emptyList(), groups = emptyList(),
            payments = payments, sessions = emptyList(), enrollments = emptyList(), today = today,
        )
        assertEquals(5, n.recentPayments.size)
        assertEquals(listOf("p7", "p6", "p5", "p4", "p3"), n.recentPayments.map { it.id })
    }

    // ------------------------------------------------------------ student

    @Test
    fun `student without enrollment shows honest empty schedule`() {
        val own = student("s1", matricule = "LZ-895", fullName = "Amine Khelifi", profileId = "prof-1")
        val n = DashboardComputer.student(
            own = own,
            subscription = SyncedSubscriptionEntity("sub1", "LZ-895", 48, 12, 36, "active"),
            subjects = listOf(SyncedSubjectEntity("subj1", "MATH", "Mathématiques")),
            materials = listOf(
                SyncedMaterialEntity("m1", "Cours", null, "pdf", "path", 100, "subj1", "Groupe 1", true, null),
                SyncedMaterialEntity("m2", "TD", null, "pdf", "path", 100, "subj1", "Groupe 1", true, null),
            ),
            teachers = emptyList(),
            slots = listOf(slot("sl1", "g1", weekday = 1)),
            sessions = listOf(session("se1", "g1", "t1", "2026-11-14 15:00:00+00", "2026-11-14 17:00:00+00")),
            enrollments = emptyList(),
            now = now,
        )
        assertEquals("Amine", n.firstName)
        assertEquals("AK", n.initials)
        assertEquals(36, n.remaining)
        assertEquals(48, n.total)
        assertEquals(1, n.activeSubjects)
        assertNull(n.nextLesson)
        assertTrue(n.lessonsByDay.isEmpty())
    }

    @Test
    fun `student schedule comes from enrolled groups`() {
        val own = student("s1", matricule = "LZ-001")
        val n = DashboardComputer.student(
            own = own,
            subscription = SyncedSubscriptionEntity("sub1", "LZ-001", 48, 0, 48, "active"),
            subjects = emptyList(),
            materials = emptyList(),
            teachers = listOf(teacher("t1")),
            slots = listOf(slot("sl1", "g1", weekday = 1, start = "15:00:00", end = "17:00:00")),
            sessions = listOf(
                session("se2", "g1", "t1", "2026-11-16 15:00:00+00", "2026-11-16 17:00:00+00"),
                session("se1", "g1", "t1", "2026-11-14 15:00:00+00", "2026-11-14 17:00:00+00"),
            ),
            enrollments = listOf(SyncedEnrollmentEntity("e1", "s1", "g1", "2026-10-10", null)),
            now = now,
        )
        // la prochaine séance = la plus proche dans le futur (samedi 14, pas lundi 16)
        assertEquals("Mathématiques", n.nextLesson?.subject)
        assertEquals("Prof t1", n.nextLesson?.teacher)
        assertTrue(n.nextLesson!!.whenLabel.contains("Demain"))
        // créneau hebdo du groupe inscrit : lundi 15:00–17:00
        assertEquals(listOf("15:00"), n.lessonsByDay[1]?.map { it.start })
        assertEquals("17:00", n.lessonsByDay[1]?.first()?.end)
        assertEquals("Prof t1", n.lessonsByDay[1]?.first()?.person)
    }

    // ------------------------------------------------------------ teacher

    @Test
    fun `teacher numbers come from own sessions and groups`() {
        val t1 = teacher("t1", profileId = "prof-teacher")
        val n = DashboardComputer.teacher(
            teacher = t1,
            groups = listOf(group("g1", teacherId = "t1"), group("g2", teacherId = "t2")),
            sessions = listOf(
                session("seA", "g1", "t1", "2026-11-13 09:00:00+00", "2026-11-13 11:00:00+00", status = "completed"),
                session("seB", "g1", "t1", "2026-11-13 15:00:00+00", "2026-11-13 17:00:00+00"),
                session("seC", "g1", "t1", "2026-11-14 15:00:00+00", "2026-11-14 17:00:00+00"),
                session("seD", "g2", "t2", "2026-11-13 15:00:00+00", "2026-11-13 17:00:00+00"),
            ),
            enrollments = listOf(
                SyncedEnrollmentEntity("e1", "s1", "g1", "2026-10-10", null),
                SyncedEnrollmentEntity("e2", "s2", "g1", "2026-10-10", "2026-11-01"),
                SyncedEnrollmentEntity("e3", "s3", "g2", "2026-10-10", null),
            ),
            attendance = emptyList(),
            now = now,
        )
        assertEquals(2, n.sessionsToday)      // les 2 séances du groupe g1 aujourd'hui
        assertEquals(1, n.groups)             // seul g1 appartient à t1
        assertEquals(1, n.students)           // 1 inscription active (e2 a quitté, e3 autre groupe)
        assertEquals("Séance seB", n.nextSession?.title) // la prochaine future
        assertEquals(2, n.todaySessions.size)
        assertEquals(TimelineState.Done, n.todaySessions.first().state)
        assertEquals(4.0f, n.weeklyHours[4], 0.01f) // vendredi (index 4) : 2h + 2h
        assertEquals(0.0f, n.weeklyHours[0], 0.01f)
        // planning hebdo réel : vendredi (ISO 5) porte les 2 séances de g1, samedi (ISO 6) seC
        assertEquals(2, n.sessionsByWeekDay[5]?.size)
        assertEquals("Séance seC", n.sessionsByWeekDay[6]?.first()?.title)
        assertNull(n.sessionsByWeekDay[1]) // lundi : rien
    }

    @Test
    fun `null teacher shows empty honest numbers`() {
        val n = DashboardComputer.teacher(
            teacher = null, groups = emptyList(), sessions = emptyList(),
            enrollments = emptyList(), attendance = emptyList(), now = now,
        )
        assertEquals(0, n.sessionsToday)
        assertNull(n.nextSession)
        assertTrue(n.todaySessions.isEmpty())
    }

    // ------------------------------------------------------------ helpers

    private fun teacher(id: String, profileId: String? = null) = SyncedTeacherEntity(
        remoteId = id, fullName = "Prof $id", photoUrl = null, description = null,
        yearsOfExperience = 5, isFeatured = true, acceptsMessages = true, rotationPriority = 0,
        lastFeaturedEpochDay = null, isActive = true, profileId = profileId, subjects = "Mathématiques",
    )

    private fun group(id: String, teacherId: String? = "t1") = SyncedGroupEntity(
        remoteId = id, name = "Groupe $id", levelName = "3AS", teacherId = teacherId, capacity = 20,
    )

    private fun slot(id: String, groupId: String, weekday: Int, start: String = "15:00", end: String = "17:00") =
        SyncedSlotEntity(
            remoteId = id, groupId = groupId, groupName = "Groupe $groupId", weekday = weekday,
            start = start, end = end, roomName = "Salle 1",
        )
}
