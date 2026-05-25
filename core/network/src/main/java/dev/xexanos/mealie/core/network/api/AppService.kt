package dev.xexanos.mealie.core.network.api

import dev.xexanos.mealie.core.network.dto.AppAboutDto
import retrofit2.http.GET

interface AppService {
    @GET("api/app/about")
    suspend fun getAppAbout(): AppAboutDto
}
