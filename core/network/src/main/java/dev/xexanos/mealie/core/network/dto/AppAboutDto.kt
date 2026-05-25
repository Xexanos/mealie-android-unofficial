package dev.xexanos.mealie.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppAboutDto(
    val version: String? = null,
    val production: Boolean? = null,
)
