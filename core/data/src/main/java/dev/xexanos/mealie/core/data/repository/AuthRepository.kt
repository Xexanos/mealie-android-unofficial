package dev.xexanos.mealie.core.data.repository

import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getStoredServerUrl(): Flow<String?>
    suspend fun probeServerUrl(url: String): UrlProbeResult
    suspend fun saveServerUrl(url: String)
    suspend fun isHttpWarningAcknowledged(url: String): Boolean
    suspend fun acknowledgeHttpWarning(url: String)
}
