package dev.xexanos.mealie.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.xexanos.mealie.MainActivity
import dev.xexanos.mealie.feature.auth.ui.HttpWarningCheckTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HttpWarningE2eTest : E2ETestBase() {

    @get:Rule(order = 0)
    val wireMock = WireMockRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun skipInLiveMode() {
        assumeWireMockOnly()
    }

    @Test
    fun whenHttpServerValid_thenShowsWarningWithUrl() {
        navigateToServerUrlScreen(composeTestRule, wireMock)
        composeTestRule.waitForNode(HttpWarningCheckTestTags.SERVER_URL_TEXT)

        composeTestRule.onNodeWithTag(HttpWarningCheckTestTags.SERVER_URL_TEXT)
            .assertIsDisplayed()
            .assertTextEquals(wireMock.baseUrl)
        composeTestRule.onNodeWithTag(HttpWarningCheckTestTags.WARNING_MESSAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(HttpWarningCheckTestTags.CONTINUE_BUTTON)
            .assertIsDisplayed()
    }
}
