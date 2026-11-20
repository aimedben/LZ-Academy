package com.lzacademy.app.repository

import com.lzacademy.app.data.auth.AuthDao
import com.lzacademy.app.data.auth.AuthSeed
import com.lzacademy.app.data.auth.PasswordHasher
import com.lzacademy.app.data.auth.toDomain
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.AuthState
import com.lzacademy.app.models.LoginResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Local authentication for the three roles.
 *
 * Flow: [initialize] seeds the accounts and restores the persisted session; [login] verifies the
 * PBKDF2 hash and persists the selected account; [logout] clears the session. The role of the
 * signed-in user drives the whole app, so a student can never reach the teacher or admin tabs
 * without signing in with the corresponding account.
 */
class AuthRepository(
    database: LZDatabase,
    private val preferences: AppPreferences,
) {
    private val dao: AuthDao = database.authDao()

    private val _state = MutableStateFlow<AuthState>(AuthState.Restoring)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val seedMutex = Mutex()

    @Volatile
    private var seeded = false

    suspend fun initialize() {
        runCatching { seedIfNeeded() }
        val userId = preferences.userId
        val user = runCatching {
            userId?.let { dao.byId(it)?.toDomain() }
        }.getOrNull()

        if (user != null && user.isActive) {
            _state.value = AuthState.SignedIn(user)
        } else {
            preferences.userId = null
            _state.value = AuthState.SignedOut
        }
    }

    suspend fun seedIfNeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            val accounts = withContext(Dispatchers.IO) { AuthSeed.accounts }
            dao.seed(accounts)
            seeded = true
        }
    }

    suspend fun login(email: String, password: String): LoginResult {
        seedIfNeeded()
        val normalized = email.trim().lowercase()
        if (normalized.isBlank() || password.isBlank()) return LoginResult.InvalidCredentials

        val row = dao.byEmail(normalized) ?: return LoginResult.InvalidCredentials
        val valid = withContext(Dispatchers.IO) {
            PasswordHasher.verify(password, row.passwordSalt, row.passwordHash)
        }
        if (!valid) return LoginResult.InvalidCredentials
        if (!row.isActive) return LoginResult.DisabledAccount

        val user = row.toDomain()
        preferences.userId = user.id
        _state.value = AuthState.SignedIn(user)
        return LoginResult.Success(user)
    }

    fun logout() {
        preferences.userId = null
        _state.value = AuthState.SignedOut
    }

    /** Test/diagnostic helper: number of seeded accounts. */
    suspend fun accountCount(): Int = dao.count()
}
