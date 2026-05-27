package dev.xexanos.mealie.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.xexanos.mealie.core.data.domain.UrlProbeResult
import dev.xexanos.mealie.core.data.repository.AuthRepository
import dev.xexanos.mealie.core.ui.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ServerUrlViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ServerUrlUiState>(ServerUrlUiState.Loading)
    val uiState: StateFlow<ServerUrlUiState> = _uiState.asStateFlow()

    private val _events = Channel<ServerUrlUiEvent>(Channel.BUFFERED)
    val events: Flow<ServerUrlUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val storedUrl = authRepository.getStoredServerUrl().first()
            if (storedUrl != null) {
                _events.send(ServerUrlUiEvent.NavigateToNext)
            } else {
                _uiState.value = ServerUrlUiState.AwaitingInput
            }
        }
    }

    fun onConnect(rawUrl: String) {
        if (_uiState.value is ServerUrlUiState.Probing) return
        val normalized = normalizeUrl(rawUrl)
        if (normalized == null) {
            _uiState.value = ServerUrlUiState.InputError(
                messageResId = R.string.setup_url_error_invalid,
                lastUrl = rawUrl,
            )
            return
        }
        _uiState.value = ServerUrlUiState.Probing(normalized)
        viewModelScope.launch {
            when (authRepository.probeServerUrl(normalized)) {
                UrlProbeResult.Success -> {
                    authRepository.saveServerUrl(normalized)
                    _events.send(ServerUrlUiEvent.NavigateToNext)
                }
                UrlProbeResult.NetworkError -> {
                    _uiState.value = ServerUrlUiState.InputError(
                        messageResId = R.string.setup_url_error_unreachable,
                        lastUrl = normalized,
                    )
                }
                UrlProbeResult.NotMealieServer -> {
                    _uiState.value = ServerUrlUiState.InputError(
                        messageResId = R.string.setup_url_error_not_mealie,
                        lastUrl = normalized,
                    )
                }
            }
        }
    }

    internal fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }

        val stripped = withScheme.trimEnd('/')

        return try {
            val uri = java.net.URI(stripped)
            if (uri.host.isNullOrBlank()) null
            else if (uri.port == 0) null
            else buildString {
                append(uri.scheme.lowercase())
                append("://")
                append(uri.host)
                if (uri.port != -1) append(":${uri.port}")
                val path = stripMealieUiPath(uri.path)
                if (path.isNotEmpty()) append(path)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stripMealieUiPath(path: String?): String {
        if (path.isNullOrBlank() || path == "/") return ""
        val firstSegment = path.trimStart('/').substringBefore('/')
        return if (firstSegment.lowercase() in MEALIE_UI_PATHS) "" else path.trimEnd('/')
    }

    companion object {
        private val MEALIE_UI_PATHS = setOf(
            "login",
            "register",
            "forgot-password",
            "admin",
            "dashboard",
            "g",
            "r",
            "shopping-lists",
            "meal-plan",
            "meal-planner",
            "cookbooks",
            "user",
            "group",
            "api",
        )
    }
}
