package com.example.aibookreader.domain.repository

import com.example.aibookreader.domain.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    val currentUser: StateFlow<AuthUser?>

    suspend fun restoreSession(): Boolean

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun register(email: String, password: String): Result<Unit>

    suspend fun logout()

    suspend fun refreshProfile()

    suspend fun updateDisplayName(displayName: String): Result<Unit>
}
