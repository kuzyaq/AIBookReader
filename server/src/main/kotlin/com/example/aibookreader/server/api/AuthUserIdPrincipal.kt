package com.example.aibookreader.server.api

import io.ktor.server.auth.Principal
import java.util.UUID

data class AuthUserIdPrincipal(val userId: UUID) : Principal
