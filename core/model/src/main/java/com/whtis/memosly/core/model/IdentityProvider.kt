package com.whtis.memosly.core.model

data class IdentityProvider(
    val name: String,
    val id: Int,
    val type: IdentityProviderType,
    val title: String,
    val identifierFilter: String,
    val config: IdentityProviderConfig,
)

enum class IdentityProviderType {
    OAUTH2,
    UNKNOWN,
}

data class IdentityProviderConfig(
    val oauth2: OAuth2Config?,
)

data class OAuth2Config(
    val clientId: String,
    val clientSecret: String,
    val authUrl: String,
    val tokenUrl: String,
    val userInfoUrl: String,
    val scopes: List<String>,
    val fieldMapping: FieldMapping,
)

data class FieldMapping(
    val identifier: String,
    val displayName: String,
    val email: String,
)
