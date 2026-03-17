package com.example.aibookreader.domain.repository

import com.example.aibookreader.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow


interface AiRepository {
    fun getChatHistory(bookId: Int): Flow<List<ChatMessage>>

    suspend fun sendAiRequest(bookId: Int, prompt: String, userMessage: String): Result<String>

    suspend fun clearChatHistory(bookId: Int)
}