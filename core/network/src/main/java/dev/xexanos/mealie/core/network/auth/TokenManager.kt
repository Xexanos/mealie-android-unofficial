package dev.xexanos.mealie.core.network.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenManager {
    private val mutex = Mutex()
    private val _currentToken = MutableStateFlow("")
    private val _authFailureEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val currentToken: StateFlow<String> = _currentToken.asStateFlow()
    val authFailureEvent: SharedFlow<Unit> = _authFailureEvent.asSharedFlow()

    suspend fun setToken(token: String) = mutex.withLock {
        _currentToken.value = token
    }

    suspend fun clearToken() = mutex.withLock {
        _currentToken.value = ""
    }

    fun hasToken(): Boolean = _currentToken.value.isNotEmpty()

    suspend fun signalAuthFailure() {
        _authFailureEvent.emit(Unit)
    }
}
