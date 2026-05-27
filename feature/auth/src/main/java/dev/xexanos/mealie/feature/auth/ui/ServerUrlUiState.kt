package dev.xexanos.mealie.feature.auth.ui

import androidx.annotation.StringRes

sealed class ServerUrlUiState {
    data object Loading : ServerUrlUiState()
    data object AwaitingInput : ServerUrlUiState()
    data class Probing(val normalizedUrl: String) : ServerUrlUiState()
    data class InputError(@StringRes val messageResId: Int, val lastUrl: String = "") : ServerUrlUiState()
}
