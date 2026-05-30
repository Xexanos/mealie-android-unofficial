package dev.xexanos.mealie.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.xexanos.mealie.core.data.domain.StartupAuthResult
import dev.xexanos.mealie.core.data.domain.StartupAuthUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class StartupAuthViewModel(
    private val startupAuthUseCase: StartupAuthUseCase,
) : ViewModel() {

    private val _event = Channel<StartupAuthEvent>(Channel.BUFFERED)
    val event: Flow<StartupAuthEvent> = _event.receiveAsFlow()

    init {
        viewModelScope.launch {
            val result = startupAuthUseCase.execute()
            val navEvent = when (result) {
                StartupAuthResult.Success -> StartupAuthEvent.NavigateToMain
                StartupAuthResult.Offline -> StartupAuthEvent.NavigateToMain
                StartupAuthResult.CredentialsInvalid -> StartupAuthEvent.NavigateToCredentials
                StartupAuthResult.NoCredentials -> StartupAuthEvent.NavigateToSetup
            }
            _event.send(navEvent)
        }
    }
}
