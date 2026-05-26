package dev.xexanos.mealie.core.data.repository

import dev.xexanos.mealie.core.data.datastore.AppPreferencesStore
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@Tag("integration")
class AuthRepositoryImplTest {

    private val mockServer = MockWebServer()
    private val appPreferencesStore = mockk<AppPreferencesStore>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(500, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .callTimeout(1, TimeUnit.SECONDS)
        .build()

    private lateinit var repository: AuthRepositoryImpl

    @BeforeEach
    fun setup() {
        mockServer.start()
        every { appPreferencesStore.getServerUrl() } returns flowOf(null)
        repository = AuthRepositoryImpl(appPreferencesStore, okHttpClient, json)
    }

    @AfterEach
    fun teardown() {
        mockServer.close()
    }

    private fun baseUrl(): String = mockServer.url("/").toString().trimEnd('/')

    @Test
    fun `when probe receives valid Mealie response then returns Success`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""{"version": "3.18.0", "production": true}""")
                .build(),
        )

        val result = repository.probeServerUrl(baseUrl())

        assertEquals(UrlProbeResult.Success, result)
    }

    @Test
    fun `when probe receives version without 3 prefix then returns NotMealieServer`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""{"version": "2.0.0", "production": true}""")
                .build(),
        )

        val result = repository.probeServerUrl(baseUrl())

        assertEquals(UrlProbeResult.NotMealieServer, result)
    }

    @Test
    fun `when probe receives null version then returns NotMealieServer`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""{"production": true}""")
                .build(),
        )

        val result = repository.probeServerUrl(baseUrl())

        assertEquals(UrlProbeResult.NotMealieServer, result)
    }

    @Test
    fun `when probe receives non-JSON response then returns NotMealieServer`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "text/html")
                .body("<html>Not Mealie</html>")
                .build(),
        )

        val result = repository.probeServerUrl(baseUrl())

        assertEquals(UrlProbeResult.NotMealieServer, result)
    }

    @Test
    fun `when probe receives 404 then returns NotMealieServer`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .code(404)
                .body("")
                .build(),
        )

        val result = repository.probeServerUrl(baseUrl())

        assertEquals(UrlProbeResult.NotMealieServer, result)
    }

    @Test
    fun `when probe cannot connect then returns NetworkError`() = runTest {
        mockServer.close()

        val result = repository.probeServerUrl("http://localhost:1")

        assertEquals(UrlProbeResult.NetworkError, result)
    }

    @Test
    fun `when probe receives valid response then GET request was sent`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""{"version": "3.18.0"}""")
                .build(),
        )

        repository.probeServerUrl(baseUrl())

        val request = mockServer.takeRequest()
        assertEquals("GET", request.method)
    }
}
