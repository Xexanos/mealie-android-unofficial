package dev.xexanos.mealie.core.data.auth

import dev.xexanos.mealie.core.data.domain.AuthResult
import dev.xexanos.mealie.core.data.repository.AuthRepository
import dev.xexanos.mealie.core.network.auth.AuthenticatorRefresher

class AuthenticatorRefresherImpl(
    private val authRepository: AuthRepository,
) : AuthenticatorRefresher {

    override suspend fun refreshViaCredentials(): String? {
        return when (val result = authRepository.reAuthenticateWithStoredCredentials()) {
            is AuthResult.Success -> result.accessToken
            AuthResult.InvalidCredentials -> null
            AuthResult.NetworkError -> null
        }
    }
}
