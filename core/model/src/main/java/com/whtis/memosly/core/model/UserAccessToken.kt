package com.whtis.memosly.core.model

data class UserAccessToken(
    val accessToken: String,
    val description: String,
    val issuedAt: String,
    val expiresAt: String,
)
