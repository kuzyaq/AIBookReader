package com.example.aibookreader.server.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class InitLibraryBookRequest(
    val title: String,
    val author: String,
    val format: String,
    val fileSize: Long,
    val originalFileName: String? = null
)

@Serializable
data class InitLibraryBookResponse(
    val id: String,
    val uploadUrl: String,
    val storageKey: String,
    val expiresInSeconds: Long,
    val contentType: String
)

@Serializable
data class LibraryBookResponse(
    val id: String,
    val title: String,
    val author: String,
    val format: String,
    val fileSize: Long,
    val status: String,
    val metadataVersion: Long
)

@Serializable
data class ProgressResponse(
    val locatorJson: String? = null,
    val currentPageIndex: Int,
    val progressFraction: Float? = null,
    val lastReadAtMs: Long,
    val version: Long
)

@Serializable
data class ProgressPutRequest(
    val locatorJson: String? = null,
    val currentPageIndex: Int,
    val progressFraction: Float? = null,
    val lastReadAtMs: Long,
    val expectedVersion: Long? = null
)

@Serializable
data class DownloadResponse(
    val url: String,
    val expiresInSeconds: Long
)

@Serializable
data class ChatMessageDto(
    val clientMessageId: String,
    val role: String,
    val content: String,
    val createdAtMs: Long
)

@Serializable
data class ChatBatchRequest(
    val messages: List<ChatMessageDto>
)

@Serializable
data class ChatMessageResponse(
    val id: String,
    val clientMessageId: String,
    val role: String,
    val content: String,
    val createdAtMs: Long,
    val serverCreatedAt: String
)
