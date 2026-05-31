package dev.xexanos.mealie.core.network.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DisplayName("MealieAuthenticator Tests")
class MealieAuthenticatorTest {

    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val refresher = mockk<AuthenticatorRefresher>()
    private lateinit var authenticator: MealieAuthenticator

    @BeforeEach
    fun setup() {
        authenticator = MealieAuthenticator(tokenManager, refresher)
    }

    private fun build401Response(request: Request, priorResponse: Response? = null): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .apply { priorResponse?.let { priorResponse(it) } }
            .build()

    private fun buildRequest(): Request =
        Request.Builder()
            .url("https://mealie.example.com/api/households/shopping/lists")
            .build()

    @Nested
    @DisplayName("AC2: Re-authentication succeeds")
    inner class ReAuthSuccess {

        @Test
        @DisplayName("[P0] When refresher returns new token then retries request with Bearer header")
        fun whenRefresherReturnsToken_thenRetriesWithNewBearer() = runTest {
            coEvery { refresher.refreshViaCredentials() } returns "new-access-token"

            val request = buildRequest()
            val response = build401Response(request)

            val retryRequest = authenticator.authenticate(null, response)

            assertNotNull(retryRequest)
            assertEquals(
                "Bearer new-access-token",
                retryRequest.header("Authorization"),
            )
        }

        @Test
        @DisplayName("[P0] When refresher returns new token then persists token via TokenManager")
        fun whenRefresherReturnsToken_thenPersistsViaTokenManager() = runTest {
            coEvery { refresher.refreshViaCredentials() } returns "new-access-token"

            val request = buildRequest()
            val response = build401Response(request)

            authenticator.authenticate(null, response)

            coVerify { tokenManager.setToken("new-access-token") }
        }
    }

    @Nested
    @DisplayName("AC3: Re-authentication fails")
    inner class ReAuthFailure {

        @Test
        @DisplayName("[P0] When refresher returns null then authenticate returns null")
        fun whenRefresherReturnsNull_thenReturnsNull() = runTest {
            coEvery { refresher.refreshViaCredentials() } returns null

            val request = buildRequest()
            val response = build401Response(request)

            val retryRequest = authenticator.authenticate(null, response)

            assertNull(retryRequest)
        }

        @Test
        @DisplayName("[P0] When refresher returns null then signals auth failure")
        fun whenRefresherReturnsNull_thenSignalsAuthFailure() = runTest {
            coEvery { refresher.refreshViaCredentials() } returns null

            val request = buildRequest()
            val response = build401Response(request)

            authenticator.authenticate(null, response)

            coVerify { tokenManager.signalAuthFailure() }
        }
    }

    @Nested
    @DisplayName("AC5: Infinite loop prevention")
    inner class InfiniteLoopPrevention {

        @Test
        @DisplayName("[P0] When response has prior response then returns null immediately")
        fun whenResponseHasPriorResponse_thenReturnsNullImmediately() = runTest {
            val request = buildRequest()
            val firstResponse = build401Response(request)
            val secondResponse = build401Response(request, priorResponse = firstResponse)

            val retryRequest = authenticator.authenticate(null, secondResponse)

            assertNull(retryRequest)
            coVerify(exactly = 0) { refresher.refreshViaCredentials() }
        }
    }
}
