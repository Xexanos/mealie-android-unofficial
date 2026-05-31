package dev.xexanos.mealie.core.data.domain

import dev.xexanos.mealie.core.data.datastore.StoredCredentials
import dev.xexanos.mealie.core.data.datastore.StoredToken
import dev.xexanos.mealie.core.data.domain.AuthResult
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import dev.xexanos.mealie.core.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

class FakeStartupAuthRepository : AuthRepository {
    var refreshResult: AuthResult = AuthResult.Success("")
    var reAuthResult: AuthResult = AuthResult.Success("")
    var refreshCallCount = 0
    var reAuthCallCount = 0
    var refreshThrowsIOException = false
    var refreshDelay: Long = 0L

    override suspend fun refreshToken(token: String): AuthResult {
        refreshCallCount++
        if (refreshDelay > 0) delay(refreshDelay)
        if (refreshThrowsIOException) throw IOException("No network")
        return refreshResult
    }

    override suspend fun reAuthenticateWithStoredCredentials(): AuthResult {
        reAuthCallCount++
        return reAuthResult
    }

    override fun getStoredServerUrl(): Flow<String?> = flowOf(null)
    override suspend fun probeServerUrl(url: String): UrlProbeResult = UrlProbeResult.Success
    override suspend fun saveServerUrl(url: String) {}
    override suspend fun isHttpWarningAcknowledged(url: String): Boolean = false
    override suspend fun acknowledgeHttpWarning(url: String) {}
    override suspend fun authenticate(username: String, password: String): AuthResult = AuthResult.Success("")
    override fun getStoredCredentials(): Flow<StoredCredentials> = flowOf(StoredCredentials())
    override fun getStoredToken(): Flow<StoredToken> = flowOf(StoredToken())
}
