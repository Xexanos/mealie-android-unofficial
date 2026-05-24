package dev.xexanos.mealie.core.ui.navigation

import app.cash.turbine.test
import dev.xexanos.mealie.core.ui.testutil.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MainDispatcherExtension::class)
class NavigationManagerTest {

    private val manager = NavigationManager()
    private object TestCommand : NavigationCommand

    @Test
    fun `navigate emits command to collector`() = runTest {
        manager.commands.test {
            manager.navigate(TestCommand)
            assertEquals(TestCommand, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple commands emitted in order`() = runTest {
        val second = object : NavigationCommand {}
        manager.commands.test {
            manager.navigate(TestCommand)
            manager.navigate(second)
            assertEquals(TestCommand, awaitItem())
            assertEquals(second, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
