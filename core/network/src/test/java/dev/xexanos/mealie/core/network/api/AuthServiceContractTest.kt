package dev.xexanos.mealie.core.network.api

import dev.xexanos.mealie.core.network.dto.AuthTokenDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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

@DisplayName("AuthService API Contract Tests")
class AuthServiceContractTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authService: AuthService

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .build()

        authService = retrofit.create(AuthService::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- AC3: POST /api/auth/token with form-encoded body ---

    @Nested
    @DisplayName("AC3: Login request format")
    inner class LoginRequestFormat {

        @Test
        @Disabled("Red phase - AuthService not yet implemented")
        @DisplayName("login sends form-encoded body with username, password, remember_me")
        fun `login sends form-encoded body with correct fields`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"test-token","token_type":"bearer"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            authService.login(username = "user@example.com", password = "secret")

            val request = mockWebServer.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/auth/token", request.path)

            val body = request.body.readUtf8()
            assertTrue(body.contains("username=user%40example.com"))
            assertTrue(body.contains("password=secret"))
            assertTrue(body.contains("remember_me=true"))

            val contentType = request.getHeader("Content-Type")
            assertTrue(contentType?.contains("application/x-www-form-urlencoded") == true)
        }

        @Test
        @Disabled("Red phase - AuthService not yet implemented")
        @DisplayName("login request does NOT use JSON body")
        fun `login request is not JSON encoded`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"test-token","token_type":"bearer"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            authService.login(username = "user", password = "pass")

            val request = mockWebServer.takeRequest()
            val contentType = request.getHeader("Content-Type") ?: ""
            assertTrue(!contentType.contains("application/json"))
        }
    }

    // --- AC3/AC4: Successful response parsing ---

    @Nested
    @DisplayName("AC3/AC4: Successful authentication response")
    inner class SuccessResponse {

        @Test
        @Disabled("Red phase - AuthService not yet implemented")
        @DisplayName("200 response parses access_token and token_type")
        fun `successful response parses access token`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"eyJhbG.test.token","token_type":"bearer"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val response = authService.login(username = "user", password = "pass")

            assertTrue(response.isSuccessful)
            val body = response.body()!!
            assertEquals("eyJhbG.test.token", body.accessToken)
            assertEquals("bearer", body.tokenType)
        }
    }

    // --- AC5: 401 Unauthorized response ---

    @Nested
    @DisplayName("AC5: Invalid credentials response")
    inner class UnauthorizedResponse {

        @Test
        @Disabled("Red phase - AuthService not yet implemented")
        @DisplayName("401 response returns unsuccessful response")
        fun `unauthorized response returns 401 status`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"detail":"Unauthorized"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val response = authService.login(username = "user", password = "wrong")

            assertEquals(401, response.code())
            assertTrue(!response.isSuccessful)
        }
    }

    // --- Edge: remember_me parameter ---

    @Nested
    @DisplayName("Edge: remember_me defaults to true")
    inner class RememberMe {

        @Test
        @Disabled("Red phase - AuthService not yet implemented")
        @DisplayName("login includes remember_me=true by default")
        fun `login includes remember_me true by default`() = runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"access_token":"token","token_type":"bearer"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            authService.login(username = "user", password = "pass")

            val body = mockWebServer.takeRequest().body.readUtf8()
            assertTrue(body.contains("remember_me=true"))
        }
    }
}
