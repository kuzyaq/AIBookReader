package com.example.aibookreader.data.remote.auth

import com.example.aibookreader.data.local.auth.AccessTokenHolder
import com.example.aibookreader.data.local.auth.AuthTokenStorage
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenRefresher @Inject constructor(
    @Named("publicApi") private val publicApi: AuthApiService,
    private val tokenStorage: AuthTokenStorage,
    private val accessTokenHolder: AccessTokenHolder
) {

    suspend fun refresh(): Boolean {
        val refresh = tokenStorage.getRefreshToken() ?: return false
        return try {
            val tokens = publicApi.refresh(RefreshRequestDto(refresh))
            tokenStorage.saveRefreshToken(tokens.refreshToken)
            accessTokenHolder.setToken(tokens.accessToken)
            true
        } catch (_: Exception) {
            false
        }
    }
}
