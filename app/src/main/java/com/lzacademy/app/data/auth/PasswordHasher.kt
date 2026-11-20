package com.lzacademy.app.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2-HMAC-SHA256 password hashing.
 *
 * This is the same idea as a backend password store: a random salt per account and a slow hash.
 * The demo iterations count keeps the first launch fast on low-end phones while still avoiding
 * any clear-text password at rest.
 */
object PasswordHasher {

    private const val ITERATIONS = 60_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder().decode(salt),
            ITERATIONS,
            KEY_LENGTH_BITS,
        )
        return try {
            val key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            Base64.getEncoder().encodeToString(key)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(password, salt)
        return MessageDigest.isEqual(
            Base64.getDecoder().decode(actual),
            Base64.getDecoder().decode(expectedHash),
        )
    }
}
