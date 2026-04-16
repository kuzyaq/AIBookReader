package com.example.aibookreader.data.remote.auth


data class RegisterRequestDto(
    val email: String,
    val password: String
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RefreshRequestDto(
    val refreshToken: String
)

data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long
)

data class UserResponseDto(
    val id: String,
    val email: String,
    val displayName: String? = null
)

data class UpdateProfileRequestDto(
    val displayName: String? = null
)

data class ErrorResponseDto(
    val error: String,
    val code: String? = null
)
