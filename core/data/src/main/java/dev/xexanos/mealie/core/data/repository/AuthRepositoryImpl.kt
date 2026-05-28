package dev.xexanos.mealie.core.data.repository

import dev.xexanos.mealie.core.data.datastore.AppPreferencesStore
import dev.xexanos.mealie.core.data.datastore.CredentialsStore
import dev.xexanos.mealie.core.data.datastore.StoredCredentials
import dev.xexanos.mealie.core.data.datastore.StoredToken
import dev.xexanos.mealie.core.data.datastore.TokenStore
import dev.xexanos.mealie.core.data.domain.AuthResult
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import dev.xexanos.mealie.core.network.api.AppService
import dev.xexanos.mealie.core.network.api.AuthService
import dev.xexanos.mealie.core.network.dto.AppAboutDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import android.util.Log
import java.io.IOException

class AuthRepositoryImpl(
    private val appPreferencesStore: AppPreferencesStore,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val tokenStore: TokenStore,
    private val credentialsStore: CredentialsStore,
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
            if (about.version?.startsWith("v3.") == true) UrlProbeResult.Success
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

    override suspend fun authenticate(username: String, password: String): AuthResult {
        return try {
            val serverUrl = appPreferencesStore.getServerUrl().first()
                ?: return AuthResult.NetworkError
            val authService = createAuthService(serverUrl)
            val response = authService.login(username = username, password = password)
            if (response.isSuccessful) {
                val body = response.body() ?: return AuthResult.NetworkError
                tokenStore.saveToken(body.accessToken)
                credentialsStore.saveCredentials(username, password)
                AuthResult.Success
            } else if (response.code() == HTTP_UNAUTHORIZED) {
                AuthResult.InvalidCredentials
            } else {
                AuthResult.NetworkError
            }
        } catch (_: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Log.e("AuthRepository", "Unexpected exception during authenticate", e)
            AuthResult.NetworkError
        }
    }

    override fun getStoredCredentials(): Flow<StoredCredentials> =
        credentialsStore.getCredentials()

    override fun getStoredToken(): Flow<StoredToken> =
        tokenStore.getToken()

    private fun createAuthService(baseUrl: String): AuthService {
        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(AuthService::class.java)
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
    }
}
