package dev.xexanos.mealie.feature.auth.ui

import app.cash.turbine.test
import dev.xexanos.mealie.feature.auth.testutil.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class HttpWarningCheckViewModelTest {

    @Test
    fun `init with HTTPS URL emits NavigateToCredentials immediately`() = runTest {
        val fake = FakeAuthRepository(storedUrl = "https://mealie.example.com")
        val vm = HttpWarningCheckViewModel(fake)
        vm.events.test {
            assertEquals(HttpWarningCheckUiEvent.NavigateToCredentials, awaitItem())
        }
    }

    @Test
    fun `init with HTTP URL already acknowledged emits NavigateToCredentials`() = runTest {
        val fake = FakeAuthRepository(
            storedUrl = "http://192.168.1.100:9925",
            ackedUrls = setOf("http://192.168.1.100:9925"),
        )
        val vm = HttpWarningCheckViewModel(fake)
        vm.events.test {
            assertEquals(HttpWarningCheckUiEvent.NavigateToCredentials, awaitItem())
        }
    }

    @Test
    fun `init with HTTP URL not acknowledged shows warning`() = runTest {
        val fake = FakeAuthRepository(storedUrl = "http://192.168.1.100:9925")
        val vm = HttpWarningCheckViewModel(fake)
        val state = vm.uiState.value
        assertTrue(state is HttpWarningCheckUiState.ShowWarning)
        assertEquals(
            "http://192.168.1.100:9925",
            (state as HttpWarningCheckUiState.ShowWarning).serverUrl,
        )
    }

    @Test
    fun `onContinue acknowledges URL and emits NavigateToCredentials`() = runTest {
        val fake = FakeAuthRepository(storedUrl = "http://192.168.1.100:9925")
        val vm = HttpWarningCheckViewModel(fake)
        vm.events.test {
            vm.onContinue()
            assertEquals(HttpWarningCheckUiEvent.NavigateToCredentials, awaitItem())
        }
        assertTrue(fake.isHttpWarningAcknowledged("http://192.168.1.100:9925"))
    }

    @Test
    fun `different HTTP URL still shows warning even if previous URL was acked`() = runTest {
        val fake = FakeAuthRepository(
            storedUrl = "http://10.0.0.5:9000",
            ackedUrls = setOf("http://192.168.1.100:9925"),
        )
        val vm = HttpWarningCheckViewModel(fake)
        val state = vm.uiState.value
        assertTrue(state is HttpWarningCheckUiState.ShowWarning)
    }
}
