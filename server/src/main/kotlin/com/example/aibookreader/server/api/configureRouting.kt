package com.example.aibookreader.server.api

import com.example.aibookreader.server.api.dto.ErrorResponse
import com.example.aibookreader.server.api.dto.LoginRequest
import com.example.aibookreader.server.api.dto.RefreshRequest
import com.example.aibookreader.server.api.dto.RegisterRequest
import com.example.aibookreader.server.api.dto.TokenResponse
import com.example.aibookreader.server.api.dto.UserResponse
import com.example.aibookreader.server.repo.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.routing

fun Application.configureRouting(
    authService: AuthService,
    userRepository: UserRepository
) {
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        post("/auth/register") {
            val body = call.receive<RegisterRequest>()
            val result = authService.register(body.email, body.password)
            result.fold(
                onSuccess = { pair ->
                    call.respond(
                        HttpStatusCode.Created,
                        TokenResponse(
                            accessToken = pair.accessToken,
                            refreshToken = pair.refreshToken,
                            expiresInSeconds = pair.expiresInSeconds
                        )
                    )
                },
                onFailure = { e -> respondAuthError(call, e) }
            )
        }

        post("/auth/login") {
            val body = call.receive<LoginRequest>()
            val result = authService.login(body.email, body.password)
            result.fold(
                onSuccess = { pair ->
                    call.respond(
                        TokenResponse(
                            accessToken = pair.accessToken,
                            refreshToken = pair.refreshToken,
                            expiresInSeconds = pair.expiresInSeconds
                        )
                    )
                },
                onFailure = { e -> respondAuthError(call, e) }
            )
        }

        post("/auth/refresh") {
            val body = call.receive<RefreshRequest>()
            val result = authService.refresh(body.refreshToken)
            result.fold(
                onSuccess = { pair ->
                    call.respond(
                        TokenResponse(
                            accessToken = pair.accessToken,
                            refreshToken = pair.refreshToken,
                            expiresInSeconds = pair.expiresInSeconds
                        )
                    )
                },
                onFailure = { e -> respondAuthError(call, e) }
            )
        }

        authenticate("jwt-auth") {
            get("/users/me") {
                val principal = call.principal<AuthUserIdPrincipal>() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Не авторизован"))
                    return@get
                }
                val user = userRepository.findById(principal.userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Пользователь не найден"))
                    return@get
                }
                call.respond(
                    UserResponse(
                        id = user.id.toString(),
                        email = user.email
                    )
                )
            }
        }
    }
}

private suspend fun respondAuthError(
    call: ApplicationCall,
    e: Throwable
) {
    val (code, message) = when (e) {
        is IllegalArgumentException -> HttpStatusCode.BadRequest to (e.message ?: "Ошибка запроса")
        is IllegalStateException -> HttpStatusCode.Conflict to (e.message ?: "Конфликт")
        else -> HttpStatusCode.InternalServerError to "Внутренняя ошибка"
    }
    call.respond(code, ErrorResponse(error = message))
}
