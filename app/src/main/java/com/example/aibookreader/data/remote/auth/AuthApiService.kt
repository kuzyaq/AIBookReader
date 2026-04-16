package com.example.aibookreader.data.remote.auth

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): TokenResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenResponseDto

    @GET("users/me")
    suspend fun getMe(): UserResponseDto

    @PATCH("users/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequestDto): UserResponseDto
}
