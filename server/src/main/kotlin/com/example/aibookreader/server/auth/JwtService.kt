package com.example.aibookreader.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.aibookreader.server.AppConfig
import java.util.Date
import java.util.UUID

class JwtService(private val config: AppConfig) {

    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun createAccessToken(userId: UUID, email: String): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + config.accessTokenTtlSeconds * 1000))
            .sign(algorithm)
    }

    fun verifier() = JWT.require(algorithm)
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .build()
}
