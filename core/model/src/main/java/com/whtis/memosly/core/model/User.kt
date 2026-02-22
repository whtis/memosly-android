package com.whtis.memosly.core.model

data class User(
    val name: String,
    val id: Int,
    val role: UserRole,
    val username: String,
    val email: String,
    val nickname: String,
    val avatarUrl: String,
    val description: String,
    val createTime: String,
    val updateTime: String,
)

enum class UserRole {
    HOST,
    ADMIN,
    USER,
    UNKNOWN,
}
