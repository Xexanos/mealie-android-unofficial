package dev.xexanos.mealie.core.network.api

import dev.xexanos.mealie.core.network.dto.AuthTokenDto
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthService {
    @FormUrlEncoded
    @POST("api/auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("remember_me") rememberMe: Boolean = true,
    ): Response<AuthTokenDto>
}
