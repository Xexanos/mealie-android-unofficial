package dev.xexanos.mealie.core.data.domain

sealed class AuthResult {
    data object Success : AuthResult()
    data object InvalidCredentials : AuthResult()
    data object NetworkError : AuthResult()
}
