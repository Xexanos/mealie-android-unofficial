package dev.xexanos.mealie.e2e

import dev.xexanos.mealie.core.data.datastore.AppPreferencesStore
import dev.xexanos.mealie.core.data.datastore.CredentialsStore
import dev.xexanos.mealie.core.data.domain.StartupAuthUseCase
import dev.xexanos.mealie.core.network.auth.TokenProvider
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.context.GlobalContext

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithStoredAuth(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val ackHttpWarning: Boolean = true,
    val refreshStatus: Int = 0,
    val authStatus: Int = 0,
    val refreshDelayMs: Int = 0,
)

class StartupStateRule(private val wireMock: WireMockRule) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val auth = description.getAnnotation(WithStoredAuth::class.java)
                if (auth != null) {
                    setupWireMockStubs(auth)
                    runBlocking { prepopulate(auth) }
                }
                resetUseCaseCache()
                try {
                    base.evaluate()
                } finally {
                    runBlocking { cleanup() }
                }
            }
        }
    }

    private fun setupWireMockStubs(auth: WithStoredAuth) {
        when (auth.refreshStatus) {
            200 -> if (auth.refreshDelayMs > 0) {
                wireMock.stubRefreshWithDelay(auth.refreshDelayMs)
            } else {
                wireMock.stubRefreshSuccess()
            }
            401 -> wireMock.stubRefreshUnauthorized()
        }
        when (auth.authStatus) {
            200 -> wireMock.stubAuthSuccess()
            401 -> wireMock.stubAuthUnauthorized()
        }
    }

    private suspend fun prepopulate(auth: WithStoredAuth) {
        val koin = GlobalContext.get()
        val appPrefs = koin.get<AppPreferencesStore>()
        val credentialsStore = koin.get<CredentialsStore>()
        val tokenProvider = koin.get<TokenProvider>()

        if (auth.serverUrl.isNotEmpty()) {
            appPrefs.setServerUrl(auth.serverUrl)
        }
        if (auth.username.isNotEmpty() && auth.password.isNotEmpty()) {
            credentialsStore.saveCredentials(auth.username, auth.password)
        }
        if (auth.token.isNotEmpty()) {
            tokenProvider.saveToken(auth.token)
        }
        if (auth.ackHttpWarning && auth.serverUrl.isNotEmpty()) {
            appPrefs.acknowledgeHttpWarning(auth.serverUrl)
        }
    }

    private fun resetUseCaseCache() {
        val koin = GlobalContext.get()
        val useCase = koin.get<StartupAuthUseCase>()
        try {
            val field = StartupAuthUseCase::class.java.getDeclaredField("cachedResult")
            field.isAccessible = true
            field.set(useCase, null)
        } catch (_: Exception) {
            // If cache reset fails, test may still pass on fresh process (Orchestrator)
        }
    }

    private suspend fun cleanup() {
        val koin = GlobalContext.get()
        koin.get<TokenProvider>().clearToken()
        koin.get<CredentialsStore>().clearCredentials()
        koin.get<AppPreferencesStore>().setServerUrl("")
    }
}
