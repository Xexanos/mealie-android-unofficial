package dev.xexanos.mealie.core.ui.di

import dev.xexanos.mealie.core.ui.navigation.NavigationManager
import org.koin.dsl.module

val uiModule = module {
    single { NavigationManager() }
}
