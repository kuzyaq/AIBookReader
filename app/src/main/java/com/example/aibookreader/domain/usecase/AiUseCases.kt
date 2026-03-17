package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject



class GetChatHistoryUseCase @Inject constructor(
    private val repository: AiRepository
) {
    operator fun invoke(bookId: Int): Flow<List<ChatMessage>> =
        repository.getChatHistory(bookId)
}


class SendAiRequestUseCase @Inject constructor(
    private val repository: AiRepository
){
    suspend operator fun invoke(bookId: Int, action: String, selectedText: String): Result<String> {
        val (message, prompt) = buildPrompt(action, selectedText)
        return repository.sendAiRequest(bookId, prompt, message)
    }

    private fun buildPrompt(action: String, text: String): Pair<String, String> {
        return when (action) {
            "explain" -> Pair(
                "Объясни выделенный текст",
                "Объясни простыми словами этот фрагмент из книги. Отвечай на русском языке:\n\n$text"
            )
            "summary" -> Pair(
                "Сделай пересказ",
                "Сделай краткий пересказ этого фрагмента. Отвечай на русском языке:\n\n$text"
            )
            "quiz" -> Pair(
                "Создай тест",
                "Создай 3 вопроса с вариантами ответов для проверки понимания текста. Отвечай на русском языке:\n\n$text"
            )
            "translate" -> Pair(
                "Переведи текст",
                "Переведи этот текст на русский язык:\n\n$text"
            )
            else -> Pair(
                action,
                // Произвольный вопрос пользователя — добавляем контекст книги
                "Контекст из книги:\n\"$text\"\n\nВопрос: $action"
            )
        }
    }
}

class ClearChatHistoryUseCase @Inject constructor(
    private val repository: AiRepository
) {
    suspend operator fun invoke(bookId: Int) {
        repository.clearChatHistory(bookId)
    }
}
