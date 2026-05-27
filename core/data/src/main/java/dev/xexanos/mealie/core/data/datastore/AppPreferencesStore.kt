package dev.xexanos.mealie.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_preferences")

class AppPreferencesStore(private val context: Context) {
    companion object {
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        val HTTP_WARNING_ACK_URLS_KEY = stringSetPreferencesKey("http_warning_ack_urls")
    }

    fun getServerUrl(): Flow<String?> =
        context.appPreferencesDataStore.data.map { it[SERVER_URL_KEY] }

    suspend fun setServerUrl(url: String) {
        context.appPreferencesDataStore.edit { it[SERVER_URL_KEY] = url }
    }

    fun getHttpWarningAckedUrls(): Flow<Set<String>> =
        context.appPreferencesDataStore.data.map { it[HTTP_WARNING_ACK_URLS_KEY] ?: emptySet() }

    suspend fun acknowledgeHttpWarning(url: String) {
        context.appPreferencesDataStore.edit { prefs ->
            val current = prefs[HTTP_WARNING_ACK_URLS_KEY] ?: emptySet()
            prefs[HTTP_WARNING_ACK_URLS_KEY] = current + url
        }
    }
}
