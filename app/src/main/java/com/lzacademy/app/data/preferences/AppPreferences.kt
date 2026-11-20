package com.lzacademy.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.lzacademy.app.theme.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Small persistent store shared by the whole app.
 *
 * It keeps the session (which local account is signed in) and the theme choice. Both survive
 * app restarts and process deaths, which is what makes the "light theme by default" and the
 * per-user authentication stable instead of a per-screen toggle.
 */
class AppPreferences(
    context: Context,
    name: String = DEFAULT_NAME,
) {
    private val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    /** Signed-in local account, or `null` when nobody is signed in. */
    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_USER_ID) else putString(KEY_USER_ID, value)
            }.apply()
        }

    /** Light is the product default, even when the device runs in dark mode. */
    var themeMode: ThemeMode
        get() {
            val raw = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.Light
            return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.Light)
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    /** Supabase access token of the signed-in account (null when never synced). */
    var supabaseToken: String?
        get() = prefs.getString(KEY_SUPABASE_TOKEN, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_SUPABASE_TOKEN) else putString(KEY_SUPABASE_TOKEN, value)
            }.apply()
        }

    /** Matricule of the student linked to the signed-in account (own profile + photo). */
    var ownMatricule: String?
        get() = prefs.getString(KEY_OWN_MATRICULE, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_OWN_MATRICULE) else putString(KEY_OWN_MATRICULE, value)
            }.apply()
        }

    /** Supabase profile uuid of the signed-in account (links a student or a teacher row). */
    var ownProfileId: String?
        get() = prefs.getString(KEY_OWN_PROFILE_ID, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_OWN_PROFILE_ID) else putString(KEY_OWN_PROFILE_ID, value)
            }.apply()
        }

    // ---- Profil admin (100 % local : SharedPreferences + fichier photo interne) ----

    /** Nom complet affiché sur le profil admin (null = "Admin"). */
    var adminName: String?
        get() = prefs.getString(KEY_ADMIN_NAME, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_ADMIN_NAME) else putString(KEY_ADMIN_NAME, value)
            }.apply()
        }

    /** Fonction / titre (ex. « Directeur pédagogique »). */
    var adminTitle: String?
        get() = prefs.getString(KEY_ADMIN_TITLE, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_ADMIN_TITLE) else putString(KEY_ADMIN_TITLE, value)
            }.apply()
        }

    /** Téléphone personnel de l'admin. */
    var adminPhone: String?
        get() = prefs.getString(KEY_ADMIN_PHONE, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_ADMIN_PHONE) else putString(KEY_ADMIN_PHONE, value)
            }.apply()
        }

    /** Email personnel de l'admin. */
    var adminEmail: String?
        get() = prefs.getString(KEY_ADMIN_EMAIL, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_ADMIN_EMAIL) else putString(KEY_ADMIN_EMAIL, value)
            }.apply()
        }

    /** Chemin local du fichier photo de profil (filesDir), ou null. */
    var adminPhotoPath: String?
        get() = prefs.getString(KEY_ADMIN_PHOTO_PATH, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_ADMIN_PHOTO_PATH) else putString(KEY_ADMIN_PHOTO_PATH, value)
            }.apply()
        }

    /** Public URL of the signed-in student's avatar (written by the student dashboard). */
    var ownAvatarUrl: String?
        get() = prefs.getString(KEY_OWN_AVATAR_URL, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_OWN_AVATAR_URL) else putString(KEY_OWN_AVATAR_URL, value)
            }.apply()
        }

    /** Supabase refresh token — renews the access token without a full password login. */
    var supabaseRefreshToken: String?
        get() = prefs.getString(KEY_SUPABASE_REFRESH, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_SUPABASE_REFRESH) else putString(KEY_SUPABASE_REFRESH, value)
            }.apply()
        }

    // ---- Notifications locales (une clé par interrupteur de l'écran Paramètres) ----

    /** Current value of one Settings switch, falling back to its catalogue default. */
    fun notificationEnabled(id: String, default: Boolean = true): Boolean =
        prefs.getBoolean(notificationKey(id), default)

    fun setNotificationEnabled(id: String, enabled: Boolean) {
        prefs.edit().putBoolean(notificationKey(id), enabled).apply()
    }

    /**
     * Same value, but reactive: a switch flipped in Settings reaches the screens that honour it
     * (student dashboard badge, admin alerts) without restarting the app.
     */
    fun notificationFlow(id: String, default: Boolean = true): Flow<Boolean> = callbackFlow {
        trySend(notificationEnabled(id, default))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == notificationKey(id)) trySend(notificationEnabled(id, default))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun notificationKey(id: String) = "notif_$id"

    /** Epoch millis of the last successful full sync; gates auto-syncs (free-tier friendly). */
    var lastSyncAtMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC_AT, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_SYNC_AT, value).apply()
        }

    companion object {
        const val DEFAULT_NAME = "lz_academy_prefs"
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SUPABASE_TOKEN = "supabase_access_token"
        private const val KEY_SUPABASE_REFRESH = "supabase_refresh_token"
        private const val KEY_OWN_MATRICULE = "supabase_own_matricule"
        private const val KEY_OWN_PROFILE_ID = "supabase_own_profile_id"
        private const val KEY_LAST_SYNC_AT = "supabase_last_sync_at"
        private const val KEY_ADMIN_NAME = "admin_profile_name"
        private const val KEY_ADMIN_TITLE = "admin_profile_title"
        private const val KEY_ADMIN_PHONE = "admin_profile_phone"
        private const val KEY_ADMIN_EMAIL = "admin_profile_email"
        private const val KEY_ADMIN_PHOTO_PATH = "admin_profile_photo_path"
        private const val KEY_OWN_AVATAR_URL = "own_avatar_url"
    }
}
