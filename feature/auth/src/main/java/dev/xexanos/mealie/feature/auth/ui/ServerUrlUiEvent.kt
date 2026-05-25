package dev.xexanos.mealie.feature.auth.ui

sealed class ServerUrlUiEvent {
    data object NavigateToNext : ServerUrlUiEvent()
}
