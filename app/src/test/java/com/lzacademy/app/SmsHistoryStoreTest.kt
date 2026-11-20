package com.lzacademy.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lzacademy.app.data.preferences.PreparedSms
import com.lzacademy.app.data.preferences.SmsHistoryStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Local "prepared SMS" history: newest first, bounded, never claims a delivery. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmsHistoryStoreTest {

    private lateinit var store: SmsHistoryStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lz_academy_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = SmsHistoryStore(context)
    }

    @Test
    fun `conserve les vingt derniers du plus recent au plus ancien`() {
        (1..25).forEach {
            store.add(PreparedSms("id$it", it.toLong(), it, "message $it"))
        }
        val out = store.load()
        assertEquals(20, out.size)
        assertEquals("id25", out.first().id)
        assertEquals("id6", out.last().id)
        assertEquals(25, out.first().recipientCount)
    }

    @Test
    fun `un stockage vide renvoie une liste vide et un json casse est ignore`() {
        assertEquals(emptyList<PreparedSms>(), store.load())

        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("lz_academy_prefs", Context.MODE_PRIVATE)
            .edit().putString("prepared_sms_history", "{pas du json").commit()

        assertEquals(emptyList<PreparedSms>(), store.load())
    }
}
