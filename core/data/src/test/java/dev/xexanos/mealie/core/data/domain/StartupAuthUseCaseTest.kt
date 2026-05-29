package dev.xexanos.mealie.core.data.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("StartupAuthUseCase Tests")
class StartupAuthUseCaseTest {

    private lateinit var fakeTokenStore: FakeTokenProvider
    private lateinit var fakeCredentialStore: FakeCredentialProvider
    private lateinit var fakeAuthRepository: FakeStartupAuthRepository
    private lateinit var useCase: StartupAuthUseCase

    @BeforeEach
    fun setup() {
        fakeTokenStore = FakeTokenProvider()
        fakeCredentialStore = FakeCredentialProvider()
        fakeAuthRepository = FakeStartupAuthRepository()
        useCase = StartupAuthUseCase(fakeTokenStore, fakeCredentialStore, fakeAuthRepository)
    }

    @Nested
    @DisplayName("AC1/AC2: Token refresh succeeds")
    inner class RefreshSuccess {

        @Test
        @DisplayName("[P0] stored token exists and refresh succeeds - returns Success")
        fun `whenStoredTokenExistsAndRefreshSucceeds thenReturnsSuccess`() = runTest {
            fakeTokenStore.storedToken = "existing-token"
            fakeCredentialStore.storedCredentials = "user" to "pass"
            fakeAuthRepository.refreshResult = AuthResult.Success

            val result = useCase.execute()

            assertEquals(StartupAuthResult.Success, result)
        }

        @Test
        @DisplayName("[P0] refresh success calls refreshToken on repository")
        fun `whenRefreshSucceeds thenRepositoryRefreshCalled`() = runTest {
            fakeTokenStore.storedToken = "old-token"
            fakeCredentialStore.storedCredentials = "user" to "pass"
            fakeAuthRepository.refreshResult = AuthResult.Success

            useCase.execute()

            assertEquals(1, fakeAuthRepository.refreshCallCount)
        }
    }

    @Nested
    @DisplayName("AC3/AC4: Refresh 401 then credential re-auth succeeds")
    inner class FallbackReAuthSuccess {

        @Test
        @DisplayName("[P0] refresh returns 401, re-auth succeeds - returns Success")
        fun `whenRefresh401AndReAuthSucceeds thenReturnsSuccess`() = runTest {
            fakeTokenStore.storedToken = "expired-token"
            fakeCredentialStore.storedCredentials = "user" to "pass"
            fakeAuthRepository.refreshResult = AuthResult.InvalidCredentials
            fakeAuthRepository.reAuthResult = AuthResult.Success

            val result = useCase.execute()

            assertEquals(StartupAuthResult.Success, result)
        }

        @Test
        @DisplayName("[P0] re-auth success calls reAuthenticate on repository")
        fun `whenReAuthSucceeds thenRepositoryReAuthCalled`() = runTest {
            fakeTokenStore.storedToken = "expired-token"
            fakeCredentialStore.storedCredentials = "user" to "pass"
            fakeAuthRepository.refreshResult = AuthResult.InvalidCredentials
            fakeAuthRepository.reAuthResult = AuthResult.Success

            useCase.execute()

            assertEquals(1, fakeAuthRepository.reAuthCallCount)
        }
    }

    @Nested
    @DisplayName("AC5: Both refresh and re-auth fail")
    inner class BothFail {

        @Test
        @DisplayName("[P1] both fail - returns CredentialsInvalid")
        fun `whenBothFail thenReturnsCredentialsInvalid`() = runTest {
            fakeTokenStore.storedToken = "expired-token"
            fakeCredentialStore.storedCredentials = "user" to "changed-pass"
            fakeAuthRepository.refreshResult = AuthResult.InvalidCredentials
            fakeAuthRepository.reAuthResult = AuthResult.InvalidCredentials

            val result = useCase.execute()

            assertEquals(StartupAuthResult.CredentialsInvalid, result)
        }

        @Test
        @DisplayName("[P1] both fail - clears stored token")
        fun `whenBothFail thenClearsToken`() = runTest {
            fakeTokenStore.storedToken = "expired-token"
            fakeCredentialStore.storedCredentials = "user" to "changed-pass"
            fakeAuthRepository.refreshResult = AuthResult.InvalidCredentials
            fakeAuthRepository.reAuthResult = AuthResult.InvalidCredentials

            useCase.execute()

            assertEquals(1, fakeTokenStore.clearTokenCallCount)
        }

        @Test
        @DisplayName("[P1] both fail - does NOT clear credentials")
        fun `whenBothFail thenDoesNotClearCredentials`() = runTest {
            fakeTokenStore.storedToken = "expired-token"
            fakeCredentialStore.storedCredentials = "user" to "pass"
            fakeAuthRepository.refreshResult = AuthResult.InvalidCredentials
            fakeAuthRepository.reAuthResult = AuthResult.InvalidCredentials

            useCase.execute()

            assertEquals(0, fakeCredentialStore.clearCredentialsCallCount)
        }
    }

    @Nested
    @DisplayName("AC6: No credentials stored")
    inner class NoCredentials {

        @Test
        @DisplayName("[P1] no credentials stored - returns NoCredentials immediately")
        fun `whenNoCredentialsStored thenReturnsNoCredentials`() = runTest {
            fakeTokenStore.storedToken = ""
            fakeCredentialStore.storedCredentials = null

            val result = useCase.execute()

            assertEquals(StartupAuthResult.NoCredentials, result)
        }

        @Test
        @DisplayName("[P1] no credentials - skips both refresh and re-auth")
        fun `whenNoCredentials thenSkipsNetworkCalls`() = runTest {
            fakeTokenStore.storedToken = ""
            fakeCredentialStore.storedCredentials = null

            useCase.execute()

            assertEquals(0, fakeAuthRepository.refreshCallCount)
            assertEquals(0, fakeAuthRepository.reAuthCallCount)
        }
    }

    @Nested
    @DisplayName("AC7: Device is offline")
    inner class Offline {

        @Test
        @DisplayName("[P1] refresh returns NetworkError - returns Offline")
        fun `whenRefreshReturnsNetworkError thenReturnsOffline`() = runTest {
            fakeTokenStore.storedToken = "valid-token"
            fakeCredentialStore.storedCredentials = "user" to "pass"
            fakeAuthRepository.refreshResult = AuthResult.NetworkError

            val result = useCase.execute()

            assertEquals(StartupAuthResult.Offline, result)
        }

        @Test
        @DisplayName("[P1] offline - does NOT attempt credential re-auth")
        fun `whenOffline thenSkipsReAuth`() = runTest {
            fakeTokenStore.storedToken = "valid-token"
            fakeCredentialStore.storedCredentials = "user" to "pass"
            fakeAuthRepository.refreshResult = AuthResult.NetworkError

            useCase.execute()

            assertEquals(0, fakeAuthRepository.reAuthCallCount)
        }
    }

    @Nested
    @DisplayName("AC8: Concurrent access safety")
    inner class MutexConcurrency {

        @Test
        @DisplayName("[P2] concurrent calls only execute one refresh cycle")
        fun `whenConcurrentCalls thenOnlyOneExecutes`() = runTest {
            fakeTokenStore.storedToken = "token"
            fakeCredentialStore.storedCredentials = "user" to "pass"
            fakeAuthRepository.refreshResult = AuthResult.Success

            val results = (1..2).map {
                async { useCase.execute() }
            }.map { it.await() }

            assertEquals(1, fakeAuthRepository.refreshCallCount)
            results.forEach { assertEquals(StartupAuthResult.Success, it) }
        }
    }
}
