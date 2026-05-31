package dev.xexanos.mealie.core.network.auth

interface AuthenticatorRefresher {
    suspend fun refreshViaCredentials(): String?
}
