package com.example.aibookreader.presentation.screens.reader

import retrofit2.HttpException

object AiErrorMessages {

    fun format(e: Throwable): String = when (e) {
        is HttpException -> httpMessage(e)
        else -> when {
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                "Нет подключения к интернету."
            e.message?.contains("timeout", ignoreCase = true) == true ->
                "Превышено время ожидания ответа."
            else -> e.message?.takeIf { it.isNotBlank() } ?: "Не удалось получить ответ."
        }
    }

    private fun httpMessage(e: HttpException): String = when (e.code()) {
        400 -> "Запрос не принят сервисом ИИ. Проверьте данные или попробуйте позже."
        401, 403 -> "Ошибка доступа к сервису ИИ (ключ API)."
        404 -> "Модель ИИ не найдена."
        429 -> "Слишком много запросов. Подождите немного."
        in 500..599 -> "Сервис ИИ временно недоступен."
        else -> "Ошибка сети (${e.code()})."
    }
}
