package com.lzacademy.app

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.lzacademy.app.ui.admin.AdminSmsContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The stateless SMS composer, rendered without ViewModel or device:
 * an empty mirror must show an honest empty state and a disabled send button.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminSmsContentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `miroir vide affiche un etat vide et desactive l envoi`() {
        compose.setContent {
            AdminSmsContent(
                recipients = emptyList(),
                levels = emptyList(),
                history = emptyList(),
                body = "Rappel : cours maintenu",
                onBodyChange = {},
                query = "",
                onQueryChange = {},
                level = null,
                onLevelChange = {},
                selected = emptySet(),
                onToggle = {},
                onSelectAll = {},
                onOpenMessages = {},
            )
        }
        compose.onNodeWithTag("admin_sms_list")
            .performScrollToNode(hasText("Aucun élève synchronisé"))
        compose.onNodeWithText("Aucun élève synchronisé").assertExists()

        compose.onNodeWithTag("admin_sms_list")
            .performScrollToNode(hasText("Ouvrir Messages (0)"))
        compose.onNodeWithText("Ouvrir Messages (0)").assertIsNotEnabled()
    }
}
