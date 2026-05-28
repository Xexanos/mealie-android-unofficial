package dev.xexanos.mealie.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.xexanos.mealie.MainActivity
import dev.xexanos.mealie.feature.auth.ui.CredentialTestTags
import dev.xexanos.mealie.feature.auth.ui.HttpWarningCheckTestTags
import dev.xexanos.mealie.feature.auth.ui.ServerUrlTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CredentialE2eTest : E2ETestBase() {

    @get:Rule(order = 0)
    val wireMock = WireMockRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun navigateToCredentialScreen() {
        if (backend is E2EBackend.WireMock) {
            wireMock.stubAppAboutSuccess()
        }

        composeTestRule.onNodeWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
            .performTextInput(backend.baseUrl)
        composeTestRule.onNodeWithTag(ServerUrlTestTags.CONNECT_BUTTON)
            .performClick()

        if (!backend.isHttps) {
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithTag(HttpWarningCheckTestTags.CONTINUE_BUTTON)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag(HttpWarningCheckTestTags.CONTINUE_BUTTON)
                .performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(CredentialTestTags.USERNAME_TEXT_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun whenValidCredentials_thenNavigatesAway() {
        if (backend is E2EBackend.WireMock) {
            wireMock.stubAuthSuccess()
        }
        navigateToCredentialScreen()

        composeTestRule.onNodeWithTag(CredentialTestTags.USERNAME_TEXT_FIELD)
            .performTextInput(backend.username)
        composeTestRule.onNodeWithTag(CredentialTestTags.PASSWORD_TEXT_FIELD)
            .performTextInput(backend.password)
        composeTestRule.onNodeWithTag(CredentialTestTags.SIGN_IN_BUTTON)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(CredentialTestTags.USERNAME_TEXT_FIELD)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun whenInvalidCredentials_thenShowsError() {
        if (backend is E2EBackend.WireMock) {
            wireMock.stubAuthUnauthorized()
        }
        navigateToCredentialScreen()

        composeTestRule.onNodeWithTag(CredentialTestTags.USERNAME_TEXT_FIELD)
            .performTextInput("invalid@example.com")
        composeTestRule.onNodeWithTag(CredentialTestTags.PASSWORD_TEXT_FIELD)
            .performTextInput("wrong-password")
        composeTestRule.onNodeWithTag(CredentialTestTags.SIGN_IN_BUTTON)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(CredentialTestTags.ERROR_TEXT)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(CredentialTestTags.ERROR_TEXT)
            .assertIsDisplayed()
    }

    @Test
    fun whenEmptyFields_thenShowsValidationError() {
        navigateToCredentialScreen()

        composeTestRule.onNodeWithTag(CredentialTestTags.SIGN_IN_BUTTON)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(CredentialTestTags.ERROR_TEXT)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(CredentialTestTags.ERROR_TEXT)
            .assertIsDisplayed()
    }
}
