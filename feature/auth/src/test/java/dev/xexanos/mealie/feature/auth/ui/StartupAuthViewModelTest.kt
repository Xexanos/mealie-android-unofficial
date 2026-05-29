package dev.xexanos.mealie.feature.auth.ui

import app.cash.turbine.test
import dev.xexanos.mealie.core.data.domain.StartupAuthResult
import dev.xexanos.mealie.feature.auth.testutil.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
@DisplayName("StartupAuthViewModel Tests")
class StartupAuthViewModelTest {

    @Nested
    @DisplayName("AC2/AC4: Refresh or re-auth succeeds")
    inner class NavigateToMain {

        @Test
        @DisplayName("[P0] Success result emits NavigateToMain event")
        fun `whenUseCaseReturnsSuccess thenEmitsNavigateToMain`() = runTest {
            val fakeUseCase = FakeStartupAuthUseCase(result = StartupAuthResult.Success)
            val vm = StartupAuthViewModel(fakeUseCase)

            vm.event.test {
                assertEquals(StartupAuthEvent.NavigateToMain, awaitItem())
            }
        }
    }

    @Nested
    @DisplayName("AC7: Device is offline")
    inner class NavigateToMainOffline {

        @Test
        @DisplayName("[P1] Offline result emits NavigateToMain")
        fun `whenUseCaseReturnsOffline thenEmitsNavigateToMain`() = runTest {
            val fakeUseCase = FakeStartupAuthUseCase(result = StartupAuthResult.Offline)
            val vm = StartupAuthViewModel(fakeUseCase)

            vm.event.test {
                assertEquals(StartupAuthEvent.NavigateToMain, awaitItem())
            }
        }
    }

    @Nested
    @DisplayName("AC5: Both auth attempts fail")
    inner class NavigateToCredentials {

        @Test
        @DisplayName("[P1] CredentialsInvalid result emits NavigateToCredentials")
        fun `whenUseCaseReturnsCredentialsInvalid thenEmitsNavigateToCredentials`() = runTest {
            val fakeUseCase = FakeStartupAuthUseCase(result = StartupAuthResult.CredentialsInvalid)
            val vm = StartupAuthViewModel(fakeUseCase)

            vm.event.test {
                assertEquals(StartupAuthEvent.NavigateToCredentials, awaitItem())
            }
        }
    }

    @Nested
    @DisplayName("AC6: No credentials stored")
    inner class NavigateToSetup {

        @Test
        @DisplayName("[P1] NoCredentials result emits NavigateToSetup")
        fun `whenUseCaseReturnsNoCredentials thenEmitsNavigateToSetup`() = runTest {
            val fakeUseCase = FakeStartupAuthUseCase(result = StartupAuthResult.NoCredentials)
            val vm = StartupAuthViewModel(fakeUseCase)

            vm.event.test {
                assertEquals(StartupAuthEvent.NavigateToSetup, awaitItem())
            }
        }
    }
}
