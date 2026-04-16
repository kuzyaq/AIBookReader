package com.example.aibookreader.server.api

import com.example.aibookreader.server.AppConfig
import com.example.aibookreader.server.auth.JwtService
import com.example.aibookreader.server.auth.PasswordHasher
import com.example.aibookreader.server.auth.TokenHasher
import com.example.aibookreader.server.repo.RefreshTokenRepository
import com.example.aibookreader.server.repo.UserRepository
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

class AuthService(
    private val config: AppConfig,
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val jwt: JwtService
) {

    private val random = SecureRandom()

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val expiresInSeconds: Long
    )

    fun register(email: String, password: String): Result<TokenPair> {
        val trimmed = email.trim()
        if (!emailLooksValid(trimmed)) {
            return Result.failure(IllegalArgumentException("Некорректный email"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Пароль короче 8 символов"))
        }
        if (users.existsByEmail(trimmed)) {
            return Result.failure(IllegalStateException("Пользователь уже существует"))
        }
        val hash = PasswordHasher.hash(password)
        val userId = users.create(trimmed, hash)
        val row = users.findById(userId) ?: return Result.failure(IllegalStateException("Не удалось создать пользователя"))
        return Result.success(issueTokens(row.id, row.email))
    }

    fun login(email: String, password: String): Result<TokenPair> {
        val row = users.findByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("Неверный email или пароль"))
        if (!PasswordHasher.verify(password, row.passwordHash)) {
            return Result.failure(IllegalArgumentException("Неверный email или пароль"))
        }
        users.touchUpdatedAt(row.id)
        return Result.success(issueTokens(row.id, row.email))
    }

    fun refresh(refreshTokenRaw: String): Result<TokenPair> {
        val hash = TokenHasher.sha256Hex(refreshTokenRaw.toByteArray(Charsets.UTF_8))
        val found = refreshTokens.findValidByHash(hash)
            ?: return Result.failure(IllegalArgumentException("Недействительный refresh token"))
        val user = users.findById(found.userId)
            ?: return Result.failure(IllegalStateException("Пользователь не найден"))
        refreshTokens.deleteById(found.rowId)
        return Result.success(issueTokens(user.id, user.email))
    }


    private fun issueTokens(userId: UUID, email: String): TokenPair {
        val access = jwt.createAccessToken(userId, email)
        val rawRefresh = ByteArray(48)
        random.nextBytes(rawRefresh)
        val refreshB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(rawRefresh)
        val refreshHash = TokenHasher.sha256Hex(refreshB64.toByteArray(Charsets.UTF_8))
        val exp = OffsetDateTime.now().plusSeconds(config.refreshTokenTtlSeconds)
        refreshTokens.insert(userId, refreshHash, exp)
        return TokenPair(
            accessToken = access,
            refreshToken = refreshB64,
            expiresInSeconds = config.accessTokenTtlSeconds
        )
    }

    private fun emailLooksValid(email: String): Boolean {
        if (email.length > 255) return false
        return email.contains('@') && email.contains('.')
    }
}
