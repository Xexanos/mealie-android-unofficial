package dev.xexanos.mealie

import android.app.Application
import dev.xexanos.mealie.core.data.di.dataModule
import dev.xexanos.mealie.core.network.di.networkModule
import dev.xexanos.mealie.core.sync.di.syncModule
import dev.xexanos.mealie.core.ui.di.uiModule
import dev.xexanos.mealie.feature.auth.di.authFeatureModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class MealieApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin {
            androidContext(this@MealieApplication)
            modules(
                networkModule,
                dataModule,
                syncModule,
                uiModule,
                authFeatureModule
            )
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}
