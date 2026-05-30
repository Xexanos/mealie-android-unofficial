package dev.xexanos.mealie.core.network.di

import dev.xexanos.mealie.core.network.BuildConfig
import dev.xexanos.mealie.core.network.auth.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
    single { buildOkHttpClient() }
    single { TokenManager() }
}

private const val CONNECT_TIMEOUT_SECONDS = 10L
private const val READ_TIMEOUT_SECONDS = 30L
private const val WRITE_TIMEOUT_SECONDS = 15L

private val SENSITIVE_BODY_PATTERNS = listOf(
    Regex("""password=[^&\s]+""") to "password=<redacted>",
    Regex(""""access_token"\s*:\s*"[^"]+"""") to """"access_token":"<redacted>"""",
)

internal fun redactSensitiveContent(message: String): String =
    SENSITIVE_BODY_PATTERNS.fold(message) { acc, (pattern, replacement) ->
        acc.replace(pattern, replacement)
    }

private fun buildOkHttpClient(): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    if (BuildConfig.DEBUG) {
        val redactingLogger = HttpLoggingInterceptor.Logger { message ->
            HttpLoggingInterceptor.Logger.DEFAULT.log(redactSensitiveContent(message))
        }
        builder.addInterceptor(
            HttpLoggingInterceptor(redactingLogger).apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("Authorization")
            }
        )
    }
    return builder.build()
}
