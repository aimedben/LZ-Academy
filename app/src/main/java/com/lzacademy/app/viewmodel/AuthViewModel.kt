package com.lzacademy.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.data.teachers.LZDatabase
import com.lzacademy.app.models.AuthState
import com.lzacademy.app.models.LoginResult
import com.lzacademy.app.repository.AuthRepository
import com.lzacademy.app.repository.SupabaseSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the login screen shows while a sign-in is in progress. */
data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * Session owner of the app. [state] is `Restoring` during the very short local read, then
 * `SignedOut` or `SignedIn`; `LZApp` switches on it to choose between the login screen and the
 * role experience. The theme mode is also persisted here so it survives restarts.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val repository = AuthRepository(LZDatabase.get(application), preferences)
    private val syncRepository = SupabaseSyncRepository(LZDatabase.get(application), preferences)

    val state: StateFlow<AuthState> = repository.state

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    init {
        viewModelScope.launch { repository.initialize() }
        // Silent refresh on relaunch when a Supabase token is already stored.
        viewModelScope.launch(Dispatchers.IO) { runCatching { syncRepository.refresh() } }
    }

    fun login(email: String, password: String) {
        if (_loginState.value.loading) return
        _loginState.value = LoginUiState(loading = true)
        viewModelScope.launch {
            when (repository.login(email, password)) {
                is LoginResult.Success -> {
                    _loginState.value = LoginUiState()
                    // Fire-and-forget: cache the Supabase academy database, the app
                    // stays usable even when the device is offline.
                    launch(Dispatchers.IO) { runCatching { syncRepository.loginAndSync(email, password) } }
                }
                LoginResult.InvalidCredentials -> _loginState.value = LoginUiState(
                    error = "E-mail ou mot de passe incorrect.",
                )
                LoginResult.DisabledAccount -> _loginState.value = LoginUiState(
                    error = "Ce compte est désactivé. Contactez l'administration.",
                )
            }
        }
    }

    fun clearError() {
        if (_loginState.value.error != null) {
            _loginState.value = _loginState.value.copy(error = null)
        }
    }

    fun logout() {
        repository.logout()
        _loginState.value = LoginUiState()
    }
}
