package dev.xexanos.mealie.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag

fun AndroidComposeTestRule<*, *>.waitForNode(
    tag: String,
    timeoutMillis: Long = E2ETestBase.TIMEOUT_MEDIUM,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

fun AndroidComposeTestRule<*, *>.waitForNodeAbsent(
    tag: String,
    timeoutMillis: Long = E2ETestBase.TIMEOUT_MEDIUM,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
    }
}

fun AndroidComposeTestRule<*, *>.waitForNodeAndAssert(
    tag: String,
    timeoutMillis: Long = E2ETestBase.TIMEOUT_MEDIUM,
) {
    waitForNode(tag, timeoutMillis)
    onNodeWithTag(tag).assertIsDisplayed()
}
