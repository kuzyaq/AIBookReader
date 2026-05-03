package com.example.aibookreader.server.repo

import com.example.aibookreader.server.db.LibraryBooks
import com.example.aibookreader.server.db.LibraryChatMessages
import com.example.aibookreader.server.db.LibraryReadingProgress
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.util.UUID

data class LibraryBookRow(
    val id: UUID,
    val userId: UUID,
    val title: String,
    val author: String,
    val format: String,
    val fileSize: Long,
    val storageKey: String,
    val status: String,
    val metadataVersion: Long
)

data class LibraryProgressRow(
    val libraryBookId: UUID,
    val locatorJson: String?,
    val currentPageIndex: Int,
    val progressFraction: Float?,
    val lastReadAtMs: Long,
    val version: Long
)

data class LibraryChatRow(
    val id: UUID,
    val clientMessageId: UUID,
    val role: String,
    val content: String,
    val createdAtMs: Long,
    val serverCreatedAt: OffsetDateTime
)

class LibraryRepository {

    fun insertBookPending(
        userId: UUID,
        title: String,
        author: String,
        format: String,
        fileSize: Long
    ): UUID = transaction {
        LibraryBooks.insert {
            it[LibraryBooks.userId] = userId
            it[LibraryBooks.title] = title
            it[LibraryBooks.author] = author
            it[LibraryBooks.format] = format
            it[LibraryBooks.fileSize] = fileSize
            it[LibraryBooks.storageKey] = "pending"
            it[LibraryBooks.status] = "pending_upload"
            it[LibraryBooks.metadataVersion] = 1L
            it[LibraryBooks.createdAt] = OffsetDateTime.now()
            it[LibraryBooks.updatedAt] = OffsetDateTime.now()
        } get LibraryBooks.id
    }.value

    fun updateStorageKey(bookId: UUID, storageKey: String) {
        transaction {
            LibraryBooks.update({ LibraryBooks.id eq bookId }) {
                it[LibraryBooks.storageKey] = storageKey
                it[LibraryBooks.updatedAt] = OffsetDateTime.now()
            }
        }
    }

    fun markReady(bookId: UUID, userId: UUID) {
        transaction {
            LibraryBooks.update({ (LibraryBooks.id eq bookId) and (LibraryBooks.userId eq userId) }) {
                it[LibraryBooks.status] = "ready"
                it[LibraryBooks.updatedAt] = OffsetDateTime.now()
            }
        }
    }

    fun findBook(bookId: UUID, userId: UUID): LibraryBookRow? = transaction {
        LibraryBooks.selectAll()
            .where { (LibraryBooks.id eq bookId) and (LibraryBooks.userId eq userId) }
            .map { row ->
                LibraryBookRow(
                    id = row[LibraryBooks.id].value,
                    userId = row[LibraryBooks.userId].value,
                    title = row[LibraryBooks.title],
                    author = row[LibraryBooks.author],
                    format = row[LibraryBooks.format],
                    fileSize = row[LibraryBooks.fileSize],
                    storageKey = row[LibraryBooks.storageKey],
                    status = row[LibraryBooks.status],
                    metadataVersion = row[LibraryBooks.metadataVersion]
                )
            }
            .singleOrNull()
    }

    fun listBooks(userId: UUID): List<LibraryBookRow> = transaction {
        LibraryBooks.selectAll()
            .where { LibraryBooks.userId eq userId }
            .map { row ->
                LibraryBookRow(
                    id = row[LibraryBooks.id].value,
                    userId = row[LibraryBooks.userId].value,
                    title = row[LibraryBooks.title],
                    author = row[LibraryBooks.author],
                    format = row[LibraryBooks.format],
                    fileSize = row[LibraryBooks.fileSize],
                    storageKey = row[LibraryBooks.storageKey],
                    status = row[LibraryBooks.status],
                    metadataVersion = row[LibraryBooks.metadataVersion]
                )
            }
            .sortedByDescending { it.title }
    }

    fun getProgress(bookId: UUID, userId: UUID): LibraryProgressRow? = transaction {
        LibraryReadingProgress.selectAll()
            .where {
                (LibraryReadingProgress.libraryBookId eq bookId) and (LibraryReadingProgress.userId eq userId)
            }
            .map { row ->
                LibraryProgressRow(
                    libraryBookId = row[LibraryReadingProgress.libraryBookId].value,
                    locatorJson = row[LibraryReadingProgress.locatorJson],
                    currentPageIndex = row[LibraryReadingProgress.currentPageIndex],
                    progressFraction = row[LibraryReadingProgress.progressFraction],
                    lastReadAtMs = row[LibraryReadingProgress.lastReadAtMs],
                    version = row[LibraryReadingProgress.version]
                )
            }
            .singleOrNull()
    }

