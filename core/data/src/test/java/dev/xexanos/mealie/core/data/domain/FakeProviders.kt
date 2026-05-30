package dev.xexanos.mealie.core.data.domain

import dev.xexanos.mealie.core.network.auth.CredentialProvider
import dev.xexanos.mealie.core.network.auth.StoredCredentialData
import dev.xexanos.mealie.core.network.auth.StoredTokenData
import dev.xexanos.mealie.core.network.auth.TokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTokenProvider : TokenProvider {
    var storedToken: String = ""
    var saveTokenCallCount = 0
    var clearTokenCallCount = 0

    override fun getToken(): Flow<StoredTokenData> =
        flowOf(StoredTokenData(accessToken = storedToken))

    override suspend fun saveToken(accessToken: String) {
        saveTokenCallCount++
        storedToken = accessToken
    }

    override suspend fun clearToken() {
        clearTokenCallCount++
        storedToken = ""
    }
}

class FakeCredentialProvider : CredentialProvider {
    var storedCredentials: Pair<String, String>? = null

    override fun getCredentials(): Flow<StoredCredentialData> {
        val creds = storedCredentials
        return if (creds != null) {
            flowOf(StoredCredentialData(username = creds.first, password = creds.second))
        } else {
            flowOf(StoredCredentialData())
        }
    }
}
