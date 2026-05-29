package dev.xexanos.mealie.feature.auth.ui

sealed class CredentialUiEvent {
    data object NavigateToMain : CredentialUiEvent()
}
