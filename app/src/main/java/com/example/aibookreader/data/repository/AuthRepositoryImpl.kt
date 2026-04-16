package com.example.aibookreader.data.repository

import com.example.aibookreader.data.local.auth.AccessTokenHolder
import com.example.aibookreader.data.local.auth.AuthTokenStorage
import com.example.aibookreader.data.remote.auth.AuthApiService
import com.example.aibookreader.data.remote.auth.ErrorResponseDto
import com.example.aibookreader.data.remote.auth.LoginRequestDto
import com.example.aibookreader.data.remote.auth.RegisterRequestDto
import com.example.aibookreader.data.remote.auth.TokenRefresher
import com.google.gson.Gson
import com.example.aibookreader.domain.model.AuthUser
import com.example.aibookreader.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @Named("publicApi") private val publicApi: AuthApiService,
    @Named("authedApi") private val authedApi: AuthApiService,
    private val tokenStorage: AuthTokenStorage,
    private val accessTokenHolder: AccessTokenHolder,
    private val tokenRefresher: TokenRefresher
) : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    override suspend fun restoreSession(): Boolean {
        if (tokenStorage.getRefreshToken() == null) return false
        if (!tokenRefresher.refresh()) {
            tokenStorage.clear()
            accessTokenHolder.clear()
            _currentUser.value = null
            return false
        }
        return try {
            loadProfile()
            true
        } catch (_: Exception) {
            logout()
            false
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val tokens = publicApi.login(LoginRequestDto(email.trim(), password))
            persistTokens(tokens.accessToken, tokens.refreshToken)
            loadProfile()
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(Exception(networkOrUnknownMessage(e), e))
        }
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val tokens = publicApi.register(RegisterRequestDto(email.trim(), password))
            persistTokens(tokens.accessToken, tokens.refreshToken)
            loadProfile()
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception(httpErrorMessage(e)))
        } catch (e: Exception) {
            Result.failure(Exception(networkOrUnknownMessage(e), e))
        }
    }

    override suspend fun logout() {
        tokenStorage.clear()
        accessTokenHolder.clear()
        _currentUser.value = null
    }

    override suspend fun refreshProfile() {
        try {
            loadProfile()
        } catch (_: Exception) {
        }
    }

    private suspend fun loadProfile() {
        val me = authedApi.getMe()
        _currentUser.value = AuthUser(id = me.id, email = me.email)
    }

    private suspend fun persistTokens(access: String, refresh: String) {
        accessTokenHolder.setToken(access)
        tokenStorage.saveRefreshToken(refresh)
    }

    private fun httpErrorMessage(e: HttpException): String {
        val code = e.code()
        val fromBody = runCatching {
            val raw = e.response()?.errorBody()?.string().orEmpty()
            if (raw.isBlank()) return@runCatching null
            Gson().fromJson(raw, ErrorResponseDto::class.java)?.error?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrNull()
        return fromBody ?: friendlyHttpStatusMessage(code)
    }

    private fun friendlyHttpStatusMessage(code: Int): String = when (code) {
        400 -> "Проверьте введённые данные"
        401 -> "Неверный email или пароль"
        403 -> "Доступ запрещён"
        404 -> "Сервис не найден"
        408 -> "Превышено время ожидания"
        409 -> "Такой email уже занят"
        422 -> "Некорректные данные"
        429 -> "Слишком много попыток. Попробуйте позже"
        in 500..599 -> "Сервер временно недоступен. Попробуйте позже"
        else -> "Не удалось выполнить запрос"
    }

    private fun networkOrUnknownMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            e is java.net.UnknownHostException -> "Нет подключения к интернету"
            e is java.net.SocketTimeoutException -> "Превышено время ожидания"
            e is IOException && (
                msg.contains("Unable to resolve host", ignoreCase = true) ||
                    msg.contains("failed to connect", ignoreCase = true) ||
                    msg.contains("Network is unreachable", ignoreCase = true)
                ) -> "Нет подключения к серверу"
            else -> "Проверьте подключение и попробуйте снова"
        }
    }
}
