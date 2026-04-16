package com.example.aibookreader.data.local.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

@Singleton
class AuthTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val store get() = context.authDataStore

    private val refreshKey = stringPreferencesKey("refresh_token")

    val refreshTokenFlow: Flow<String?> = store.data.map { it[refreshKey] }

    suspend fun getRefreshToken(): String? = store.data.map { it[refreshKey] }.first()

    suspend fun saveRefreshToken(token: String) {
        store.edit { it[refreshKey] = token }
    }

    suspend fun clear() {
        store.edit { it.remove(refreshKey) }
    }
}
