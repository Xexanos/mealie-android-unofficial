package dev.xexanos.mealie.feature.auth.ui

import androidx.annotation.StringRes

sealed class CredentialUiState {
    data object Loading : CredentialUiState()
    data class AwaitingInput(
        val username: String = "",
        val password: String = "",
        @StringRes val errorResId: Int? = null,
        val isSubmitting: Boolean = false,
    ) : CredentialUiState()
}
