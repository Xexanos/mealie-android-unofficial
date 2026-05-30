package dev.xexanos.mealie.core.data.di

import dev.xexanos.mealie.core.data.datastore.AppPreferencesStore
import dev.xexanos.mealie.core.data.datastore.CredentialProviderAdapter
import dev.xexanos.mealie.core.data.datastore.CredentialsStore
import dev.xexanos.mealie.core.data.datastore.TokenProviderAdapter
import dev.xexanos.mealie.core.data.datastore.TokenStore
import dev.xexanos.mealie.core.data.domain.StartupAuthUseCase
import dev.xexanos.mealie.core.data.repository.AuthRepository
import dev.xexanos.mealie.core.data.repository.AuthRepositoryImpl
import dev.xexanos.mealie.core.network.auth.CredentialProvider
import dev.xexanos.mealie.core.network.auth.TokenProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { AppPreferencesStore(androidContext()) }
    single { TokenStore(androidContext()) }
    single { CredentialsStore(androidContext()) }
    single<TokenProvider> { TokenProviderAdapter(get()) }
    single<CredentialProvider> { CredentialProviderAdapter(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get()) }
    single { StartupAuthUseCase(get(), get(), get()) }
}
