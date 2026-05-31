package dev.xexanos.mealie.core.data.auth

import dev.xexanos.mealie.core.data.repository.AuthRepository
import dev.xexanos.mealie.core.data.repository.AuthResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("AuthenticatorRefresherImpl Tests")
class AuthenticatorRefresherImplTest {

    private val authRepository = mockk<AuthRepository>()
    private lateinit var refresher: AuthenticatorRefresherImpl

    @BeforeEach
    fun setup() {
        refresher = AuthenticatorRefresherImpl(authRepository)
    }

    @Nested
    @DisplayName("AC2: Successful credential re-authentication")
    inner class SuccessCase {

        @Test
        @Disabled("RED PHASE: AuthenticatorRefresherImpl not yet implemented")
        @DisplayName("[P1] When AuthRepository returns Success then returns access token")
        fun whenAuthRepositoryReturnsSuccess_thenReturnsAccessToken() = runTest {
            coEvery {
                authRepository.reAuthenticateWithStoredCredentials()
            } returns AuthResult.Success("refreshed-token-456")

            val result = refresher.refreshViaCredentials()

            assertEquals("refreshed-token-456", result)
        }
    }

    @Nested
    @DisplayName("AC3: Failed credential re-authentication")
    inner class FailureCases {

        @Test
        @Disabled("RED PHASE: AuthenticatorRefresherImpl not yet implemented")
        @DisplayName("[P1] When AuthRepository returns InvalidCredentials then returns null")
        fun whenAuthRepositoryReturnsInvalidCredentials_thenReturnsNull() = runTest {
            coEvery {
                authRepository.reAuthenticateWithStoredCredentials()
            } returns AuthResult.InvalidCredentials

            val result = refresher.refreshViaCredentials()

            assertNull(result)
        }

        @Test
        @Disabled("RED PHASE: AuthenticatorRefresherImpl not yet implemented")
        @DisplayName("[P1] When AuthRepository returns NetworkError then returns null")
        fun whenAuthRepositoryReturnsNetworkError_thenReturnsNull() = runTest {
            coEvery {
                authRepository.reAuthenticateWithStoredCredentials()
            } returns AuthResult.NetworkError

            val result = refresher.refreshViaCredentials()

            assertNull(result)
        }
    }
}
