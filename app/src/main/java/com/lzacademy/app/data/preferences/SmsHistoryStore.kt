package com.lzacademy.app.data.preferences

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One SMS "prepared" from the admin screen. The app never sends anything itself:
 * the Messages app is opened prefilled and the admin presses Send, so this record
 * only claims preparation — never delivery.
 */
data class PreparedSms(
    val id: String,
    val atMillis: Long,
    val recipientCount: Int,
    val preview: String,
)

/** Bounded local history (20 entries, newest first) stored as JSON in SharedPreferences. */
class SmsHistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(AppPreferences.DEFAULT_NAME, Context.MODE_PRIVATE)

    fun load(): List<PreparedSms> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id")
                if (id.isEmpty()) return@mapNotNull null
                PreparedSms(
                    id = id,
                    atMillis = item.optLong("atMillis"),
                    recipientCount = item.optInt("recipientCount"),
                    preview = item.optString("preview"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun add(entry: PreparedSms) {
        val merged = (listOf(entry) + load()).distinctBy { it.id }.take(MAX_ENTRIES)
        val array = JSONArray()
        merged.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("atMillis", it.atMillis)
                    .put("recipientCount", it.recipientCount)
                    .put("preview", it.preview),
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val KEY = "prepared_sms_history"
        private const val MAX_ENTRIES = 20
    }
}
