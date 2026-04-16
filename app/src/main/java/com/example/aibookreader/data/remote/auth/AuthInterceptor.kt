package com.example.aibookreader.data.remote.auth

import com.example.aibookreader.data.local.auth.AccessTokenHolder
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val accessTokenHolder: AccessTokenHolder
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath
        if (path.contains("/auth/login") || path.contains("/auth/register") || path.contains("/auth/refresh")) {
            return chain.proceed(req)
        }
        val token = accessTokenHolder.accessToken ?: return chain.proceed(req)
        return chain.proceed(
            req.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
