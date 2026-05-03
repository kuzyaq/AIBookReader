package com.example.aibookreader.server

import com.auth0.jwt.JWT
import com.example.aibookreader.server.api.AuthService
import com.example.aibookreader.server.api.dto.ErrorResponse
import com.example.aibookreader.server.api.dto.InitLibraryBookRequest
import com.example.aibookreader.server.api.dto.RegisterRequest
import com.example.aibookreader.server.api.dto.TokenResponse
import com.example.aibookreader.server.api.dto.UserResponse
import com.example.aibookreader.server.auth.JwtService
import com.example.aibookreader.server.auth.PasswordHasher
import com.example.aibookreader.server.auth.TokenHasher
import com.example.aibookreader.server.repo.LibraryRepository
import com.example.aibookreader.server.repo.RefreshTokenRepository
import com.example.aibookreader.server.repo.UserRepository
import com.example.aibookreader.server.repo.UserRow
import com.example.aibookreader.server.repo.ValidRefreshToken
import com.example.aibookreader.server.storage.S3StorageService
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Все модульные тесты сервера в одном месте: в IDE дерево как у pytest в одном файле —
 * верхний узел [ServerTests], внутри вложенные группы [@Nested], затем отдельные [@Test].
 *
 * Запуск: ПКМ по классу [ServerTests] → Run, или по отдельной группе внутри него.
 */
@DisplayName("Сервер — модульные тесты")
class ServerTests {

    @Nested
    @DisplayName("PasswordHasher")
    inner class PasswordHasherTests {

        @Test
        fun verifyAcceptsPlainPasswordForOwnHash() {
            val plain = "correct-horse-battery-staple"
            val hash = PasswordHasher.hash(plain)
            assertTrue(PasswordHasher.verify(plain, hash))
        }

        @Test
        fun verifyRejectsWrongPassword() {
            val hash = PasswordHasher.hash("secret-one")
            assertFalse(PasswordHasher.verify("secret-two", hash))
        }
    }

    @Nested
    @DisplayName("TokenHasher")
    inner class TokenHasherTests {

        @Test
        fun sha256Hex_matchesKnownVectorForUtf8Hello() {
            val expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
            assertEquals(expected, TokenHasher.sha256Hex("hello".toByteArray(Charsets.UTF_8)))
        }
    }

    @Nested
    @DisplayName("JwtService")
    inner class JwtServiceTests {

        @Test
        fun createAccessToken_containsSubjectAndEmailClaims() {
            val config = testAppConfig()
            val jwtService = JwtService(config)
            val userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
            val email = "reader@example.com"

            val token = jwtService.createAccessToken(userId, email)
            val decoded = JWT.decode(token)

            assertEquals(userId.toString(), decoded.subject)
            assertEquals(email, decoded.getClaim("email").asString())
            assertEquals(config.jwtIssuer, decoded.issuer)
            assertEquals(config.jwtAudience, decoded.audience.first())
        }

        @Test
        fun verifierAcceptsTokenFromSameService() {
            val jwtService = JwtService(testAppConfig())
            val userId = UUID.randomUUID()
            val token = jwtService.createAccessToken(userId, "u@u.com")
            jwtService.verifier().verify(token)
        }
    }

