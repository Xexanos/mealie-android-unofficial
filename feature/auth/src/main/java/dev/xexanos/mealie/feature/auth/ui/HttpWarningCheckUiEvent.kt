package dev.xexanos.mealie.feature.auth.ui

sealed class HttpWarningCheckUiEvent {
    data object NavigateToCredentials : HttpWarningCheckUiEvent()
}
