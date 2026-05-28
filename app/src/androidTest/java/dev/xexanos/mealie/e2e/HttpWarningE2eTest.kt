package dev.xexanos.mealie.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.xexanos.mealie.MainActivity
import dev.xexanos.mealie.feature.auth.ui.HttpWarningCheckTestTags
import dev.xexanos.mealie.feature.auth.ui.ServerUrlTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HttpWarningE2eTest {

    @get:Rule(order = 0)
    val wireMock = WireMockRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun navigatePastServerUrl() {
        wireMock.stubAppAboutSuccess()
        composeTestRule.onNodeWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
            .performTextInput(wireMock.baseUrl)
        composeTestRule.onNodeWithTag(ServerUrlTestTags.CONNECT_BUTTON)
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag(HttpWarningCheckTestTags.SERVER_URL_TEXT)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun whenHttpServerValid_thenShowsWarningWithUrl() {
        navigatePastServerUrl()

        composeTestRule.onNodeWithTag(HttpWarningCheckTestTags.SERVER_URL_TEXT)
            .assertIsDisplayed()
            .assertTextEquals(wireMock.baseUrl)
        composeTestRule.onNodeWithTag(HttpWarningCheckTestTags.WARNING_MESSAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(HttpWarningCheckTestTags.CONTINUE_BUTTON)
            .assertIsDisplayed()
    }
}