    @Nested
    @DisplayName("AuthService")
    inner class AuthServiceTests {

        private val config = testAppConfig()
        private val users = mockk<UserRepository>()
        private val refreshTokens = mockk<RefreshTokenRepository>()
        private val jwt = mockk<JwtService>()
        private val auth = AuthService(config, users, refreshTokens, jwt)

        @AfterEach
        fun tearDown() {
            unmockkAll()
        }

        @Test
        fun register_rejectsInvalidEmail() {
            val r = auth.register("not-an-email", "password123")
            assertTrue(r.isFailure)
            assertTrue(r.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        fun register_rejectsShortPassword() {
            val r = auth.register("a@b.com", "short")
            assertTrue(r.isFailure)
            assertTrue(r.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        fun register_rejectsDuplicateUser() {
            every { users.existsByEmail("dup@example.com") } returns true
            val r = auth.register("dup@example.com", "password123")
            assertTrue(r.isFailure)
            assertTrue(r.exceptionOrNull() is IllegalStateException)
        }

        @Test
        fun register_createsUserAndReturnsTokens() {
            val userId = UUID.randomUUID()
            every { users.existsByEmail("new@example.com") } returns false
            val hashSlot = slot<String>()
            every { users.create("new@example.com", capture(hashSlot)) } returns userId
            every { users.findById(userId) } answers {
                UserRow(
                    id = userId,
                    email = "new@example.com",
                    displayName = null,
                    passwordHash = hashSlot.captured
                )
            }
            every { jwt.createAccessToken(userId, "new@example.com") } returns "access-jwt"
            val refreshHashSlot = slot<String>()
            val refreshExpSlot = slot<java.time.OffsetDateTime>()
            every { refreshTokens.insert(userId, capture(refreshHashSlot), capture(refreshExpSlot)) } returns UUID.randomUUID()

            val r = auth.register("new@example.com", "password123")
            assertTrue(r.isSuccess)
            val pair = r.getOrThrow()
            assertEquals("access-jwt", pair.accessToken)
            assertEquals(config.accessTokenTtlSeconds, pair.expiresInSeconds)
            assertTrue(PasswordHasher.verify("password123", hashSlot.captured))
            assertTrue(refreshHashSlot.isCaptured)
            verify(exactly = 1) { refreshTokens.insert(userId, refreshHashSlot.captured, refreshExpSlot.captured) }
        }

        @Test
        fun login_rejectsUnknownEmail() {
            every { users.findByEmail("ghost@example.com") } returns null
            val r = auth.login("ghost@example.com", "password123")
            assertTrue(r.isFailure)
            assertTrue(r.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        fun login_rejectsWrongPassword() {
            val userId = UUID.randomUUID()
            val hash = PasswordHasher.hash("real-password")
            every { users.findByEmail("u@u.com") } returns UserRow(
                id = userId,
                email = "u@u.com",
                displayName = null,
                passwordHash = hash
            )
            val r = auth.login("u@u.com", "wrong-password")
            assertTrue(r.isFailure)
        }

        @Test
        fun login_updatesUserAndReturnsTokens() {
            val userId = UUID.randomUUID()
            val hash = PasswordHasher.hash("ok-password-1")
            every { users.findByEmail("ok@example.com") } returns UserRow(
                id = userId,
                email = "ok@example.com",
                displayName = null,
                passwordHash = hash
            )
            every { users.touchUpdatedAt(userId) } returns Unit
            every { jwt.createAccessToken(userId, "ok@example.com") } returns "jwt-access"
            val loginRefreshHash = slot<String>()
            val loginRefreshExp = slot<java.time.OffsetDateTime>()
            every { refreshTokens.insert(userId, capture(loginRefreshHash), capture(loginRefreshExp)) } returns UUID.randomUUID()

            val r = auth.login("ok@example.com", "ok-password-1")
            assertTrue(r.isSuccess)
            verify(exactly = 1) { users.touchUpdatedAt(userId) }
        }

        @Test
        fun refresh_rejectsUnknownToken() {
            val h = TokenHasher.sha256Hex("opaque-refresh".toByteArray(Charsets.UTF_8))
            every { refreshTokens.findValidByHash(h) } returns null
            val r = auth.refresh("opaque-refresh")
            assertTrue(r.isFailure)
        }

        @Test
        fun refresh_rotatesTokenAndReturnsNewPair() {
            val userId = UUID.randomUUID()
            val rowId = UUID.randomUUID()
            val raw = "client-refresh-token"
            val hash = TokenHasher.sha256Hex(raw.toByteArray(Charsets.UTF_8))
            every { refreshTokens.findValidByHash(hash) } returns ValidRefreshToken(rowId, userId)
            every { users.findById(userId) } returns UserRow(
                id = userId,
                email = "r@r.com",
                displayName = null,
                passwordHash = "x"
            )
            every { refreshTokens.deleteById(rowId) } returns Unit
            every { jwt.createAccessToken(userId, "r@r.com") } returns "new-access"
            val rh = slot<String>()
            val re = slot<java.time.OffsetDateTime>()
            every { refreshTokens.insert(userId, capture(rh), capture(re)) } returns UUID.randomUUID()

            val r = auth.refresh(raw)
            assertTrue(r.isSuccess)
            verify(exactly = 1) { refreshTokens.deleteById(rowId) }
        }
    }

    @Nested
    @DisplayName("HTTP API (Ktor test host)")
    inner class HttpApiTests {

        @AfterEach
        fun tearDown() {
            unmockkAll()
        }

        private fun testJson() = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        private fun ApplicationTestBuilder.createJsonClient() = createClient {
            install(ContentNegotiation) {
                json(testJson())
            }
        }

        @Test
        fun health_returnsOk() = testApplication {
            val config = testAppConfig()
            val jwtService = JwtService(config)
            val auth = mockk<AuthService>(relaxed = true)
            val users = mockk<UserRepository>(relaxed = true)
            val library = mockk<LibraryRepository>(relaxed = true)
            val s3 = mockk<S3StorageService>(relaxed = true)
            application {
                configureTestModule(jwtService, config, auth, users, library, s3)
            }
            val response = client.get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
        }

        @Test
        fun register_mapsIllegalArgumentToBadRequest() = testApplication {
            val config = testAppConfig()
            val jwtService = JwtService(config)
            val auth = mockk<AuthService>()
            every { auth.register("bad", "password123") } returns Result.failure(IllegalArgumentException("Некорректный email"))
            val users = mockk<UserRepository>(relaxed = true)
            val library = mockk<LibraryRepository>(relaxed = true)
            val s3 = mockk<S3StorageService>(relaxed = true)
            application {
                configureTestModule(jwtService, config, auth, users, library, s3)
            }
            val http = createJsonClient()
            val response = http.post("/auth/register") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(RegisterRequest("bad", "password123"))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val err = response.body<ErrorResponse>()
            assertEquals("Некорректный email", err.error)
        }

        @Test
        fun register_success_returnsCreatedAndTokens() = testApplication {
            val config = testAppConfig()
            val jwtService = JwtService(config)
            val auth = mockk<AuthService>()
            every {
                auth.register("ok@example.com", "password123")
            } returns Result.success(AuthService.TokenPair("access-x", "refresh-y", 900L))
            val users = mockk<UserRepository>(relaxed = true)
            val library = mockk<LibraryRepository>(relaxed = true)
            val s3 = mockk<S3StorageService>(relaxed = true)
            application {
                configureTestModule(jwtService, config, auth, users, library, s3)
            }
            val http = createJsonClient()
            val response = http.post("/auth/register") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(RegisterRequest("ok@example.com", "password123"))
            }
            assertEquals(HttpStatusCode.Created, response.status)
            val body = response.body<TokenResponse>()
            assertEquals("access-x", body.accessToken)
            assertEquals("refresh-y", body.refreshToken)
            assertEquals(900L, body.expiresInSeconds)
        }

        @Test
        fun usersMe_returnsProfileWhenAuthorized() = testApplication {
            val config = testAppConfig()
            val jwtService = JwtService(config)
            val auth = mockk<AuthService>(relaxed = true)
            val userId = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
            val users = mockk<UserRepository>()
            every { users.findById(userId) } returns UserRow(
                id = userId,
                email = "me@example.com",
                displayName = "Reader",
                passwordHash = "hash"
            )
            val library = mockk<LibraryRepository>(relaxed = true)
            val s3 = mockk<S3StorageService>(relaxed = true)
            application {
                configureTestModule(jwtService, config, auth, users, library, s3)
            }
            val token = jwtService.createAccessToken(userId, "me@example.com")
            val http = createJsonClient()
            val response = http.get("/users/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val user = response.body<UserResponse>()
            assertEquals(userId.toString(), user.id)
            assertEquals("me@example.com", user.email)
            assertEquals("Reader", user.displayName)
        }

        @Test
        fun libraryBooksInit_rejectsInvalidFormat() = testApplication {
            val config = testAppConfig()
            val jwtService = JwtService(config)
            val userId = UUID.randomUUID()
            val auth = mockk<AuthService>(relaxed = true)
            val users = mockk<UserRepository>(relaxed = true)
            val library = mockk<LibraryRepository>(relaxed = true)
            val s3 = mockk<S3StorageService>(relaxed = true)
            application {
                configureTestModule(jwtService, config, auth, users, library, s3)
            }
            val token = jwtService.createAccessToken(userId, "u@u.com")
            val http = createJsonClient()
            val response = http.post("/library/books/init") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    InitLibraryBookRequest(
                        title = "T",
                        author = "A",
                        format = "MOBI",
                        fileSize = 100L
                    )
                )
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }
}
