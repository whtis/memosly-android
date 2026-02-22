package com.whtis.memosly.core.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IdentityProviderDto(
    val name: String?,
    val id: Int?,
    val type: String?,
    val title: String?,
    val identifierFilter: String?,
    val config: IdentityProviderConfigDto?,
)

@JsonClass(generateAdapter = true)
data class IdentityProviderConfigDto(
    val oauth2Config: OAuth2ConfigDto?,
)

@JsonClass(generateAdapter = true)
data class OAuth2ConfigDto(
    val clientId: String?,
    val clientSecret: String?,
    val authUrl: String?,
    val tokenUrl: String?,
    val userInfoUrl: String?,
    val scopes: List<String>?,
    val fieldMapping: FieldMappingDto?,
)

@JsonClass(generateAdapter = true)
data class FieldMappingDto(
    val identifier: String?,
    val displayName: String?,
    val email: String?,
)

@JsonClass(generateAdapter = true)
data class ListIdentityProvidersResponse(
    val identityProviders: List<IdentityProviderDto>,
)
