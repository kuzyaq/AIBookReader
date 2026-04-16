package com.example.aibookreader.data.remote.auth

import com.example.aibookreader.data.local.auth.AccessTokenHolder
import com.example.aibookreader.data.local.auth.AuthTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher,
    private val accessTokenHolder: AccessTokenHolder,
    private val tokenStorage: AuthTokenStorage
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/auth/")) return null
        if (response.priorResponse != null) return null

        val refreshed = runBlocking(Dispatchers.IO) {
            tokenRefresher.refresh()
        }
        if (!refreshed) {
            runBlocking(Dispatchers.IO) {
                tokenStorage.clear()
                accessTokenHolder.clear()
            }
            return null
        }
        val token = accessTokenHolder.accessToken ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }
}
