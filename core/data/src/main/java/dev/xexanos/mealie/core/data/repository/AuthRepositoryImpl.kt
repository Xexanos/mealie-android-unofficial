package dev.xexanos.mealie.core.data.repository

import dev.xexanos.mealie.core.data.datastore.AppPreferencesStore
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import dev.xexanos.mealie.core.network.api.AppService
import dev.xexanos.mealie.core.network.dto.AppAboutDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

class AuthRepositoryImpl(
    private val appPreferencesStore: AppPreferencesStore,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) : AuthRepository {

    override fun getStoredServerUrl(): Flow<String?> =
        appPreferencesStore.getServerUrl()

    override suspend fun saveServerUrl(url: String) =
        appPreferencesStore.setServerUrl(url)

    override suspend fun probeServerUrl(url: String): UrlProbeResult {
        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl("$url/")
                .client(okHttpClient)
                .addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType())
                )
                .build()
            val service = retrofit.create(AppService::class.java)
            val about: AppAboutDto = service.getAppAbout()
            if (about.version?.startsWith("3.") == true) UrlProbeResult.Success
            else UrlProbeResult.NotMealieServer
        } catch (_: IOException) {
            UrlProbeResult.NetworkError
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            UrlProbeResult.NotMealieServer
        }
    }

    override suspend fun isHttpWarningAcknowledged(url: String): Boolean {
        val ackedUrls = appPreferencesStore.getHttpWarningAckedUrls().first()
        return url in ackedUrls
    }

    override suspend fun acknowledgeHttpWarning(url: String) {
        appPreferencesStore.acknowledgeHttpWarning(url)
    }
}
