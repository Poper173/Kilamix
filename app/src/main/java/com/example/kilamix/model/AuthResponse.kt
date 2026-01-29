package com.itech.kilamix.model


data class AuthResponse(
    val success: Boolean,
    val data: AuthData
)

data class AuthData(
    val token: String,
    val user: User
)
