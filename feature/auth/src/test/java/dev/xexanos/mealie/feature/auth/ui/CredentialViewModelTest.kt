package dev.xexanos.mealie.feature.auth.ui

import app.cash.turbine.test
import dev.xexanos.mealie.core.ui.R
import dev.xexanos.mealie.feature.auth.testutil.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class CredentialViewModelTest {

    // --- AC1: Initial State ---

    @Nested
    @DisplayName("AC1: Screen displays with empty fields and Sign In button")
    inner class InitialState {

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("init emits AwaitingInput with empty username and password")
        fun `init emits AwaitingInput with empty username and password`() = runTest {
            val vm = CredentialViewModel(FakeAuthRepository())
            val state = vm.uiState.value
            assertTrue(state is CredentialUiState.AwaitingInput)
            val input = state as CredentialUiState.AwaitingInput
            assertEquals("", input.username)
            assertEquals("", input.password)
            assertNull(input.errorResId)
            assertFalse(input.isSubmitting)
        }
    }

    // --- AC2: Empty field validation ---

    @Nested
    @DisplayName("AC2: Empty fields show validation error, no network call")
    inner class EmptyFieldValidation {

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("onSignIn with empty username and password shows validation error")
        fun `onSignIn with empty fields shows validation error`() = runTest {
            val fake = FakeAuthRepository()
            val vm = CredentialViewModel(fake)
            vm.onSignIn()
            val state = vm.uiState.value as CredentialUiState.AwaitingInput
            assertEquals(R.string.credential_error_empty, state.errorResId)
            assertEquals(0, fake.authenticateCallCount)
        }

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("onSignIn with empty password shows validation error")
        fun `onSignIn with only username shows validation error`() = runTest {
            val fake = FakeAuthRepository()
            val vm = CredentialViewModel(fake)
            vm.onUsernameChanged("user@example.com")
            vm.onSignIn()
            val state = vm.uiState.value as CredentialUiState.AwaitingInput
            assertEquals(R.string.credential_error_empty, state.errorResId)
            assertEquals(0, fake.authenticateCallCount)
        }

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("onSignIn with empty username shows validation error")
        fun `onSignIn with only password shows validation error`() = runTest {
            val fake = FakeAuthRepository()
            val vm = CredentialViewModel(fake)
            vm.onPasswordChanged("secret")
            vm.onSignIn()
            val state = vm.uiState.value as CredentialUiState.AwaitingInput
            assertEquals(R.string.credential_error_empty, state.errorResId)
            assertEquals(0, fake.authenticateCallCount)
        }
    }

    // --- AC3 + AC4: Successful authentication ---

    @Nested
    @DisplayName("AC3/AC4: Successful auth stores credentials and navigates")
    inner class SuccessfulAuth {

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("onSignIn with valid credentials emits NavigateToMain")
        fun `onSignIn with valid credentials emits NavigateToMain`() = runTest {
            val fake = FakeAuthRepository(authResult = AuthResult.Success)
            val vm = CredentialViewModel(fake)
            vm.onUsernameChanged("user@example.com")
            vm.onPasswordChanged("secret")
            vm.events.test {
                vm.onSignIn()
                assertEquals(CredentialUiEvent.NavigateToMain, awaitItem())
            }
        }

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("onSignIn sets isSubmitting true during network call")
        fun `onSignIn sets isSubmitting true during network call`() = runTest {
            val fake = FakeAuthRepository(authResult = AuthResult.Success)
            val vm = CredentialViewModel(fake)
            vm.onUsernameChanged("user@example.com")
            vm.onPasswordChanged("secret")
            vm.onSignIn()
            // After completion, isSubmitting should be false again
            val state = vm.uiState.value
            if (state is CredentialUiState.AwaitingInput) {
                assertFalse(state.isSubmitting)
            }
        }
    }

    // --- AC5: Invalid credentials (401) ---

    @Nested
    @DisplayName("AC5: Invalid credentials clears password, shows error")
    inner class InvalidCredentials {

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("401 clears password, retains username, shows error")
        fun `invalid credentials clears password retains username shows error`() = runTest {
            val fake = FakeAuthRepository(authResult = AuthResult.InvalidCredentials)
            val vm = CredentialViewModel(fake)
            vm.onUsernameChanged("user@example.com")
            vm.onPasswordChanged("wrong-password")
            vm.onSignIn()
            val state = vm.uiState.value as CredentialUiState.AwaitingInput
            assertEquals("user@example.com", state.username)
            assertEquals("", state.password)
            assertEquals(R.string.credential_error_invalid, state.errorResId)
            assertFalse(state.isSubmitting)
        }
    }

    // --- AC6: Network error ---

    @Nested
    @DisplayName("AC6: Network error retains both fields, shows error")
    inner class NetworkError {

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("network error retains username and password, shows network error")
        fun `network error retains both fields shows error`() = runTest {
            val fake = FakeAuthRepository(authResult = AuthResult.NetworkError)
            val vm = CredentialViewModel(fake)
            vm.onUsernameChanged("user@example.com")
            vm.onPasswordChanged("secret")
            vm.onSignIn()
            val state = vm.uiState.value as CredentialUiState.AwaitingInput
            assertEquals("user@example.com", state.username)
            assertEquals("secret", state.password)
            assertEquals(R.string.credential_error_network, state.errorResId)
            assertFalse(state.isSubmitting)
        }
    }

    // --- Edge Case: Double-tap guard ---

    @Nested
    @DisplayName("Edge: Double-tap guard prevents concurrent submissions")
    inner class DoubleTapGuard {

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("onSignIn while isSubmitting is a no-op")
        fun `onSignIn while isSubmitting is a no-op`() = runTest {
            val fake = FakeAuthRepository(authResult = AuthResult.Success)
            val vm = CredentialViewModel(fake)
            vm.onUsernameChanged("user@example.com")
            vm.onPasswordChanged("secret")
            // Simulate submitting state
            vm.onSignIn()
            vm.onSignIn() // second tap should be ignored
            assertEquals(1, fake.authenticateCallCount)
        }
    }

    // --- AC8: String resources (verify error states use @StringRes) ---

    @Nested
    @DisplayName("AC8: All error messages use @StringRes resource IDs")
    inner class StringResources {

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("validation error uses R.string.credential_error_empty")
        fun `validation error uses string resource ID`() = runTest {
            val vm = CredentialViewModel(FakeAuthRepository())
            vm.onSignIn()
            val state = vm.uiState.value as CredentialUiState.AwaitingInput
            assertEquals(R.string.credential_error_empty, state.errorResId)
        }

        @Test
        @Disabled("Red phase - CredentialViewModel not yet implemented")
        @DisplayName("auth error uses R.string.credential_error_invalid")
        fun `auth error uses string resource ID`() = runTest {
            val fake = FakeAuthRepository(authResult = AuthResult.InvalidCredentials)
            val vm = CredentialViewModel(fake)
            vm.onUsernameChanged("user")
            vm.onPasswordChanged("pass")
            vm.onSignIn()
            val state = vm.uiState.value as CredentialUiState.AwaitingInput
            assertEquals(R.string.credential_error_invalid, state.errorResId)
        }
    }
}
