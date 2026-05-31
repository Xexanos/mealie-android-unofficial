package dev.xexanos.mealie.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class MealieAuthenticator(
    private val tokenManager: TokenManager,
    private val refresher: AuthenticatorRefresher,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) > 1) return null

        val newToken = runBlocking {
            refresher.refreshViaCredentials()
        } ?: run {
            runBlocking { tokenManager.signalAuthFailure() }
            return null
        }

        runBlocking { tokenManager.setToken(newToken) }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
