package com.lzacademy.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lzacademy.app.data.auth.AuthSeed
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.AuthState
import com.lzacademy.app.models.LoginResult
import com.lzacademy.app.models.UserRole
import com.lzacademy.app.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** Local authentication: seeding, PBKDF2 verification, persisted session and logout. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthRepositoryTest {

    private lateinit var database: LZDatabase
    private lateinit var preferences: AppPreferences
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LZDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = AppPreferences(context, "auth_test_${UUID.randomUUID()}")
        repository = AuthRepository(database, preferences)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `initialize seeds the three accounts and starts signed out`() = runBlocking {
        repository.initialize()

        assertEquals(3, repository.accountCount())
        assertEquals(AuthState.SignedOut, repository.state.value)
        assertNull(preferences.userId)
    }

    @Test
    fun `each demo account signs in with the right role`() = runBlocking {
        repository.initialize()

        val student = repository.login(AuthSeed.STUDENT_EMAIL, AuthSeed.DEMO_PASSWORD)
        assertTrue(student is LoginResult.Success)
        assertEquals(UserRole.Student, (student as LoginResult.Success).user.role)
        assertEquals("s-amine", student.user.linkedStudentId)

        repository.logout()
        val teacher = repository.login(AuthSeed.TEACHER_EMAIL, AuthSeed.DEMO_PASSWORD)
        assertEquals(UserRole.Teacher, (teacher as LoginResult.Success).user.role)
        assertEquals("t-karim", teacher.user.linkedTeacherId)

        repository.logout()
        val admin = repository.login(AuthSeed.ADMIN_EMAIL, AuthSeed.DEMO_PASSWORD)
        assertEquals(UserRole.Admin, (admin as LoginResult.Success).user.role)
    }

    @Test
    fun `a wrong password is rejected and keeps the app signed out`() = runBlocking {
        repository.initialize()

        val result = repository.login(AuthSeed.STUDENT_EMAIL, "wrong-password")

        assertEquals(LoginResult.InvalidCredentials, result)
        assertEquals(AuthState.SignedOut, repository.state.value)
        assertNull(preferences.userId)
    }

    @Test
    fun `the session is restored after a process restart`() = runBlocking {
        repository.initialize()
        repository.login(AuthSeed.TEACHER_EMAIL, AuthSeed.DEMO_PASSWORD)
        assertEquals("u-teacher", preferences.userId)

        // Same preferences, fresh repository: simulates a new process.
        val restarted = AuthRepository(database, preferences)
        restarted.initialize()

        val state = restarted.state.value
        assertTrue(state is AuthState.SignedIn)
        assertEquals("u-teacher", (state as AuthState.SignedIn).user.id)
    }

    @Test
    fun `logout clears the persisted session`() = runBlocking {
        repository.initialize()
        repository.login(AuthSeed.STUDENT_EMAIL, AuthSeed.DEMO_PASSWORD)

        repository.logout()

        assertEquals(AuthState.SignedOut, repository.state.value)
        assertNull(preferences.userId)
    }
}
