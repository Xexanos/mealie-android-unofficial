package dev.xexanos.mealie.core.network.auth

import kotlinx.coroutines.flow.Flow

data class StoredTokenData(val accessToken: String = "")

data class StoredCredentialData(val username: String = "", val password: String = "")

interface TokenProvider {
    fun getToken(): Flow<StoredTokenData>
    suspend fun saveToken(accessToken: String)
    suspend fun clearToken()
}

interface CredentialProvider {
    fun getCredentials(): Flow<StoredCredentialData>
}
