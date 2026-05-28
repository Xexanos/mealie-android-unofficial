package dev.xexanos.mealie.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class StoredCredentials(
    val username: String = "",
    val password: String = "",
)

internal object StoredCredentialsSerializer : Serializer<StoredCredentials> {
    override val defaultValue: StoredCredentials = StoredCredentials()

    override suspend fun readFrom(input: InputStream): StoredCredentials =
        Json.decodeFromString(input.readBytes().decodeToString())

    override suspend fun writeTo(t: StoredCredentials, output: OutputStream) {
        output.write(Json.encodeToString(t).encodeToByteArray())
    }
}

class CredentialsStore(private val context: Context) {
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "credentials_keyset", "credentials_keyset_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://mealie_credentials_master_key")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    private val Context.credentialsDataStore: DataStore<StoredCredentials> by dataStore(
        fileName = "credentials.pb",
        serializer = AeadSerializer(
            aead = aead,
            wrappedSerializer = StoredCredentialsSerializer,
        ),
    )

    fun getCredentials(): Flow<StoredCredentials> =
        context.credentialsDataStore.data

    suspend fun saveCredentials(username: String, password: String) {
        context.credentialsDataStore.updateData {
            StoredCredentials(username = username, password = password)
        }
    }

    suspend fun clearCredentials() {
        context.credentialsDataStore.updateData { StoredCredentials() }
    }
}
