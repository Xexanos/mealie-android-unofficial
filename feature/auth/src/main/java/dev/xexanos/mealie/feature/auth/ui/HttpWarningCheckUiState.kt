package dev.xexanos.mealie.feature.auth.ui

sealed class HttpWarningCheckUiState {
    data object Loading : HttpWarningCheckUiState()
    data class ShowWarning(val serverUrl: String) : HttpWarningCheckUiState()
}
