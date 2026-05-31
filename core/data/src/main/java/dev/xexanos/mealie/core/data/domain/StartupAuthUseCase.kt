package dev.xexanos.mealie.core.data.domain

import dev.xexanos.mealie.core.data.repository.AuthRepository
import dev.xexanos.mealie.core.network.auth.CredentialProvider
import dev.xexanos.mealie.core.network.auth.TokenProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class StartupAuthUseCase(
    private val tokenProvider: TokenProvider,
    private val credentialProvider: CredentialProvider,
    private val authRepository: AuthRepository,
) {
    private val mutex = Mutex()
    private var cachedResult: StartupAuthResult? = null

    open suspend fun execute(): StartupAuthResult = mutex.withLock {
        cachedResult?.let { return it }

        val credentials = credentialProvider.getCredentials().first()
        if (credentials.username.isEmpty()) {
            return cacheAndReturn(StartupAuthResult.NoCredentials)
        }

        val storedToken = tokenProvider.getToken().first()
        if (storedToken.accessToken.isNotEmpty()) {
            when (authRepository.refreshToken(storedToken.accessToken)) {
                is AuthResult.Success -> return cacheAndReturn(StartupAuthResult.Success)
                AuthResult.NetworkError -> return cacheAndReturn(StartupAuthResult.Offline)
                AuthResult.InvalidCredentials -> { /* fall through to re-auth */ }
            }
        }

        when (authRepository.reAuthenticateWithStoredCredentials()) {
            is AuthResult.Success -> return cacheAndReturn(StartupAuthResult.Success)
            AuthResult.NetworkError -> return cacheAndReturn(StartupAuthResult.Offline)
            AuthResult.InvalidCredentials -> {
                tokenProvider.clearToken()
                return cacheAndReturn(StartupAuthResult.CredentialsInvalid)
            }
        }
    }

    private fun cacheAndReturn(result: StartupAuthResult): StartupAuthResult {
        cachedResult = result
        return result
    }
}
