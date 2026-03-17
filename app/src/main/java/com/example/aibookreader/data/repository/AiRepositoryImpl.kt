package com.example.aibookreader.data.repository

import com.example.aibookreader.data.local.dao.ChatHistoryDao
import com.example.aibookreader.data.local.entity.ChatMessageEntity
import com.example.aibookreader.data.local.mapper.ChatMapper
import com.example.aibookreader.data.remote.gemini.GeminiApiService
import com.example.aibookreader.data.remote.gemini.buildRequest
import com.example.aibookreader.data.remote.gemini.extractText
import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val chatHistoryDao: ChatHistoryDao,
    private val geminiApiService: GeminiApiService
) : AiRepository {

    override fun getChatHistory(bookId: Int): Flow<List<ChatMessage>> {
        return chatHistoryDao.getChatHistory(bookId).map { entities ->
            ChatMapper.toDomainList(entities)
        }
    }

    override suspend fun sendAiRequest(
        bookId: Int,
        prompt: String,
        userMessage: String
    ): Result<String> = runCatching {

        chatHistoryDao.insertMessage(
            ChatMessageEntity(
                bookId = bookId,
                message = userMessage,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
        )

        val response = geminiApiService.generateContent(
            request = buildRequest(prompt)
        )

        val aiText = response.extractText()
            ?: throw IllegalStateException("Gemini вернул пустой ответ")

        chatHistoryDao.insertMessage(
            ChatMessageEntity(
                bookId = bookId,
                message = aiText,
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
        )

        aiText
    }

    override suspend fun clearChatHistory(bookId: Int) {
        chatHistoryDao.clearChatHistory(bookId)
    }
}