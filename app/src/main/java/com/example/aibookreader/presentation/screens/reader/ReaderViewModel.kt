package com.example.aibookreader.presentation.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.domain.usecase.ClearChatHistoryUseCase
import com.example.aibookreader.domain.usecase.GetBookByIdUseCase
import com.example.aibookreader.domain.usecase.GetChatHistoryUseCase
import com.example.aibookreader.domain.usecase.GetReaderPageUseCase
import com.example.aibookreader.domain.usecase.SendAiRequestUseCase
import com.example.aibookreader.domain.usecase.UpdateReadingProgressUseCase
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

    private val pageCache = mutableMapOf<Int, List<ReaderBlock>>()
    private val MAX_CACHE_SIZE = 10

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
        android.util.Log.d("ReaderVM", "loadPage: bookId=$bookId, page=$page")

        // Если есть в кэше — показываем немедленно (предзагрузка сработала)
        pageCache[page]?.let { cachedBlocks ->
            _uiState.update {
                it.copy(
                    blocks = cachedBlocks,
                    currentPage = page,
                    isLoading = false
                )
            }
        }

        // Грузим из Room в любом случае (обновляем кэш свежими данными)
        viewModelScope.launch {
            try {
                val blocks = getReaderPageUseCase(bookId = bookId, page = page)
                android.util.Log.d("ReaderVM", "Loaded ${blocks.size} blocks for page $page")

                // Сохраняем в кэш с ограничением размера
                addToCache(page, blocks)

                // Обновляем UI только если это та страница, которую сейчас смотрят
                // (при быстром свайпе пользователь мог уйти дальше)
                _uiState.update { state ->
                    if (state.currentPage == page || pageCache[state.currentPage] == null) {
                        state.copy(blocks = blocks, currentPage = page, isLoading = false)
                    } else state
                }

                updateReadingProgressUseCase(bookId, page)

                // Предзагружаем соседние страницы в фоне
                preloadNeighbors(bookId, page)

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    /**
     * Предзагрузка соседних страниц в кэш.
     * Запускается после успешной загрузки текущей страницы.
     * Не блокирует UI — работает в фоновых корутинах.
     */
    private fun preloadNeighbors(bookId: Int, page: Int) {
        val total = _uiState.value.totalPages
        val neighbors = listOf(page - 1, page + 1).filter { it in 0 until total }

        neighbors.forEach { neighborPage ->
            if (!pageCache.containsKey(neighborPage)) {
                viewModelScope.launch {
                    try {
                        val blocks = getReaderPageUseCase(bookId = bookId, page = neighborPage)
                        addToCache(neighborPage, blocks)
                        android.util.Log.d("ReaderVM", "Preloaded page $neighborPage")
                    } catch (_: Exception) {
                        // Тихая ошибка — предзагрузка не критична
                    }
                }
            }
        }
    }

    /**
     * Добавляет страницу в кэш с выталкиванием старых записей.
     * При достижении MAX_CACHE_SIZE удаляем самые дальние от текущей страницы.
     */
    private fun addToCache(page: Int, blocks: List<ReaderBlock>) {
        pageCache[page] = blocks
        if (pageCache.size > MAX_CACHE_SIZE) {
            val current = _uiState.value.currentPage
            // Удаляем страницу, наиболее удалённую от текущей
            val toRemove = pageCache.keys.maxByOrNull { kotlin.math.abs(it - current) }
            toRemove?.let { pageCache.remove(it) }
        }
    }

    fun setSelectedText(text: String) {
        _uiState.update {
            it.copy(
                selectedText = text,
                isSheetOpen = true,
                aiError = null,
                isActionMode = true
            )
        }
    }

    fun openAiFromBottomBar() {
        val currentMessages = _uiState.value.chatMessages
        val pageText = getCurrentPageText()

        _uiState.update {
            it.copy(
                selectedText = it.selectedText ?: pageText.ifBlank { null },
                isSheetOpen = true,
                isActionMode = currentMessages.isEmpty(),
                aiError = null
            )
        }
    }

    fun switchToChat() {
        _uiState.update {
            it.copy(
                isActionMode = false
            )
        }
    }
    fun switchToActions() {
        _uiState.update {
            it.copy(
                isActionMode = true
            )
        }
    }

    fun closeSheet() {
        _uiState.update { it.copy(isSheetOpen = false, selectionKey = it.selectionKey + 1) }
    }

    // ИИ-действия
    fun onAiActionClick(action: String) {
        val bookId = currentBookId ?: return
        val selectedText = _uiState.value.selectedText ?: return
        val pageContext = getCurrentPageText()


        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null, isActionMode = false) }

            val result = sendAiRequestUseCase(bookId, action, selectedText, pageContext)

            result.fold(
                onSuccess = { answer ->
                    _uiState.update { it.copy(isAiLoading = false, isActionMode = false) }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiError = formatError(exception)
                        )
                    }
                }
            )
        }
    }

    fun sendChatMessage(message: String) {
        val bookId = currentBookId ?: return
        val selectedText = _uiState.value.selectedText ?: ""
        val pageContext = getCurrentPageText()

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null, isActionMode = false) }

            val result = sendAiRequestUseCase(
                bookId = bookId,
                action = message,
                selectedText = selectedText,
                pageContext = pageContext
            )

            result.fold(
                onSuccess = { answer ->
                    _uiState.update { it.copy(isAiLoading = false) }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiError = formatError(exception)
                        )
                    }
                }
            )
        }
    }

    fun clearChatHistory() {
        val bookId = currentBookId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionMode = true) }
            clearChatHistoryUseCase(bookId)
        }
    }

    private fun getCurrentPageText(): String =
        _uiState.value.blocks.joinToString("\n") { block ->
            when (block) {
                is ReaderBlock.Title -> block.text
                is ReaderBlock.Paragraph -> block.text
                is ReaderBlock.Quote -> block.text
                is ReaderBlock.Image -> ""
            }
        }.trim()

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