package dev.xexanos.mealie.core.network.result

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object NetworkError : ApiResult<Nothing>()
    data object AuthError : ApiResult<Nothing>()
    data class HttpError(
        val code: Int,
        val detail: String?,
    ) : ApiResult<Nothing>()
}
