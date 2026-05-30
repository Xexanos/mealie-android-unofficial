package dev.xexanos.mealie.e2e

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.xexanos.mealie.feature.auth.ui.HttpWarningCheckTestTags
import dev.xexanos.mealie.feature.auth.ui.ServerUrlTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before

abstract class E2ETestBase {

    protected val backend: E2EBackend by lazy { E2EBackend.fromInstrumentationArgs() }

    @Before
    fun assumeBackendReachable() {
        if (backend is E2EBackend.Live) {
            val reachable = runBlocking { backend.healthCheck() }
            Assume.assumeTrue("Live backend unreachable - skipping", reachable)
        }
    }

    protected fun assumeWireMockOnly() {
        Assume.assumeTrue(
            "Test requires WireMock backend - skipping in live mode",
            backend is E2EBackend.WireMock,
        )
    }

    protected fun navigateToServerUrlScreen(
        composeTestRule: AndroidComposeTestRule<*, *>,
        wireMock: WireMockRule,
    ) {
        wireMock.stubAppAboutSuccess()
        composeTestRule.waitForNode(ServerUrlTestTags.URL_TEXT_FIELD)
        composeTestRule.onNodeWithTag(ServerUrlTestTags.URL_TEXT_FIELD)
            .performTextInput(wireMock.baseUrl)
        composeTestRule.onNodeWithTag(ServerUrlTestTags.CONNECT_BUTTON)
            .performClick()
    }

    protected fun navigatePastHttpWarning(
        composeTestRule: AndroidComposeTestRule<*, *>,
    ) {
        composeTestRule.waitForNode(HttpWarningCheckTestTags.CONTINUE_BUTTON)
        composeTestRule.onNodeWithTag(HttpWarningCheckTestTags.CONTINUE_BUTTON)
            .performClick()
    }

    companion object {
        const val TIMEOUT_SHORT = 5_000L
        const val TIMEOUT_MEDIUM = 10_000L
        const val TIMEOUT_LONG = 15_000L
    }
}
