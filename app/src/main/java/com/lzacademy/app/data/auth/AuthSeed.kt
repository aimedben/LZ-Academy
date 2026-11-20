package com.lzacademy.app.data.auth

import com.lzacademy.app.models.UserRole

/**
 * The three local demo accounts, one per role. They exist so the three experiences can be tested
 * exactly as they will be with real accounts; the password is hashed with a random salt on seed.
 */
object AuthSeed {

    const val DEMO_PASSWORD = "LZ2026!"

    const val STUDENT_EMAIL = "student@lzacademy.dz"
    const val TEACHER_EMAIL = "teacher@lzacademy.dz"
    const val ADMIN_EMAIL = "admin@lzacademy.dz"

    private val cached: List<UserEntity> by lazy { buildAccounts() }

    val accounts: List<UserEntity> get() = cached

    private fun buildAccounts(): List<UserEntity> = listOf(
        account(
            id = "u-student",
            email = STUDENT_EMAIL,
            fullName = "Amine Khelifi",
            initials = "AK",
            role = UserRole.Student,
            linkedStudentId = "s-amine",
        ),
        account(
            id = "u-teacher",
            email = TEACHER_EMAIL,
            fullName = "M. Karim Lebbah",
            initials = "KL",
            role = UserRole.Teacher,
            linkedTeacherId = "t-karim",
        ),
        account(
            id = "u-admin",
            email = ADMIN_EMAIL,
            fullName = "Admin LZ Academy",
            initials = "AD",
            role = UserRole.Admin,
        ),
    )

    private fun account(
        id: String,
        email: String,
        fullName: String,
        initials: String,
        role: UserRole,
        linkedTeacherId: String? = null,
        linkedStudentId: String? = null,
    ): UserEntity {
        val salt = PasswordHasher.newSalt()
        return UserEntity(
            id = id,
            email = email,
            fullName = fullName,
            initials = initials,
            role = role.name,
            passwordHash = PasswordHasher.hash(DEMO_PASSWORD, salt),
            passwordSalt = salt,
            linkedTeacherId = linkedTeacherId,
            linkedStudentId = linkedStudentId,
            isActive = true,
        )
    }
}
