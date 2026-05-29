package dev.xexanos.mealie.e2e

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

sealed class E2EBackend {
    abstract val baseUrl: String
    abstract val username: String
    abstract val password: String

    val isHttps: Boolean get() = baseUrl.startsWith("https://")

    data class WireMock(
        override val baseUrl: String = "http://10.0.2.2:8080",
        override val username: String = "test@example.com",
        override val password: String = "password123",
    ) : E2EBackend()

    data class Live(
        override val baseUrl: String,
        override val username: String,
        override val password: String,
    ) : E2EBackend()

    suspend fun healthCheck(): Boolean = runCatching {
        withContext(Dispatchers.IO) {
            val conn = URL("$baseUrl/api/app/about").openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            try {
                conn.responseCode == 200
            } finally {
                conn.disconnect()
            }
        }
    }.getOrDefault(false)

    companion object {
        private const val DEFAULT_LIVE_BASE_URL = "https://demo.mealie.io"
        private const val DEFAULT_LIVE_USERNAME = "changeme@example.com"
        private const val DEFAULT_LIVE_PASSWORD = "MyPassword"

        fun fromInstrumentationArgs(): E2EBackend {
            val args: Bundle = InstrumentationRegistry.getArguments()
            val mode = args.getString("e2eBackend", "wiremock")
            return when (mode) {
                "live" -> Live(
                    baseUrl = args.getString("e2eBaseUrl") ?: DEFAULT_LIVE_BASE_URL,
                    username = args.getString("e2eUsername") ?: DEFAULT_LIVE_USERNAME,
                    password = args.getString("e2ePassword") ?: DEFAULT_LIVE_PASSWORD,
                )
                else -> WireMock()
            }
        }
    }
}
