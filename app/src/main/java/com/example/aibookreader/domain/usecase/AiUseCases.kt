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
    suspend operator fun invoke(bookId: Int, action: String, selectedText: String, pageContext: String = ""): Result<String> {
        val (userMessage, prompt) = buildPrompt(action, selectedText, pageContext)
        return repository.sendAiRequest(bookId, prompt, userMessage)
    }

    private fun buildPrompt(action: String, selectedText: String, pageContext: String): Pair<String, String> {

        val contextSection = if (pageContext.isNotBlank() && pageContext != selectedText) {
            "Контекст (текущая страница книги):\n«$pageContext»\n\n"
        } else ""

        val selectedSection = "Выделенный фрагмент для анализа:\n«$selectedText»\n\n"

        return when (action) {
            "explain" -> Pair(
                "Объясни выделенный текст: \n«$selectedText»",
                "${contextSection}${selectedSection}" +
                        "Задание: Объясни простыми словами выделенный фрагмент. " +
                        "Используй контекст страницы чтобы дать точное и полное объяснение. " +
                        "Отвечай на русском языке. Убери форматирование текста."
            )
            "summary" -> Pair(
                "Сделай пересказ выделенного текста",
                "${contextSection}${selectedSection}" +
                        "Задание: Сделай краткий пересказ выделенного фрагмента. " +
                        "Учти что происходило на странице для полного понимания. " +
                        "Отвечай на русском языке. Убери форматирование текста."
            )
            "quiz" -> Pair(
                "Создай тест",
                "${contextSection}${selectedSection}" +
                        "Задание: Создай 3 вопроса с вариантами ответов для проверки " +
                        "понимания выделенного фрагмента. Вопросы должны учитывать " +
                        "контекст страницы. Отвечай на русском языке. Убери форматирование текста."
            )
            "translate" -> Pair(
                "Переведи текст: \n«$selectedText»",
                "${contextSection}${selectedSection}"+
                        "Задание: Переведи выделенный текст на русский язык. " +
                        "Учитывай контекст страницы для более точного перевода. Убери форматирование текста."
            )
            else -> Pair(
                // Произвольный вопрос из поля ввода
                action,
                // Для свободного вопроса контекст особенно важен:
                // пользователь может спросить «а кто этот человек?»
                // и без контекста ИИ не поймёт о ком речь
                "${contextSection}${selectedSection}" +
                        "Вопрос пользователя: $action" +
                        "Учитывай контекст страницы. Убери форматирование текста."
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
