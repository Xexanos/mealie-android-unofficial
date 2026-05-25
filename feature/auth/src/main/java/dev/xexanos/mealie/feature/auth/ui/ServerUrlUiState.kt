package dev.xexanos.mealie.feature.auth.ui

sealed class ServerUrlUiState {
    data object Loading : ServerUrlUiState()
    data object AwaitingInput : ServerUrlUiState()
    data class Probing(val normalizedUrl: String) : ServerUrlUiState()
    data class InputError(val message: String, val lastUrl: String = "") : ServerUrlUiState()
}
