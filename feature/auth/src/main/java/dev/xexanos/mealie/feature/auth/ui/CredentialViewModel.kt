package dev.xexanos.mealie.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.xexanos.mealie.core.data.domain.AuthResult
import dev.xexanos.mealie.core.data.repository.AuthRepository
import dev.xexanos.mealie.core.ui.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CredentialViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<CredentialUiState>(CredentialUiState.AwaitingInput())
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    private val _events = Channel<CredentialUiEvent>(Channel.BUFFERED)
    val events: Flow<CredentialUiEvent> = _events.receiveAsFlow()

    fun onUsernameChanged(value: String) {
        val current = _uiState.value as? CredentialUiState.AwaitingInput ?: return
        _uiState.value = current.copy(username = value, errorResId = null)
    }

    fun onPasswordChanged(value: String) {
        val current = _uiState.value as? CredentialUiState.AwaitingInput ?: return
        _uiState.value = current.copy(password = value, errorResId = null)
    }

    fun onSignIn() {
        val current = _uiState.value as? CredentialUiState.AwaitingInput ?: return
        if (current.isSubmitting) return

        val trimmedUsername = current.username.trim()
        if (trimmedUsername.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(errorResId = R.string.credential_error_empty)
            return
        }

        val submitted = current.copy(
            username = trimmedUsername,
            isSubmitting = true,
            errorResId = null,
        )
        _uiState.value = submitted

        viewModelScope.launch {
            when (authRepository.authenticate(trimmedUsername, current.password)) {
                AuthResult.Success -> {
                    _uiState.value = submitted.copy(isSubmitting = false)
                    _events.send(CredentialUiEvent.NavigateToMain)
                }
                AuthResult.InvalidCredentials -> {
                    _uiState.value = submitted.copy(
                        password = "",
                        isSubmitting = false,
                        errorResId = R.string.credential_error_invalid,
                    )
                }
                AuthResult.NetworkError -> {
                    _uiState.value = submitted.copy(
                        isSubmitting = false,
                        errorResId = R.string.credential_error_network,
                    )
                }
            }
        }
    }
}
