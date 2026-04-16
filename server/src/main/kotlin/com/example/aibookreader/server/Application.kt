package com.example.aibookreader.server

import com.example.aibookreader.server.api.AuthService
import com.example.aibookreader.server.api.AuthUserIdPrincipal
import com.example.aibookreader.server.api.configureRouting
import com.example.aibookreader.server.api.dto.ErrorResponse
import com.example.aibookreader.server.auth.JwtService
import com.example.aibookreader.server.db.DatabaseFactory
import com.example.aibookreader.server.repo.RefreshTokenRepository
import com.example.aibookreader.server.repo.UserRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import java.util.UUID

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0", module = { module(config) })
        .start(wait = true)
}

fun Application.module(appConfig: AppConfig = AppConfig.fromEnvironment()) {
    DatabaseFactory.init(appConfig)

    val userRepository = UserRepository()
    val refreshTokenRepository = RefreshTokenRepository()
    val jwtService = JwtService(appConfig)
    val authService = AuthService(appConfig, userRepository, refreshTokenRepository, jwtService)

    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
    }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        )
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = "Внутренняя ошибка сервера")
            )
        }
    }

    install(Authentication) {
        jwt("jwt-auth") {
            realm = "aibookreader"
            verifier(jwtService.verifier())
            validate { credential ->
                val jwtPrincipal = credential.payload
                val subject = jwtPrincipal.subject ?: return@validate null
                val userId = runCatching { UUID.fromString(subject) }.getOrNull() ?: return@validate null
                AuthUserIdPrincipal(userId)
            }
        }
    }

    configureRouting(authService, userRepository)
}
