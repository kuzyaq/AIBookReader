package com.example.aibookreader.data.remote.library

import com.google.gson.annotations.SerializedName

data class InitLibraryBookRequestDto(
    val title: String,
    val author: String,
    val format: String,
    val fileSize: Long,
    val originalFileName: String? = null
)

data class InitLibraryBookResponseDto(
    val id: String,
    val uploadUrl: String,
    val storageKey: String,
    val expiresInSeconds: Long,
    val contentType: String
)

data class LibraryBookDto(
    val id: String,
    val title: String,
    val author: String,
    val format: String,
    val fileSize: Long,
    val status: String,
    val metadataVersion: Long
)

data class DownloadResponseDto(
    val url: String,
    val expiresInSeconds: Long
)

data class ProgressResponseDto(
    val locatorJson: String? = null,
    val currentPageIndex: Int,
    val progressFraction: Float? = null,
    val lastReadAtMs: Long,
    val version: Long
)

data class ProgressPutDto(
    val locatorJson: String? = null,
    val currentPageIndex: Int,
    val progressFraction: Float? = null,
    val lastReadAtMs: Long,
    val expectedVersion: Long? = null
)

data class ChatMessageItemDto(
    val clientMessageId: String,
    val role: String,
    val content: String,
    val createdAtMs: Long
)

data class ChatBatchRequestDto(
    val messages: List<ChatMessageItemDto>
)

data class ChatMessageResponseDto(
    val id: String,
    val clientMessageId: String,
    val role: String,
    val content: String,
    val createdAtMs: Long,
    val serverCreatedAt: String
)
