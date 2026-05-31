package dev.xexanos.mealie.core.data.domain

sealed class AuthResult {
    data class Success(val accessToken: String) : AuthResult()
    data object InvalidCredentials : AuthResult()
    data object NetworkError : AuthResult()
}
