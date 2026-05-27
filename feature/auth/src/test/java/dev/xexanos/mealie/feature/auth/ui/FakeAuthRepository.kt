package dev.xexanos.mealie.feature.auth.ui

import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import dev.xexanos.mealie.core.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAuthRepository(
    private var storedUrl: String? = null,
    private var probeResult: UrlProbeResult = UrlProbeResult.Success,
    ackedUrls: Set<String> = emptySet(),
) : AuthRepository {
    private val _ackedUrls = ackedUrls.toMutableSet()
    var probeCallCount = 0

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
}
