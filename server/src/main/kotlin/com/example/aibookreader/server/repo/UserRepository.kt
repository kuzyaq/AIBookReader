package com.example.aibookreader.server.repo

import com.example.aibookreader.server.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.UUID

data class UserRow(
    val id: UUID,
    val email: String,
    val passwordHash: String
)

class UserRepository {

    fun create(email: String, passwordHash: String): UUID = transaction {
        val id = Users.insert {
            it[Users.email] = email.lowercase()
            it[Users.passwordHash] = passwordHash
            it[createdAt] = OffsetDateTime.now()
            it[updatedAt] = OffsetDateTime.now()
        } get Users.id
        id.value
    }

    fun findByEmail(email: String): UserRow? = transaction {
        Users.selectAll()
            .where { Users.email eq email.lowercase() }
            .map { row ->
                UserRow(
                    id = row[Users.id].value,
                    email = row[Users.email],
                    passwordHash = row[Users.passwordHash]
                )
            }
            .singleOrNull()
    }

    fun findById(id: UUID): UserRow? = transaction {
        Users.selectAll()
            .where { Users.id eq id }
            .map { row ->
                UserRow(
                    id = row[Users.id].value,
                    email = row[Users.email],
                    passwordHash = row[Users.passwordHash]
                )
            }
            .singleOrNull()
    }

    fun existsByEmail(email: String): Boolean = findByEmail(email) != null

    fun touchUpdatedAt(userId: UUID) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }
}
