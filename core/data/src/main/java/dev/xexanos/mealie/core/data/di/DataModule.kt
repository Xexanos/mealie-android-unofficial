package dev.xexanos.mealie.core.data.di

import dev.xexanos.mealie.core.data.datastore.AppPreferencesStore
import dev.xexanos.mealie.core.data.datastore.CredentialsStore
import dev.xexanos.mealie.core.data.datastore.TokenStore
import dev.xexanos.mealie.core.data.repository.AuthRepository
import dev.xexanos.mealie.core.data.repository.AuthRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { AppPreferencesStore(androidContext()) }
    single { TokenStore(androidContext()) }
    single { CredentialsStore(androidContext()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get()) }
}
