package com.example.aibookreader.server.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String,
    val code: String? = null
)
