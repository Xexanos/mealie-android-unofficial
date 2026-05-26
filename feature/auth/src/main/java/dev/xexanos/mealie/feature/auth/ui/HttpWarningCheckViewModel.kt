package dev.xexanos.mealie.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.xexanos.mealie.core.data.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HttpWarningCheckViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HttpWarningCheckUiState>(HttpWarningCheckUiState.Loading)
    val uiState: StateFlow<HttpWarningCheckUiState> = _uiState.asStateFlow()

    private val _events = Channel<HttpWarningCheckUiEvent>(Channel.BUFFERED)
    val events: Flow<HttpWarningCheckUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val serverUrl = authRepository.getStoredServerUrl().first() ?: return@launch
            if (serverUrl.startsWith("https://", ignoreCase = true)) {
                _events.send(HttpWarningCheckUiEvent.NavigateToCredentials)
            } else if (authRepository.isHttpWarningAcknowledged(serverUrl)) {
                _events.send(HttpWarningCheckUiEvent.NavigateToCredentials)
            } else {
                _uiState.value = HttpWarningCheckUiState.ShowWarning(serverUrl)
            }
        }
    }

    fun onContinue() {
        val state = _uiState.value
        if (state !is HttpWarningCheckUiState.ShowWarning) return
        viewModelScope.launch {
            authRepository.acknowledgeHttpWarning(state.serverUrl)
            _events.send(HttpWarningCheckUiEvent.NavigateToCredentials)
        }
    }
}
