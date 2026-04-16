package com.example.aibookreader.domain.repository

import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.domain.model.PendingAiFailure
import kotlinx.coroutines.flow.Flow


interface AiRepository {
    fun getChatHistory(bookId: Int): Flow<List<ChatMessage>>

    fun observePendingFailure(bookId: Int): Flow<PendingAiFailure?>

    suspend fun savePendingFailure(
        bookId: Int,
        prompt: String,
        userMessage: String,
        errorMessage: String
    )

    suspend fun clearPendingFailure(bookId: Int)

    suspend fun sendAiRequest(
        bookId: Int,
        prompt: String,
        userMessage: String,
        appendUserMessageToHistory: Boolean = true
    ): Result<String>

    suspend fun clearChatHistory(bookId: Int)
}