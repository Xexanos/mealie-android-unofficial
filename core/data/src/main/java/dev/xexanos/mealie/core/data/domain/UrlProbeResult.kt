package dev.xexanos.mealie.core.data.domain

sealed class UrlProbeResult {
    data object Success : UrlProbeResult()
    data object NetworkError : UrlProbeResult()
    data object NotMealieServer : UrlProbeResult()
}
