package com.example.aibookreader.server.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Users : UUIDTable("users") {
    val email = varchar("email", 255)
    val displayName = varchar("display_name", 255).nullable()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object RefreshTokens : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val tokenHash = varchar("token_hash", 64)
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdAt = timestampWithTimeZone("created_at")
}

object LibraryBooks : UUIDTable("library_books") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 512)
    val author = varchar("author", 512)
    val format = varchar("format", 16)
    val fileSize = long("file_size")
    val storageKey = varchar("storage_key", 1024)
    val status = varchar("status", 32)
    val metadataVersion = long("metadata_version")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object LibraryReadingProgress : Table("library_reading_progress") {
    val libraryBookId = reference("library_book_id", LibraryBooks, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val locatorJson = text("locator_json").nullable()
    val currentPageIndex = integer("current_page_index")
    val progressFraction = float("progress_fraction").nullable()
    val lastReadAtMs = long("last_read_at_ms")
    val version = long("version")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object LibraryChatMessages : UUIDTable("library_chat_messages") {
    val libraryBookId = reference("library_book_id", LibraryBooks, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val clientMessageId = uuid("client_message_id")
    val role = varchar("role", 16)
    val content = text("content")
    val createdAtMs = long("created_at_ms")
    val serverCreatedAt = timestampWithTimeZone("server_created_at")
}
