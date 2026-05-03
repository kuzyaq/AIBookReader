package com.example.aibookreader.data.repository

import com.example.aibookreader.data.local.dao.ChatHistoryDao
import com.example.aibookreader.data.local.entity.ChatMessageEntity
import com.example.aibookreader.data.local.entity.PendingAiRetryEntity
import com.example.aibookreader.data.local.mapper.ChatMapper
import com.example.aibookreader.data.remote.gemini.GeminiApiService
import com.example.aibookreader.data.remote.gemini.buildRequest
import com.example.aibookreader.data.remote.gemini.extractText
import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.domain.model.PendingAiFailure
import com.example.aibookreader.data.sync.LibrarySyncEnqueuer
import com.example.aibookreader.domain.repository.AiRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val chatHistoryDao: ChatHistoryDao,
    private val geminiApiService: GeminiApiService,
    private val librarySyncEnqueuer: LibrarySyncEnqueuer
) : AiRepository {

    override fun getChatHistory(bookId: Int): Flow<List<ChatMessage>> {
        return chatHistoryDao.getChatHistory(bookId).map { entities ->
            ChatMapper.toDomainList(entities)
        }
    }

    override fun observePendingFailure(bookId: Int): Flow<PendingAiFailure?> =
        chatHistoryDao.observePendingRetry(bookId).map { row ->
            row?.let {
                PendingAiFailure(
                    prompt = it.prompt,
                    userMessage = it.userMessage,
                    errorMessage = it.errorMessage
                )
            }
        }

    override suspend fun savePendingFailure(
        bookId: Int,
        prompt: String,
        userMessage: String,
        errorMessage: String
    ) {
        chatHistoryDao.upsertPendingRetry(
            PendingAiRetryEntity(
                bookId = bookId,
                prompt = prompt,
                userMessage = userMessage,
                errorMessage = errorMessage
            )
        )
    }

    override suspend fun clearPendingFailure(bookId: Int) {
        chatHistoryDao.clearPendingRetry(bookId)
    }

    override suspend fun sendAiRequest(
        bookId: Int,
        prompt: String,
        userMessage: String,
        appendUserMessageToHistory: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            chatHistoryDao.clearPendingRetry(bookId)

            if (appendUserMessageToHistory) {
                chatHistoryDao.insertMessage(
                    ChatMessageEntity(
                        bookId = bookId,
                        message = userMessage,
                        isUser = true,
                        timestamp = System.currentTimeMillis(),
                        clientUuid = UUID.randomUUID().toString(),
                        synced = 0
                    )
                )
            }

            val response = try {
                geminiApiService.generateContent(request = buildRequest(prompt))
            } catch (e: HttpException) {
                throw mapHttpException(e)
            }

            val aiText = response.extractText()
                ?: throw IllegalStateException("ИИ вернул пустой ответ.")

            chatHistoryDao.insertMessage(
                ChatMessageEntity(
                    bookId = bookId,
                    message = aiText,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    clientUuid = UUID.randomUUID().toString(),
                    synced = 0
                )
            )

            librarySyncEnqueuer.enqueueChatPush(bookId)

            aiText
        }
    }

    private fun mapHttpException(e: HttpException): Exception {
        val text = when (e.code()) {
            400 -> "Запрос не принят сервисом ИИ. Проверьте данные или попробуйте позже."
            401, 403 -> "Ошибка доступа к сервису ИИ (ключ API)."
            404 -> "Модель ИИ не найдена."
            429 -> "Слишком много запросов. Подождите немного."
            in 500..599 -> "Сервис ИИ временно недоступен."
            else -> "Ошибка сети (${e.code()})."
        }
        return Exception(text)
    }

    override suspend fun clearChatHistory(bookId: Int) {
        chatHistoryDao.clearChatHistory(bookId)
        chatHistoryDao.clearPendingRetry(bookId)
    }
}