package dev.xexanos.mealie.feature.auth.ui

sealed class StartupAuthEvent {
    data object NavigateToMain : StartupAuthEvent()
    data object NavigateToCredentials : StartupAuthEvent()
    data object NavigateToSetup : StartupAuthEvent()
}
