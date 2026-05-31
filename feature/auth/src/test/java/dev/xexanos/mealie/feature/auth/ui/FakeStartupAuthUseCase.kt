package dev.xexanos.mealie.feature.auth.ui

import dev.xexanos.mealie.core.data.domain.StartupAuthResult
import dev.xexanos.mealie.core.data.domain.StartupAuthUseCase
import dev.xexanos.mealie.core.network.auth.CredentialProvider
import dev.xexanos.mealie.core.network.auth.StoredCredentialData
import dev.xexanos.mealie.core.network.auth.StoredTokenData
import dev.xexanos.mealie.core.network.auth.TokenProvider
import dev.xexanos.mealie.core.data.domain.AuthResult
import dev.xexanos.mealie.core.data.datastore.StoredCredentials
import dev.xexanos.mealie.core.data.datastore.StoredToken
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import dev.xexanos.mealie.core.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeStartupAuthUseCase(
    private val result: StartupAuthResult,
) : StartupAuthUseCase(
    tokenProvider = NoOpTokenProvider(),
    credentialProvider = NoOpCredentialProvider(),
    authRepository = NoOpAuthRepository(),
) {
    override suspend fun execute(): StartupAuthResult = result
}

private class NoOpTokenProvider : TokenProvider {
    override fun getToken(): Flow<StoredTokenData> = flowOf(StoredTokenData())
    override suspend fun saveToken(accessToken: String) {}
    override suspend fun clearToken() {}
}

private class NoOpCredentialProvider : CredentialProvider {
    override fun getCredentials(): Flow<StoredCredentialData> = flowOf(StoredCredentialData())
}

private class NoOpAuthRepository : AuthRepository {
    override fun getStoredServerUrl(): Flow<String?> = flowOf(null)
    override suspend fun probeServerUrl(url: String): UrlProbeResult = UrlProbeResult.Success
    override suspend fun saveServerUrl(url: String) {}
    override suspend fun isHttpWarningAcknowledged(url: String): Boolean = false
    override suspend fun acknowledgeHttpWarning(url: String) {}
    override suspend fun authenticate(username: String, password: String): AuthResult = AuthResult.Success("")
    override suspend fun refreshToken(token: String): AuthResult = AuthResult.Success("")
    override suspend fun reAuthenticateWithStoredCredentials(): AuthResult = AuthResult.Success("")
    override fun getStoredCredentials(): Flow<StoredCredentials> = flowOf(StoredCredentials())
    override fun getStoredToken(): Flow<StoredToken> = flowOf(StoredToken())
}
