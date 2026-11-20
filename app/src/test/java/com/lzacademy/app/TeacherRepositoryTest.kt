package com.lzacademy.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.data.teachers.TeacherEntity
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.models.TeacherMessageStatus
import com.lzacademy.app.repository.SendMessageResult
import com.lzacademy.app.repository.TeacherRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Functional tests of the student-home showcase, the rotation and the local message queue.
 *
 * No seeding exists anymore: the directory is fed exclusively by [TeacherRepository.upsertMirrorTeachers]
 * (called by the Supabase sync), so the tests simulate a real mirror payload.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TeacherRepositoryTest {

    private lateinit var database: LZDatabase
    private lateinit var repository: TeacherRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LZDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TeacherRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** 18 featured teachers named t-01..t-18 (+2 inactive), like a real mirror payload. */
    private fun mirrorPayload(): List<TeacherEntity> {
        val featured = (1..18).map { i ->
            entity("t-%02d".format(i), featured = true, accepts = i % 4 != 0)
        }
        val inactive = listOf(
            entity("t-inactif-1", featured = false, accepts = true, isActive = false),
            entity("t-inactif-2", featured = false, accepts = false, isActive = false),
        )
        return featured + inactive
    }

    private fun entity(
        id: String,
        featured: Boolean,
        accepts: Boolean,
        isActive: Boolean = true,
        profileId: String? = null,
    ) = TeacherEntity(
        id = id,
        fullName = "Prof $id",
        photoUrl = null,
        subjects = "Mathématiques",
        professionalDescription = "",
        isFeaturedOnStudentHome = featured,
        acceptsStudentMessages = accepts,
        featuredRotationPriority = 0,
        lastFeaturedAtEpochDay = null,
        levels = "",
        yearsOfExperience = 5,
        groups = 0,
        isActive = isActive,
        profileId = profileId,
    )

    private fun featureOn(epochDay: Long): List<Teacher> = runBlocking {
        repository.observeFeatured(today = { LocalDate.ofEpochDay(epochDay) }).first()
    }

    @Test
    fun `the mirror feeds the directory with the real teachers`() = runBlocking {
        repository.upsertMirrorTeachers(mirrorPayload())
        val teachers = repository.observeTeachers().first()

        // the directory view exposes ACTIVE teachers only — inactive mirror rows stay hidden
        assertEquals(18, teachers.size)
        assertEquals(18, teachers.count { it.isActive })
        assertEquals(18, teachers.count { it.isFeaturedOnStudentHome })
        assertTrue(teachers.any { it.id == "t-01" })
    }

    @Test
    fun `the page of the day holds four distinct featured teachers and stays stable`() = runBlocking {
        repository.upsertMirrorTeachers(mirrorPayload())
        val day = LocalDate.of(2026, 11, 10).toEpochDay()

        val first = featureOn(day)
        val second = featureOn(day)

        assertEquals(4, first.size)
        assertEquals(first.size, first.map(Teacher::id).distinct().size)
        assertTrue(first.all { it.isFeaturedOnStudentHome })
        assertEquals(first.map(Teacher::id), second.map(Teacher::id))
    }

    @Test
    fun `rotation is fair and never repeats a teacher inside one page`() = runBlocking {
        repository.upsertMirrorTeachers(mirrorPayload())
        val start = LocalDate.of(2026, 11, 10).toEpochDay()
        val pages = (0L until 9L).map { offset -> featureOn(start + offset).map(Teacher::id) }

        pages.forEach { page ->
            assertEquals("a page always holds 4 teachers", 4, page.size)
            assertEquals("no duplicate inside a page", page.size, page.distinct().size)
        }

        // 18 eligible teachers, 4 per day: everybody must appear within nine days.
        assertEquals(18, pages.flatten().distinct().size)
    }

    @Test
    fun `a teacher who opts out disappears immediately and is replaced`() = runBlocking {
        repository.upsertMirrorTeachers(mirrorPayload())
        val day = LocalDate.of(2026, 11, 10).toEpochDay()
        val page = featureOn(day)
        val optedOut = page.first()

        repository.setShowcaseVisibility(optedOut.id, false)

        val after = featureOn(day)
        assertEquals(4, after.size)
        assertFalse(after.any { it.id == optedOut.id })
        assertEquals(3, after.count { teacher -> page.any { it.id == teacher.id } })
    }

    @Test
    fun `showcase visibility and message availability are independent`() = runBlocking {
        repository.upsertMirrorTeachers(mirrorPayload())
        val teacherId = "t-01"

        repository.setAcceptsMessages(teacherId, false)
        var teacher = repository.observeTeacher(teacherId).first()!!
        assertTrue(teacher.isFeaturedOnStudentHome)
        assertFalse(teacher.acceptsStudentMessages)

        repository.setShowcaseVisibility(teacherId, false)
        repository.setAcceptsMessages(teacherId, true)
        teacher = repository.observeTeacher(teacherId).first()!!
        assertFalse(teacher.isFeaturedOnStudentHome)
        assertTrue(teacher.acceptsStudentMessages)
    }

    @Test
    fun `messages are refused for an offline teacher and queued as pending otherwise`() = runBlocking {
        repository.upsertMirrorTeachers(mirrorPayload())

        val offline = "t-04" // accepts = (4 % 4 != 0) → false
        assertEquals(SendMessageResult.Unavailable, repository.sendMessage(offline, "Bonjour"))
        assertTrue(repository.observeMessages(offline).first().isEmpty())

        val online = "t-01"
        assertEquals(SendMessageResult.Sent, repository.sendMessage(online, "  Bonjour  "))
        val messages = repository.observeMessages(online).first()
        assertEquals(1, messages.size)
        assertEquals("Bonjour", messages.first().body)
        assertEquals(TeacherMessageStatus.Pending, messages.first().status)
        assertEquals(1, repository.observeMessageCount(online).first())

        assertEquals(
            "blank messages are ignored",
            SendMessageResult.Empty,
            repository.sendMessage(online, "   "),
        )
    }

    @Test
    fun `a student can send at most three messages per day to one teacher`() = runBlocking {
        repository.upsertMirrorTeachers(mirrorPayload())
        val online = "t-01"

        repeat(TeacherRepository.MAX_MESSAGES_PER_DAY) { index ->
            assertEquals(SendMessageResult.Sent, repository.sendMessage(online, "Message $index"))
        }
        assertEquals(
            SendMessageResult.DailyLimitReached,
            repository.sendMessage(online, "Trop tard"),
        )
        assertEquals(
            TeacherRepository.MAX_MESSAGES_PER_DAY,
            repository.observeMessageCount(online).first(),
        )
    }

    @Test
    fun `a teacher is resolved from its supabase profile`() = runBlocking {
        repository.upsertMirrorTeachers(listOf(entity("t-01", featured = true, accepts = true, profileId = "prof-1")))

        val teacher = repository.observeTeacherByProfile("prof-1").first()
        assertEquals("t-01", teacher?.id)

        assertNull(repository.observeTeacherByProfile("unknown").first())
        assertNull(repository.observeTeacherByProfile(null).first())
    }
}
