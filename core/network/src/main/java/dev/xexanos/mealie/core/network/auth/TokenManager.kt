package dev.xexanos.mealie.core.network.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenManager {
    private val mutex = Mutex()
    private val _currentToken = MutableStateFlow("")

    val currentToken: StateFlow<String> = _currentToken.asStateFlow()

    suspend fun setToken(token: String) = mutex.withLock {
        _currentToken.value = token
    }

    suspend fun clearToken() = mutex.withLock {
        _currentToken.value = ""
    }

    fun hasToken(): Boolean = _currentToken.value.isNotEmpty()
}
