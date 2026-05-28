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
data class StoredToken(
    val accessToken: String = "",
)

internal object StoredTokenSerializer : Serializer<StoredToken> {
    override val defaultValue: StoredToken = StoredToken()

    override suspend fun readFrom(input: InputStream): StoredToken =
        Json.decodeFromString(input.readBytes().decodeToString())

    override suspend fun writeTo(t: StoredToken, output: OutputStream) {
        output.write(Json.encodeToString(t).encodeToByteArray())
    }
}

class TokenStore(private val context: Context) {
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "token_keyset", "token_keyset_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://mealie_token_master_key")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    private val Context.tokenDataStore: DataStore<StoredToken> by dataStore(
        fileName = "token.pb",
        serializer = AeadSerializer(
            aead = aead,
            wrappedSerializer = StoredTokenSerializer,
        ),
    )

    fun getToken(): Flow<StoredToken> =
        context.tokenDataStore.data

    suspend fun saveToken(accessToken: String) {
        context.tokenDataStore.updateData {
            StoredToken(accessToken = accessToken)
        }
    }

    suspend fun clearToken() {
        context.tokenDataStore.updateData { StoredToken() }
    }
}
