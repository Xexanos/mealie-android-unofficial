package dev.xexanos.mealie.di

import dev.xexanos.mealie.core.data.di.dataModule
import dev.xexanos.mealie.core.data.repository.AuthRepository
import dev.xexanos.mealie.core.network.di.networkModule
import dev.xexanos.mealie.core.sync.di.syncModule
import dev.xexanos.mealie.core.ui.di.uiModule
import dev.xexanos.mealie.feature.auth.di.authFeatureModule
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.koin.test.verify.verify

@Tag("integration")
class KoinModuleCheckTest {

    @Test
    fun `when network module verified then all dependencies resolve`() {
        networkModule.verify()
    }

    @Test
    fun `when ui module verified then all dependencies resolve`() {
        uiModule.verify()
    }

    @Test
    fun `when sync module verified then all dependencies resolve`() {
        syncModule.verify()
    }

    @Test
    fun `when data module verified then all dependencies resolve`() {
        dataModule.verify(
            extraTypes = listOf(
                android.content.Context::class,
                OkHttpClient::class,
                Json::class,
            ),
        )
    }

    @Test
    fun `when auth feature module verified then all dependencies resolve`() {
        authFeatureModule.verify(
            extraTypes = listOf(
                AuthRepository::class,
            ),
        )
    }
}
