package com.lzacademy.app.data.auth

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lzacademy.app.models.AuthUser
import com.lzacademy.app.models.UserRole

/**
 * A local account of the app. Each role (student, teacher, administrator) signs in with its own
 * e-mail and password. Passwords are never stored in clear text: only a salted PBKDF2 hash is
 * persisted.
 *
 * Supabase Auth will replace this table later; the rest of the app already depends on the
 * [AuthUser] domain model, not on this entity, so the switch will not leak into the screens.
 */
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val initials: String,
    val role: String,
    val passwordHash: String,
    val passwordSalt: String,
    val linkedTeacherId: String?,
    val linkedStudentId: String?,
    val isActive: Boolean,
)

internal fun UserEntity.toDomain(): AuthUser = AuthUser(
    id = id,
    email = email,
    fullName = fullName,
    initials = initials,
    role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.Student),
    linkedTeacherId = linkedTeacherId,
    linkedStudentId = linkedStudentId,
    isActive = isActive,
)
