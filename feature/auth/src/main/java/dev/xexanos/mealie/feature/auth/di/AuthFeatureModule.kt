package dev.xexanos.mealie.feature.auth.di

import dev.xexanos.mealie.feature.auth.ui.CredentialViewModel
import dev.xexanos.mealie.feature.auth.ui.HttpWarningCheckViewModel
import dev.xexanos.mealie.feature.auth.ui.ServerUrlViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val authFeatureModule = module {
    viewModel { ServerUrlViewModel(get()) }
    viewModel { HttpWarningCheckViewModel(get()) }
    viewModel { CredentialViewModel(get()) }
}
