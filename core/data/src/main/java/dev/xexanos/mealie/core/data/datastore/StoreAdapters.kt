package dev.xexanos.mealie.core.data.datastore

import dev.xexanos.mealie.core.network.auth.CredentialProvider
import dev.xexanos.mealie.core.network.auth.StoredCredentialData
import dev.xexanos.mealie.core.network.auth.StoredTokenData
import dev.xexanos.mealie.core.network.auth.TokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TokenProviderAdapter(private val tokenStore: TokenStore) : TokenProvider {
    override fun getToken(): Flow<StoredTokenData> =
        tokenStore.getToken().map { StoredTokenData(accessToken = it.accessToken) }

    override suspend fun saveToken(accessToken: String) =
        tokenStore.saveToken(accessToken)

    override suspend fun clearToken() =
        tokenStore.clearToken()
}

class CredentialProviderAdapter(private val credentialsStore: CredentialsStore) : CredentialProvider {
    override fun getCredentials(): Flow<StoredCredentialData> =
        credentialsStore.getCredentials().map {
            StoredCredentialData(username = it.username, password = it.password)
        }
}
