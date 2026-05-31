package dev.xexanos.mealie.core.network.auth

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("TokenInterceptor Tests")
class TokenInterceptorTest {

    private val tokenManager = mockk<TokenManager>()
    private lateinit var interceptor: TokenInterceptor

    private val tokenFlow = MutableStateFlow("")

    @BeforeEach
    fun setup() {
        every { tokenManager.currentToken } returns tokenFlow
        interceptor = TokenInterceptor(tokenManager)
    }

    private fun buildChain(request: Request): Pair<Interceptor.Chain, io.mockk.CapturingSlot<Request>> {
        val chain = mockk<Interceptor.Chain>()
        val requestSlot = slot<Request>()
        every { chain.request() } returns request
        every { chain.proceed(capture(requestSlot)) } answers {
            Response.Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }
        return chain to requestSlot
    }

    @Nested
    @DisplayName("AC6: Token injection")
    inner class TokenInjection {

        @Test
        @DisplayName("[P0] When token exists then adds Bearer Authorization header")
        fun whenTokenExists_thenAddsAuthorizationHeader() {
            tokenFlow.value = "valid-token-123"
            val request = Request.Builder()
                .url("https://mealie.example.com/api/households/shopping/lists")
                .build()
            val (chain, requestSlot) = buildChain(request)

            interceptor.intercept(chain)

            assertEquals("Bearer valid-token-123", requestSlot.captured.header("Authorization"))
        }

        @Test
        @DisplayName("[P1] When token is empty then does not add Authorization header")
        fun whenTokenIsEmpty_thenNoAuthorizationHeader() {
            tokenFlow.value = ""
            val request = Request.Builder()
                .url("https://mealie.example.com/api/households/shopping/lists")
                .build()
            val (chain, requestSlot) = buildChain(request)

            interceptor.intercept(chain)

            assertNull(requestSlot.captured.header("Authorization"))
        }

        @Test
        @DisplayName("[P0] When Authorization header already present then does not override")
        fun whenAuthorizationHeaderAlreadyPresent_thenDoesNotOverride() {
            tokenFlow.value = "interceptor-token"
            val request = Request.Builder()
                .url("https://mealie.example.com/api/auth/refresh")
                .header("Authorization", "Bearer explicit-token")
                .build()
            val (chain, requestSlot) = buildChain(request)

            interceptor.intercept(chain)

            assertEquals("Bearer explicit-token", requestSlot.captured.header("Authorization"))
        }
    }
}
