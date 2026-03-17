package com.example.aibookreader.presentation.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aibookreader.domain.usecase.GetBookByIdUseCase
import com.example.aibookreader.domain.usecase.GetReaderPageUseCase
import com.example.aibookreader.domain.usecase.UpdateReadingProgressUseCase
import com.example.aibookreader.data.remote.gemini.GeminiClient
import com.example.aibookreader.data.remote.gemini.buildRequest
import com.example.aibookreader.data.remote.gemini.extractText
import com.example.aibookreader.domain.usecase.ClearChatHistoryUseCase
import com.example.aibookreader.domain.usecase.GetChatHistoryUseCase
import com.example.aibookreader.domain.usecase.SendAiRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(

    private val getReaderPageUseCase: GetReaderPageUseCase,
    private val updateReadingProgressUseCase: UpdateReadingProgressUseCase,
    private val getBookByIdUseCase: GetBookByIdUseCase,
    private val sendAiRequestUseCase: SendAiRequestUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase

) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentBookId: Int? = null

    fun openBook(
        bookId: Int
    ) {
        if (currentBookId == bookId) return

        currentBookId = bookId

        getChatHistoryUseCase(bookId)
            .onEach { messages ->
                _uiState.update { state ->
                    state.copy(chatMessages = messages)
                }
            }
            .launchIn(viewModelScope)


        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val book = getBookByIdUseCase(bookId)
            book?.let {
                _uiState.update { state ->
                    state.copy(
                        title = it.title,
                        totalPages = it.totalPages,
                        currentPage = it.currentPage
                    )
                }
                loadPage(it.currentPage)
            }
        }
    }

    fun nextPage() {
        if (_uiState.value.currentPage < _uiState.value.totalPages - 1) {
            loadPage(_uiState.value.currentPage + 1)
        }
    }

    fun previousPage() {
        if (_uiState.value.currentPage > 0) {
            loadPage(_uiState.value.currentPage - 1)
        }
    }

    fun loadPage(page: Int) {
        val bookId = currentBookId ?: return
        android.util.Log.d("ReaderVM", "Запрос в БД: bookId=$bookId, page=$page")

        viewModelScope.launch {
            try {
                val blocks = getReaderPageUseCase(
                    bookId = bookId,
                    page = page
                )
                android.util.Log.d("ReaderVM", "Loaded ${blocks.size} blocks for page $page")

                _uiState.update {
                    it.copy(
                        blocks = blocks,
                        currentPage = page,
                        isLoading = false
                    )
                }

                updateReadingProgressUseCase(bookId, page)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun setSelectedText(text: String) {
        _uiState.update {
            it.copy(
                selectedText = text,
                isSheetOpen = true,
                aiError = null
            )
        }
    }

    fun closeSheet() {
        _uiState.update { it.copy(isSheetOpen = false) }
    }

    // ИИ-действия
    fun onAiActionClick(action: String) {
        val bookId = currentBookId ?: return
        val selectedText = _uiState.value.selectedText ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null) }

            val result = sendAiRequestUseCase(bookId, action, selectedText)

            result.fold(
                onSuccess = { answer ->
                    _uiState.update { it.copy(isAiLoading = false) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(
                        isAiLoading = false,
                        aiError = formatError(exception)
                    ) }
                }
            )
        }
    }

    fun sendChatMessage(message: String) {
        val bookId = currentBookId ?: return
        val selectedText = _uiState.value.selectedText ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null) }

            val result = sendAiRequestUseCase(
                bookId = bookId,
                action = message,
                selectedText = selectedText
            )

            result.fold(
                onSuccess = { answer ->
                    _uiState.update { it.copy(isAiLoading = false) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(
                        isAiLoading = false,
                        aiError = formatError(exception)
                    ) }
                }
            )
        }
    }

    fun clearChatHistory(){
        val bookId = currentBookId ?: return
        viewModelScope.launch {
            clearChatHistoryUseCase(bookId)
        }
    }

    private fun formatError(e: Throwable): String = when {
        e.message?.contains("Unable to resolve host") == true ->
            "Нет подключения к интернету."
        e.message?.contains("401") == true ->
            "Неверный API-ключ. Проверь GEMINI_API_KEY в local.properties."
        e.message?.contains("429") == true ->
            "Превышен лимит запросов. Подожди минуту и попробуй снова."
        e.message?.contains("404") == true ->
            "Модель ИИ не найдена. Проверь название модели в GeminiApi.kt."
        else -> "Ошибка: ${e.localizedMessage}"
    }
}

sealed class AiAction(val prompt: String) {
    object Explain : AiAction("Объясни простыми словами этот термин: ")
    object Summary : AiAction("Сделай краткий пересказ этого фрагмента: ")
    object Quiz : AiAction("Создай 3 вопроса для проверки понимания этого текста: ")
}