package dev.xexanos.mealie.core.data.domain

sealed class StartupAuthResult {
    data object Success : StartupAuthResult()
    data object CredentialsInvalid : StartupAuthResult()
    data object NoCredentials : StartupAuthResult()
    data object Offline : StartupAuthResult()
}
