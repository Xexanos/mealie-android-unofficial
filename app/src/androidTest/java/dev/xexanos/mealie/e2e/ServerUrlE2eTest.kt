package dev.xexanos.mealie.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.xexanos.mealie.MainActivity
import dev.xexanos.mealie.feature.auth.ui.ServerUrlTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ServerUrlE2eTest : E2ETestBase() {

    @get:Rule(order = 0)
    val wireMock = WireMockRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun skipInLiveMode() {
        assumeWireMockOnly()
    }

    @Test
    fun whenServerReturnsOldVersion_thenShowsNotMealieError() {
        wireMock.stubAppAboutNotMealie()

        composeTestRule.onNodeWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
            .performTextInput(wireMock.baseUrl)
        composeTestRule.onNodeWithTag(ServerUrlTestTags.CONNECT_BUTTON)
            .performClick()

        composeTestRule.waitForNode(ServerUrlTestTags.ERROR_TEXT)
        composeTestRule.onNodeWithText("Not a Mealie server")
            .assertIsDisplayed()
    }

    @Test
    fun whenServerUnreachable_thenShowsNetworkError() {
        composeTestRule.onNodeWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
            .performTextInput("http://10.0.2.2:9999")
        composeTestRule.onNodeWithTag(ServerUrlTestTags.CONNECT_BUTTON)
            .performClick()

        composeTestRule.waitForNode(ServerUrlTestTags.ERROR_TEXT, TIMEOUT_LONG)
        composeTestRule.onNodeWithText("Could not reach server")
            .assertIsDisplayed()
    }

    @Test
    fun whenEmptyUrlSubmitted_thenShowsValidationError() {
        composeTestRule.onNodeWithTag(ServerUrlTestTags.CONNECT_BUTTON)
            .performClick()

        composeTestRule.waitForNode(ServerUrlTestTags.ERROR_TEXT, TIMEOUT_SHORT)
        composeTestRule.onNodeWithText("Enter a valid URL (e.g. https://mealie.example.com)")
            .assertIsDisplayed()
    }

    @Test
    fun whenProbing_thenShowsLoadingAndDisablesInput() {
        wireMock.stubAppAboutWithDelay(3000)

        composeTestRule.onNodeWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
            .performTextInput(wireMock.baseUrl)
        composeTestRule.onNodeWithTag(ServerUrlTestTags.CONNECT_BUTTON)
            .performClick()

        composeTestRule.waitForNode(ServerUrlTestTags.PROGRESS_INDICATOR, TIMEOUT_SHORT)
        composeTestRule.onNodeWithTag(ServerUrlTestTags.PROGRESS_INDICATOR)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(ServerUrlTestTags.CONNECT_BUTTON)
            .assertIsNotEnabled()
    }
}
