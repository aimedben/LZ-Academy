package com.lzacademy.app

import android.view.ViewGroup
import com.example.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Startup smoke test: the real entry point of the APK is created, the Compose theme is applied
 * and the navigation graph is composed. It catches wiring errors that pure unit tests miss
 * (missing route, broken theme, crash in a ViewModel initialiser).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class AppLaunchTest {

    @Test(timeout = 120_000)
    fun `the app starts without crashing and renders its first screen`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()

        assertFalse("the activity must not finish during startup", controller.get().isFinishing)

        val content = controller.get().findViewById<ViewGroup>(android.R.id.content)
        assertTrue("the Compose tree must be attached to the activity", content.childCount > 0)
    }
}
