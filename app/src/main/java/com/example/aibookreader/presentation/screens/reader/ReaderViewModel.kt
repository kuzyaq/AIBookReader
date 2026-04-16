package com.example.aibookreader.presentation.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.domain.model.ReaderSettings
import com.example.aibookreader.domain.repository.ReaderRepository
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val getReaderPageUseCase: GetReaderPageUseCase,
    private val updateReadingProgressUseCase: UpdateReadingProgressUseCase,
    private val getBookByIdUseCase: GetBookByIdUseCase,
    private val sendAiRequestUseCase: SendAiRequestUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val readerRepository: ReaderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentBookId: Int? = null
    private val pageCache = mutableMapOf<Int, List<ReaderBlock>>()
    private val MAX_CACHE_SIZE = 10

    fun openBook(bookId: Int) {
        if (currentBookId == bookId) return
        currentBookId = bookId

        getChatHistoryUseCase(bookId)
            .onEach { msgs -> _uiState.update { it.copy(chatMessages = msgs) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val book = getBookByIdUseCase(bookId) ?: return@launch

            val hasDir = !book.extractedDir.isNullOrBlank() && File(book.extractedDir).exists()
            if (hasDir) {
                val chapters = readerRepository.getChapters(bookId)
                _uiState.update {
                    it.copy(
                        title = book.title,
                        totalPages = chapters.size,
                        currentPage = book.currentPage,
                        useWebView = true,
                        chapters = chapters,
                        basePath = book.opfBasePath
                    )
                }
                loadChapter(book.currentPage.coerceIn(0, chapters.size - 1))
            } else {
                _uiState.update {
                    it.copy(
                        title = book.title, totalPages = book.totalPages,
                        currentPage = book.currentPage, useWebView = false
                    )
                }
                loadPage(book.currentPage)
            }
        }
    }

    fun loadChapter(chapterIndex: Int, startFromEnd: Boolean = false) {
        val bookId = currentBookId ?: return
        val state = _uiState.value
        if (chapterIndex !in state.chapters.indices) return
        val chapter = state.chapters[chapterIndex]
        val base = state.basePath ?: return
        val file = File(base, chapter.href)

        _uiState.update {
            it.copy(
                currentChapterIndex = chapterIndex,
                currentPage = chapterIndex,
                currentPageInChapter = if (startFromEnd) -1 else 0,
                pagesInCurrentChapter = it.chapterPageCounts[chapterIndex] ?: 1,
                chapterFilePath = file.absolutePath,
                isLoading = false,
                selectionKey = it.selectionKey + 1
            )
        }
        viewModelScope.launch { updateReadingProgressUseCase(bookId, chapterIndex) }
    }

    fun nextChapter() {
        val s = _uiState.value
        if (s.currentChapterIndex < s.chapters.size - 1) loadChapter(s.currentChapterIndex + 1)
    }

    fun previousChapter() {
        val s = _uiState.value
        if (s.currentChapterIndex > 0) loadChapter(s.currentChapterIndex - 1, startFromEnd = true)
    }

    fun onPagesInChapterCalculated(total: Int) {
        _uiState.update {
            val page = if (it.currentPageInChapter < 0) total - 1
                       else it.currentPageInChapter.coerceIn(0, total - 1)
            val counts = it.chapterPageCounts.toMutableMap()
            counts[it.currentChapterIndex] = total
            it.copy(pagesInCurrentChapter = total, currentPageInChapter = page, chapterPageCounts = counts)
        }
    }

    fun onPageInChapterChanged(page: Int) {
        _uiState.update {
            if (it.currentPageInChapter == page) it
            else it.copy(currentPageInChapter = page)
        }
    }

    fun nextPage() {
        val s = _uiState.value
        if (s.useWebView) {
            if (s.currentPageInChapter < s.pagesInCurrentChapter - 1)
                _uiState.update { it.copy(currentPageInChapter = s.currentPageInChapter + 1) }
            else nextChapter()
        } else {
            if (s.currentPage < s.totalPages - 1) loadPage(s.currentPage + 1)
        }
    }

    fun previousPage() {
        val s = _uiState.value
        if (s.useWebView) {
            if (s.currentPageInChapter > 0)
                _uiState.update { it.copy(currentPageInChapter = s.currentPageInChapter - 1) }
            else previousChapter()
        } else {
            if (s.currentPage > 0) loadPage(s.currentPage - 1)
        }
    }

    fun loadPage(page: Int) {
        val bookId = currentBookId ?: return
        pageCache[page]?.let { cached ->
            _uiState.update { it.copy(blocks = cached, currentPage = page, isLoading = false) }
        }
        viewModelScope.launch {
            try {
                val blocks = getReaderPageUseCase(bookId = bookId, page = page)
                addToCache(page, blocks)
                _uiState.update { st ->
                    if (st.currentPage == page || pageCache[st.currentPage] == null)
                        st.copy(blocks = blocks, currentPage = page, isLoading = false)
                    else st
                }
                updateReadingProgressUseCase(bookId, page)
                preloadNeighbors(bookId, page)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun preloadNeighbors(bookId: Int, page: Int) {
        val total = _uiState.value.totalPages
        listOf(page - 1, page + 1).filter { it in 0 until total }.forEach { np ->
            if (!pageCache.containsKey(np)) {
                viewModelScope.launch {
                    try { addToCache(np, getReaderPageUseCase(bookId, np)) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun addToCache(page: Int, blocks: List<ReaderBlock>) {
        pageCache[page] = blocks
        if (pageCache.size > MAX_CACHE_SIZE) {
            val cur = _uiState.value.currentPage
            pageCache.keys.maxByOrNull { kotlin.math.abs(it - cur) }?.let { pageCache.remove(it) }
        }
    }

    fun openTextSettings() { _uiState.update { it.copy(isTextSettingsOpen = true) } }
    fun closeTextSettings() { _uiState.update { it.copy(isTextSettingsOpen = false) } }
    fun updateReaderSettings(s: ReaderSettings) { _uiState.update { it.copy(readerSettings = s) } }
    fun resetReaderSettings() { _uiState.update { it.copy(readerSettings = ReaderSettings()) } }

    fun setSelectedText(text: String) {
        _uiState.update { it.copy(selectedText = text, isSheetOpen = true, aiError = null, isActionMode = true) }
    }

    fun openAiFromBottomBar() {
        val msgs = _uiState.value.chatMessages
        val pageText = getCurrentPageText()
        _uiState.update {
            it.copy(
                selectedText = it.selectedText ?: pageText.ifBlank { null },
                isSheetOpen = true, isActionMode = msgs.isEmpty(), aiError = null
            )
        }
    }

    fun switchToChat() { _uiState.update { it.copy(isActionMode = false) } }
    fun switchToActions() { _uiState.update { it.copy(isActionMode = true) } }
    fun closeSheet() { _uiState.update { it.copy(isSheetOpen = false, selectionKey = it.selectionKey + 1) } }

    fun onAiActionClick(action: String) {
        val bookId = currentBookId ?: return
        val sel = _uiState.value.selectedText ?: return
        val ctx = getCurrentPageText()
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null, isActionMode = false) }
            sendAiRequestUseCase(bookId, action, sel, ctx).fold(
                onSuccess = { _uiState.update { it.copy(isAiLoading = false, isActionMode = false) } },
                onFailure = { e -> _uiState.update { it.copy(isAiLoading = false, aiError = formatError(e)) } }
            )
        }
    }

    fun sendChatMessage(message: String) {
        val bookId = currentBookId ?: return
        val sel = _uiState.value.selectedText ?: ""
        val ctx = getCurrentPageText()
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null, isActionMode = false) }
            sendAiRequestUseCase(bookId, message, sel, ctx).fold(
                onSuccess = { _uiState.update { it.copy(isAiLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(isAiLoading = false, aiError = formatError(e)) } }
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

    private fun getCurrentPageText(): String {
        val s = _uiState.value
        if (s.useWebView) {
            val path = s.chapterFilePath ?: return ""
            return try {
                val f = File(path)
                if (f.exists()) org.jsoup.Jsoup.parse(f, "UTF-8").body().text() else ""
            } catch (_: Exception) { "" }
        }
        return s.blocks.joinToString("\n") {
            when (it) {
                is ReaderBlock.Title -> it.text
                is ReaderBlock.Paragraph -> it.text
                is ReaderBlock.Quote -> it.text
                is ReaderBlock.Image -> ""
            }
        }.trim()
    }

    private fun formatError(e: Throwable): String = when {
        e.message?.contains("Unable to resolve host") == true -> "Нет подключения к интернету."
        e.message?.contains("401") == true -> "Неверный API-ключ."
        e.message?.contains("429") == true -> "Превышен лимит запросов."
        e.message?.contains("404") == true -> "Модель ИИ не найдена."
        else -> "Ошибка: ${e.localizedMessage}"
    }
}
