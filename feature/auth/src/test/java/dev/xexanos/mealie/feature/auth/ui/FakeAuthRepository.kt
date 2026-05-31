package dev.xexanos.mealie.feature.auth.ui

import dev.xexanos.mealie.core.data.datastore.StoredCredentials
import dev.xexanos.mealie.core.data.datastore.StoredToken
import dev.xexanos.mealie.core.data.domain.AuthResult
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import dev.xexanos.mealie.core.data.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAuthRepository(
    private var storedUrl: String? = null,
    private var probeResult: UrlProbeResult = UrlProbeResult.Success,
    ackedUrls: Set<String> = emptySet(),
    var authResult: AuthResult = AuthResult.Success(""),
) : AuthRepository {
    private val _ackedUrls = ackedUrls.toMutableSet()
    var probeCallCount = 0
    var authenticateCallCount = 0
    var authGate: CompletableDeferred<Unit>? = null

    override fun getStoredServerUrl(): Flow<String?> = flowOf(storedUrl)

    override suspend fun probeServerUrl(url: String): UrlProbeResult {
        probeCallCount++
        return probeResult
    }

    override suspend fun saveServerUrl(url: String) {
        storedUrl = url
    }

    override suspend fun isHttpWarningAcknowledged(url: String): Boolean = url in _ackedUrls

    override suspend fun acknowledgeHttpWarning(url: String) {
        _ackedUrls.add(url)
    }

    override suspend fun authenticate(username: String, password: String): AuthResult {
        authenticateCallCount++
        authGate?.await()
        return authResult
    }

    override suspend fun refreshToken(token: String): AuthResult = authResult

    override suspend fun reAuthenticateWithStoredCredentials(): AuthResult = authResult

    override fun getStoredCredentials(): Flow<StoredCredentials> =
        flowOf(StoredCredentials())

    override fun getStoredToken(): Flow<StoredToken> =
        flowOf(StoredToken())
}