    fun upsertProgress(
        bookId: UUID,
        userId: UUID,
        locatorJson: String?,
        currentPageIndex: Int,
        progressFraction: Float?,
        lastReadAtMs: Long,
        expectedVersion: Long?
    ): LibraryProgressRow = transaction {
        val existing = LibraryReadingProgress.selectAll()
            .where {
                (LibraryReadingProgress.libraryBookId eq bookId) and (LibraryReadingProgress.userId eq userId)
            }
            .singleOrNull()

        val nextVersion = if (existing == null) {
            1L
        } else {
            val v = existing[LibraryReadingProgress.version]
            if (expectedVersion != null && expectedVersion != v) {
                throw IllegalStateException("VERSION_CONFLICT")
            }
            v + 1
        }

        if (existing == null) {
            LibraryReadingProgress.insert {
                it[LibraryReadingProgress.libraryBookId] = bookId
                it[LibraryReadingProgress.userId] = userId
                it[LibraryReadingProgress.locatorJson] = locatorJson
                it[LibraryReadingProgress.currentPageIndex] = currentPageIndex
                it[LibraryReadingProgress.progressFraction] = progressFraction
                it[LibraryReadingProgress.lastReadAtMs] = lastReadAtMs
                it[LibraryReadingProgress.version] = nextVersion
                it[LibraryReadingProgress.updatedAt] = OffsetDateTime.now()
            }
        } else {
            LibraryReadingProgress.update({
                (LibraryReadingProgress.libraryBookId eq bookId) and (LibraryReadingProgress.userId eq userId)
            }) {
                it[LibraryReadingProgress.locatorJson] = locatorJson
                it[LibraryReadingProgress.currentPageIndex] = currentPageIndex
                it[LibraryReadingProgress.progressFraction] = progressFraction
                it[LibraryReadingProgress.lastReadAtMs] = lastReadAtMs
                it[LibraryReadingProgress.version] = nextVersion
                it[LibraryReadingProgress.updatedAt] = OffsetDateTime.now()
            }
        }

        LibraryReadingProgress.selectAll()
            .where {
                (LibraryReadingProgress.libraryBookId eq bookId) and (LibraryReadingProgress.userId eq userId)
            }
            .map { row ->
                LibraryProgressRow(
                    libraryBookId = row[LibraryReadingProgress.libraryBookId].value,
                    locatorJson = row[LibraryReadingProgress.locatorJson],
                    currentPageIndex = row[LibraryReadingProgress.currentPageIndex],
                    progressFraction = row[LibraryReadingProgress.progressFraction],
                    lastReadAtMs = row[LibraryReadingProgress.lastReadAtMs],
                    version = row[LibraryReadingProgress.version]
                )
            }
            .single()
    }

    fun insertChatMessagesIgnoreDuplicates(
        bookId: UUID,
        userId: UUID,
        messages: List<Triple<UUID, String, Pair<String, Long>>>
    ) {
        transaction {
            messages.forEach { (clientId, role, pair) ->
                val (content, createdAtMs) = pair
                try {
                    LibraryChatMessages.insert {
                        it[LibraryChatMessages.libraryBookId] = bookId
                        it[LibraryChatMessages.userId] = userId
                        it[LibraryChatMessages.clientMessageId] = clientId
                        it[LibraryChatMessages.role] = role
                        it[LibraryChatMessages.content] = content
                        it[LibraryChatMessages.createdAtMs] = createdAtMs
                        it[LibraryChatMessages.serverCreatedAt] = OffsetDateTime.now()
                    }
                } catch (_: Exception) {
                    // duplicate (library_book_id, client_message_id)
                }
            }
        }
    }

    fun listChatAfter(
        bookId: UUID,
        userId: UUID,
        after: OffsetDateTime?
    ): List<LibraryChatRow> = transaction {
        val condition = if (after == null) {
            (LibraryChatMessages.libraryBookId eq bookId) and (LibraryChatMessages.userId eq userId)
        } else {
            (LibraryChatMessages.libraryBookId eq bookId) and (LibraryChatMessages.userId eq userId) and
                (LibraryChatMessages.serverCreatedAt greater after)
        }
        val q = LibraryChatMessages.selectAll().where { condition }
        q.map { row ->
            LibraryChatRow(
                id = row[LibraryChatMessages.id].value,
                clientMessageId = row[LibraryChatMessages.clientMessageId],
                role = row[LibraryChatMessages.role],
                content = row[LibraryChatMessages.content],
                createdAtMs = row[LibraryChatMessages.createdAtMs],
                serverCreatedAt = row[LibraryChatMessages.serverCreatedAt]
            )
        }.sortedBy { it.serverCreatedAt }
    }
}
