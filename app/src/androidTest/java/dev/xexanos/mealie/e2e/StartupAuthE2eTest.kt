package dev.xexanos.mealie.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dev.xexanos.mealie.MainActivity
import dev.xexanos.mealie.feature.auth.ui.CredentialTestTags
import dev.xexanos.mealie.feature.auth.ui.ServerUrlTestTags
import dev.xexanos.mealie.feature.auth.ui.StartupTestTags
import dev.xexanos.mealie.navigation.PostAuthTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupAuthE2eTest : E2ETestBase() {

    @get:Rule(order = 0)
    val wireMock = WireMockRule()

    @get:Rule(order = 1)
    val startupState = StartupStateRule(wireMock)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun skipInLiveMode() {
        assumeWireMockOnly()
    }

    @Test
    fun whenNoCredentialsStored_thenNavigatesToServerUrlScreen() {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
            .assertIsDisplayed()
    }

    @Test
    @WithStoredAuth(
        serverUrl = "http://10.0.2.2:8080",
        username = "test@example.com",
        password = "test-password",
        token = "existing-token-123",
        refreshStatus = 200,
    )
    fun whenRefreshSucceeds_thenNavigatesToMain() {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(PostAuthTestTags.CONTAINER)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(PostAuthTestTags.CONTAINER)
            .assertIsDisplayed()
    }

    @Test
    @WithStoredAuth(
        serverUrl = "http://10.0.2.2:8080",
        username = "test@example.com",
        password = "test-password",
        token = "expired-token-456",
        refreshStatus = 401,
        authStatus = 200,
    )
    fun whenRefreshFailsAndReAuthSucceeds_thenNavigatesToMain() {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(PostAuthTestTags.CONTAINER)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(PostAuthTestTags.CONTAINER)
            .assertIsDisplayed()
    }

    @Test
    @WithStoredAuth(
        serverUrl = "http://10.0.2.2:8080",
        username = "test@example.com",
        password = "test-password",
        token = "expired-token-789",
        refreshStatus = 401,
        authStatus = 401,
    )
    fun whenBothFail_thenNavigatesToCredentialScreen() {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithTag(CredentialTestTags.USERNAME_TEXT_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(CredentialTestTags.USERNAME_TEXT_FIELD)
            .assertIsDisplayed()
    }

    @Test
    @WithStoredAuth(
        serverUrl = "http://10.0.2.2:8080",
        username = "test@example.com",
        password = "test-password",
        token = "existing-token-slow",
        refreshStatus = 200,
        refreshDelayMs = 3000,
    )
    fun whenRefreshInProgress_thenShowsLoadingIndicator() {
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithTag(StartupTestTags.PROGRESS_INDICATOR)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(StartupTestTags.PROGRESS_INDICATOR)
            .assertIsDisplayed()
    }
}
