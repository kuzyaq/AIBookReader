package com.example.aibookreader.data.local.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessTokenHolder @Inject constructor() {

    @Volatile
    var accessToken: String? = null
        private set

    fun setToken(token: String?) {
        accessToken = token
    }

    fun clear() {
        accessToken = null
    }
}
