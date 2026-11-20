package com.lzacademy.app.models

/**
 * The signed-in identity used by the UI. It is deliberately independent from Room so the local
 * authentication layer can later be replaced by Supabase Auth without touching the screens.
 */
data class AuthUser(
    val id: String,
    val email: String,
    val fullName: String,
    val initials: String,
    val role: UserRole,
    val linkedTeacherId: String? = null,
    val linkedStudentId: String? = null,
    val isActive: Boolean = true,
) {
    /** "Amine" for "Amine Khelifi"; used by the student greeting. */
    val firstName: String
        get() = fullName.trim().split(" ").firstOrNull().orEmpty().ifBlank { fullName }
}

sealed interface AuthState {
    /** Session is being read from local storage (very short, shows the splash). */
    data object Restoring : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

sealed interface LoginResult {
    data class Success(val user: AuthUser) : LoginResult
    data object InvalidCredentials : LoginResult
    data object DisabledAccount : LoginResult
}
