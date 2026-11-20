package com.lzacademy.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import com.lzacademy.app.data.supabase.SyncedSubscriptionEntity
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.AttendanceOutcome
import com.lzacademy.app.models.AttendanceStatus
import com.lzacademy.app.models.AttendanceUndo
import com.lzacademy.app.models.RenewalResult
import com.lzacademy.app.repository.AttendanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The rules that decide whether a presence consumes a session: decrement, idempotence,
 * non-consuming statuses, exhaustion, renewal and refund on cancellation. The roster and the
 * counters come from the Supabase mirror — like the real academy (abonnement 4 séances).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceRepositoryTest {

    private lateinit var database: LZDatabase
    private lateinit var repository: AttendanceRepository
    private val today: LocalDate = LocalDate.now()
    private val session = AttendanceRepository.DEFAULT_SESSION_LABEL

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LZDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AttendanceRepository(database)
        // simulate a successful Supabase sync: mirror students + subscriptions, then hydrate
        database.syncDao().upsertStudents(
            listOf(
                student("r1", "LZ-001", "Amine Khelifi"),
                student("r2", "LZ-002", "Rayan Bensalem"),
                student("r3", "LZ-003", "Yacine Merad"),
            ),
        )
        database.syncDao().upsertSubscriptions(
            listOf(
                SyncedSubscriptionEntity("sb1", "LZ-001", 4, 0, 4, "active"),
                SyncedSubscriptionEntity("sb2", "LZ-002", 4, 0, 4, "active"),
                SyncedSubscriptionEntity("sb3", "LZ-003", 4, 4, 0, "active"), // la réelle épuisée
            ),
        )
        repository.hydrateSubscriptions()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun student(remoteId: String, matricule: String, fullName: String) = SyncedStudentEntity(
        remoteId = remoteId,
        matricule = matricule,
        fullName = fullName,
        phone = null,
        levelName = null,
        avatarPath = null,
        isActive = true,
        profileId = null,
        createdAt = "2026-07-10T10:00:00+00:00",
    )

    private suspend fun remaining(studentId: String): Int =
        repository.subscriptionOf(studentId)?.remainingSessions ?: -1

    @Test
    fun `the roster comes from the mirror keyed by matricule`() = runBlocking {
        val roster = repository.observeRoster(today, session).first()
        assertEquals(listOf("LZ-001", "LZ-002", "LZ-003"), roster.map { it.student.id })
        assertEquals(0, roster.first { it.student.id == "LZ-003" }.subscription.remainingSessions)
    }

    @Test
    fun `a present mark decrements the pack exactly once`() = runBlocking {
        assertEquals(4, remaining("LZ-001"))

        val first = repository.record("LZ-001", today, session, AttendanceStatus.Present)
        assertTrue(first is AttendanceOutcome.Recorded)
        assertEquals(3, remaining("LZ-001"))

        val second = repository.record("LZ-001", today, session, AttendanceStatus.Present)
        assertEquals(AttendanceOutcome.AlreadyRecorded, second)
        assertEquals(3, remaining("LZ-001"))

        assertEquals(1, repository.observeDay(today, session).first().size)
    }

    @Test
    fun `late consumes a session but absent and justified refund it`() = runBlocking {
        repository.record("LZ-002", today, session, AttendanceStatus.Late)
        assertEquals(3, remaining("LZ-002"))

        // switching to a non-consuming status refunds the session
        repository.record("LZ-002", today, session, AttendanceStatus.Absent)
        assertEquals(4, remaining("LZ-002"))

        repository.record("LZ-002", today, session, AttendanceStatus.Justified)
        assertEquals(4, remaining("LZ-002"))
    }

    @Test
    fun `an exhausted pack refuses a presence until the administration renews`() = runBlocking {
        assertEquals(0, remaining("LZ-003"))

        val refused = repository.record("LZ-003", today, session, AttendanceStatus.Present)
        assertTrue(refused is AttendanceOutcome.Exhausted)
        assertEquals(0, remaining("LZ-003"))
        assertTrue(repository.observeDay(today, session).first().isEmpty())

        val renewed = repository.renew("LZ-003")
        assertTrue(renewed is RenewalResult.Renewed)
        assertEquals(4, remaining("LZ-003"))

        val accepted = repository.record("LZ-003", today, session, AttendanceStatus.Present)
        assertTrue(accepted is AttendanceOutcome.Recorded)
        assertEquals(3, remaining("LZ-003"))
    }

    @Test
    fun `cancelling a consuming mark refunds the session`() = runBlocking {
        val outcome = repository.record("LZ-001", today, session, AttendanceStatus.Present)
        val record = (outcome as AttendanceOutcome.Recorded).record
        assertEquals(3, remaining("LZ-001"))

        val undo = repository.undo(record.id)

        assertTrue(undo is AttendanceUndo.Refunded)
        assertEquals(4, remaining("LZ-001"))
        assertTrue(repository.observeDay(today, session).first().isEmpty())
    }

    @Test
    fun `switching present to absent refunds without exceeding the pack`() = runBlocking {
        repository.record("LZ-001", today, session, AttendanceStatus.Present)
        assertEquals(3, remaining("LZ-001"))

        repository.record("LZ-001", today, session, AttendanceStatus.Absent)

        assertEquals(4, remaining("LZ-001"))
    }

    @Test
    fun `renewal resets the counter to the pack size and increments the renewal count`() = runBlocking {
        repository.record("LZ-001", today, session, AttendanceStatus.Present)
        repository.record("LZ-001", today, session + "-bis", AttendanceStatus.Present)
        assertEquals(2, remaining("LZ-001"))

        val renewal = repository.renew("LZ-001")

        assertTrue(renewal is RenewalResult.Renewed)
        val subscription = (renewal as RenewalResult.Renewed).subscription
        assertEquals(4, subscription.remainingSessions)
        assertEquals(4, subscription.totalSessions)
        // hydration starts the ledger at 0 renewals — the first renew makes 1
        assertEquals(1, subscription.renewalCount)
    }

    @Test
    fun `hydration never resets a counter already consumed locally`() = runBlocking {
        repository.record("LZ-001", today, session, AttendanceStatus.Present)
        assertEquals(3, remaining("LZ-001"))

        repository.hydrateSubscriptions()

        assertEquals("le compteur local reste maître", 3, remaining("LZ-001"))
    }
}
