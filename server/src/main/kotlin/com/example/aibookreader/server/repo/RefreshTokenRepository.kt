package com.example.aibookreader.server.repo

import com.example.aibookreader.server.db.RefreshTokens
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

data class ValidRefreshToken(
    val rowId: UUID,
    val userId: UUID
)

class RefreshTokenRepository {

    fun insert(userId: UUID, tokenHash: String, expiresAt: OffsetDateTime): UUID = transaction {
        val id = RefreshTokens.insert {
            it[RefreshTokens.userId] = userId
            it[RefreshTokens.tokenHash] = tokenHash
            it[RefreshTokens.expiresAt] = expiresAt
            it[RefreshTokens.createdAt] = OffsetDateTime.now()
        } get RefreshTokens.id
        id.value
    }

    fun findValidByHash(tokenHash: String): ValidRefreshToken? = transaction {
        val now = OffsetDateTime.now()
        RefreshTokens.selectAll()
            .where {
                (RefreshTokens.tokenHash eq tokenHash) and (RefreshTokens.expiresAt greater now)
            }
            .map { row ->
                ValidRefreshToken(
                    rowId = row[RefreshTokens.id].value,
                    userId = row[RefreshTokens.userId].value
                )
            }
            .singleOrNull()
    }

    fun deleteById(id: UUID) {
        transaction {
            RefreshTokens.deleteWhere { RefreshTokens.id eq id }
        }
    }

    fun deleteExpired() {
        transaction {
            val now = OffsetDateTime.now()
            RefreshTokens.deleteWhere { RefreshTokens.expiresAt lessEq now }
        }
    }
}
