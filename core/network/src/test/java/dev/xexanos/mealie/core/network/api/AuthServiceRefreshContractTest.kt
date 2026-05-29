package dev.xexanos.mealie.core.network.api

import dev.xexanos.mealie.core.network.dto.AuthTokenDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Disabled("Red phase - AuthService.refreshToken() not implemented yet")
@DisplayName("AuthService Refresh API Contract Tests")
class AuthServiceRefreshContractTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authService: AuthService

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        authService = retrofit.create(AuthService::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Nested
    @DisplayName("AC1: Refresh request format")
    inner class RefreshRequestFormat {

        @Test
        @DisplayName("refreshToken sends GET with Authorization Bearer header")
        fun `whenRefreshTokenCalled thenSendsGetWithBearerHeader`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"new-token","token_type":"bearer"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            authService.refreshToken("Bearer stored-token-value")

            val request = mockWebServer.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/auth/refresh", request.path)
            assertEquals("Bearer stored-token-value", request.getHeader("Authorization"))
        }

        @Test
        @DisplayName("refreshToken sends no request body")
        fun `whenRefreshTokenCalled thenNoRequestBody`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"new-token","token_type":"bearer"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            authService.refreshToken("Bearer token")

            val request = mockWebServer.takeRequest()
            assertEquals(0L, request.bodySize)
        }
    }

    @Nested
    @DisplayName("AC2: Successful refresh response")
    inner class SuccessResponse {

        @Test
        @DisplayName("200 response parses access_token and token_type")
        fun `whenRefreshReturns200 thenParsesAccessToken`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"eyJhbG.fresh.token","token_type":"bearer"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val response = authService.refreshToken("Bearer old-token")

            assertTrue(response.isSuccessful)
            val body = response.body()!!
            assertEquals("eyJhbG.fresh.token", body.accessToken)
            assertEquals("bearer", body.tokenType)
        }
    }

    @Nested
    @DisplayName("AC3: Token expired (401) response")
    inner class UnauthorizedResponse {

        @Test
        @DisplayName("401 response returns unsuccessful response")
        fun `whenRefreshReturns401 thenResponseIsUnsuccessful`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"detail":"Could not validate credentials"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val response = authService.refreshToken("Bearer expired-token")

            assertEquals(401, response.code())
            assertTrue(!response.isSuccessful)
        }
    }
}
