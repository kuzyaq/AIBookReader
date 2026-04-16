package com.example.aibookreader.presentation.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.domain.repository.AiRepository
import com.example.aibookreader.domain.repository.BookRepository
import com.example.aibookreader.domain.usecase.ClearChatHistoryUseCase
import com.example.aibookreader.domain.usecase.GetChatHistoryUseCase
import com.example.aibookreader.domain.usecase.SendAiRequestUseCase
import com.example.aibookreader.presentation.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

sealed class NavCommand {
    data object Forward : NavCommand()
    data object Backward : NavCommand()
}

/** Состояние экрана: [Publication] + фабрика навигатора и [Locator] — ядро модели Readium. */
@OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
data class ReadiumUiState(
    val book: Book? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showControls: Boolean = true,
    val publication: Publication? = null,
    val navigatorFactory: EpubNavigatorFactory? = null,
    val initialLocator: Locator? = null,

    val currentPage: Int = 1,
    val totalPages: Int = 1,

    val selectedText: String? = null,
    val pageContext: String = "",
    val isSheetOpen: Boolean = false,
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val aiChatError: AiChatErrorUi? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isActionMode: Boolean = true,

    val isSettingsOpen: Boolean = false,
    val isDarkMode: Boolean = false,
    /** Настройки рендера EPUB (шрифт, цвета) — применяются через [EpubNavigatorFragment.submitPreferences]. */
    val epubPreferences: EpubPreferences = EpubPreferences(),
    val fontSizeMultiplier: Double = 1.0,
    val lineHeightMultiplier: Double = 1.2
)

@OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
@HiltViewModel
class ReadiumReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val assetRetriever: AssetRetriever,
    private val publicationOpener: PublicationOpener,
    private val sendAiRequestUseCase: SendAiRequestUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val aiRepository: AiRepository,
    private val themeManager: ThemeManager
) : ViewModel() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ReadiumUiState())
    val uiState: StateFlow<ReadiumUiState> = _uiState.asStateFlow()

    private val _navCommands = MutableSharedFlow<NavCommand>(extraBufferCapacity = 1)
    val navCommands: SharedFlow<NavCommand> = _navCommands.asSharedFlow()

    val bookId: Int = savedStateHandle.get<Int>("bookId") ?: -1

    /** Текущая позиция в книге (Readium Locator: href, progression, выделение и т.д.). */
    private var currentLocator: Locator? = null

    private val aiSendMutex = Mutex()

    /** Базовое число позиций Readium (~фрагменты текста); «страницы» в UI масштабируются от шрифта/интервала. */
    private var baseReadiumPositionCount: Int = 1

    init {
        themeManager.isDarkMode
            .onEach { dark ->
                _uiState.update { it.copy(isDarkMode = dark) }
                rebuildPreferences(isDark = dark)
            }
            .launchIn(viewModelScope)
    }

    fun openBook() {
        if (_uiState.value.publication != null) return
        if (bookId < 0) {
            _uiState.update { it.copy(error = "Неверный ID книги", isLoading = false) }
            return
        }

        combine(
            getChatHistoryUseCase(bookId),
            aiRepository.observePendingFailure(bookId)
        ) { msgs, pending ->
            val err = pending?.let { p ->
                AiChatErrorUi(
                    message = p.errorMessage,
                    retry = PendingAiRetry(prompt = p.prompt, userMessage = p.userMessage)
                )
            }
            _uiState.update { s -> s.copy(chatMessages = msgs, aiChatError = err) }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val book = bookRepository.getBookById(bookId)
            if (book == null) {
                _uiState.update { it.copy(error = "Книга не найдена", isLoading = false) }
                return@launch
            }

            val file = File(book.filePath)
            if (!file.exists()) {
                _uiState.update { it.copy(error = "Файл не найден: ${book.filePath}", isLoading = false) }
                return@launch
            }

            // 1) file → Url → Asset (абстракция источника для Readium)
            val url = file.toUrl()
            val asset = assetRetriever.retrieve(url)
                .getOrElse {
                    _uiState.update { s -> s.copy(error = "Ошибка чтения: $it", isLoading = false) }
                    return@launch
                }

            // 2) Asset → Publication: распакованная модель EPUB (спайн, ресурсы, метаданные)
            val publication = publicationOpener.open(asset, allowUserInteraction = false)
                .getOrElse {
                    _uiState.update { s -> s.copy(error = "Ошибка открытия EPUB: $it", isLoading = false) }
                    return@launch
                }

            // Сервис positions: дискретные «опорные» точки для прогресса (не экранные страницы)
            baseReadiumPositionCount = publication.positions().size.coerceAtLeast(1)

            // 3) Фабрика UI-навигатора (WebView под капотом) для этой публикации
            val factory = EpubNavigatorFactory(publication)
            // Восстановление чтения: Locator — стандартный JSON Readium (сохраняем в БД)
            val initialLocator = book.locator?.let {
                try { Locator.fromJSON(org.json.JSONObject(it)) } catch (_: Exception) { null }
            }

            _uiState.update {
                it.copy(
                    book = book,
                    publication = publication,
                    navigatorFactory = factory,
                    initialLocator = initialLocator,
                    isLoading = false
                )
            }
        }
    }

    /** Оценка «плотности» вёрстки: больше шрифт/интервал → условно больше «страниц» в UI. */
    private fun layoutPagesFactor(): Double {
        val s = _uiState.value
        val font = s.fontSizeMultiplier.coerceIn(0.5, 3.0)
        val line = (s.lineHeightMultiplier / 1.2).coerceIn(0.75, 2.5)
        return font * line
    }

    private fun effectiveTotalPages(): Int {
        val factor = layoutPagesFactor()
        return (baseReadiumPositionCount * factor).roundToInt().coerceAtLeast(1)
    }

    /** totalProgression (0..1) из Locator — единая метрика прогресса по всей книге. */
    private fun updatePageNumbers(locator: Locator) {
        val total = effectiveTotalPages()
        val progression = locator.locations.totalProgression ?: 0.0
        val current = if (total > 0) {
            (progression * total).roundToInt().coerceIn(1, total)
        } else 1
        _uiState.update { it.copy(currentPage = current, totalPages = total) }
    }

    fun onLocationChanged(locator: Locator) {
        currentLocator = locator
        updatePageNumbers(locator)
        extractPageContext(locator)
    }

    private var lastContextHref: Url? = null

    /** Текущий HTML-ресурс по href → plain text для контекста ИИ (вне Readium API). */
    private fun extractPageContext(locator: Locator) {
        if (locator.href == lastContextHref) return
        lastContextHref = locator.href
        val pub = _uiState.value.publication ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resource = pub.get(locator.href) ?: return@launch
                val bytes = resource.read().getOrNull() ?: return@launch
                val html = String(bytes, Charsets.UTF_8)
                val text = org.jsoup.Jsoup.parse(html).body().text()
                val trimmed = if (text.length > 3000) text.substring(0, 3000) else text
                _uiState.update { it.copy(pageContext = trimmed) }
            } catch (_: Exception) { }
        }
    }

    /** Сохраняем Locator как JSON — при следующем открытии передаём в EpubNavigatorFactory. */
    fun saveProgress() {
        val loc = currentLocator ?: return
        applicationScope.launch {
            bookRepository.saveLocator(bookId, loc.toJSON().toString())
        }
    }

    fun goForward() { _navCommands.tryEmit(NavCommand.Forward) }
    fun goBackward() { _navCommands.tryEmit(NavCommand.Backward) }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun toggleTheme() {
        viewModelScope.launch { themeManager.toggleDarkMode() }
    }

    fun setSelectedText(text: String) {
        viewModelScope.launch { aiRepository.clearPendingFailure(bookId) }
        _uiState.update {
            it.copy(
                selectedText = text,
                isSheetOpen = true,
                aiError = null,
                isActionMode = true
            )
        }
    }

    fun openAiSheet() {
        val msgs = _uiState.value.chatMessages
        _uiState.update {
            it.copy(
                isSheetOpen = true,
                isActionMode = msgs.isEmpty(),
                aiError = null
            )
        }
    }

    fun switchToChat() { _uiState.update { it.copy(isActionMode = false) } }
    fun switchToActions() { _uiState.update { it.copy(isActionMode = true) } }
    fun closeSheet() {
        _uiState.update { it.copy(isSheetOpen = false) }
    }

    fun onAiActionClick(action: String) {
        val sel = _uiState.value.selectedText ?: return
        val context = _uiState.value.pageContext
        val (userMessage, prompt) = sendAiRequestUseCase.buildPrompt(action, sel, context)
        viewModelScope.launch {
            if (!aiSendMutex.tryLock()) return@launch
            try {
                _uiState.update {
                    it.copy(
                        isAiLoading = true,
                        aiError = null,
                        isActionMode = false
                    )
                }
                sendAiRequestUseCase.execute(bookId, prompt, userMessage, true).fold(
                    onSuccess = { _uiState.update { s -> s.copy(isAiLoading = false) } },
                    onFailure = { e ->
                        aiRepository.savePendingFailure(
                            bookId,
                            prompt,
                            userMessage,
                            AiErrorMessages.format(e)
                        )
                        _uiState.update { s -> s.copy(isAiLoading = false) }
                    }
                )
            } finally {
                aiSendMutex.unlock()
            }
        }
    }

    fun sendChatMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        val sel = _uiState.value.selectedText ?: ""
        val context = _uiState.value.pageContext
        val (userMessage, prompt) = sendAiRequestUseCase.buildPrompt(trimmed, sel, context)
        viewModelScope.launch {
            if (!aiSendMutex.tryLock()) return@launch
            try {
                _uiState.update {
                    it.copy(
                        isAiLoading = true,
                        aiError = null,
                        isActionMode = false
                    )
                }
                sendAiRequestUseCase.execute(bookId, prompt, userMessage, true).fold(
                    onSuccess = { _uiState.update { s -> s.copy(isAiLoading = false) } },
                    onFailure = { e ->
                        aiRepository.savePendingFailure(
                            bookId,
                            prompt,
                            userMessage,
                            AiErrorMessages.format(e)
                        )
                        _uiState.update { s -> s.copy(isAiLoading = false) }
                    }
                )
            } finally {
                aiSendMutex.unlock()
            }
        }
    }

    fun retryLastAiRequest() {
        val retry = _uiState.value.aiChatError?.retry ?: return
        viewModelScope.launch {
            if (!aiSendMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isAiLoading = true, aiError = null) }
                sendAiRequestUseCase.execute(bookId, retry.prompt, retry.userMessage, false).fold(
                    onSuccess = { _uiState.update { s -> s.copy(isAiLoading = false) } },
                    onFailure = { e ->
                        aiRepository.savePendingFailure(
                            bookId,
                            retry.prompt,
                            retry.userMessage,
                            AiErrorMessages.format(e)
                        )
                        _uiState.update { s -> s.copy(isAiLoading = false) }
                    }
                )
            } finally {
                aiSendMutex.unlock()
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionMode = true) }
            clearChatHistoryUseCase(bookId)
        }
    }

    fun openSettings() { _uiState.update { it.copy(isSettingsOpen = true) } }
    fun closeSettings() { _uiState.update { it.copy(isSettingsOpen = false) } }

    fun setFontSize(multiplier: Double) {
        _uiState.update { it.copy(fontSizeMultiplier = multiplier) }
        rebuildPreferences()
        currentLocator?.let { updatePageNumbers(it) }
    }

    fun setLineHeight(multiplier: Double) {
        _uiState.update { it.copy(lineHeightMultiplier = multiplier) }
        rebuildPreferences()
        currentLocator?.let { updatePageNumbers(it) }
    }

    /** EpubPreferences → CSS/инъекции в WebView навигатора (тема, типографика). */
    private fun rebuildPreferences(isDark: Boolean = _uiState.value.isDarkMode) {
        val s = _uiState.value
        val bgColor: ReadiumColor
        val txtColor: ReadiumColor
        if (isDark) {
            bgColor = ReadiumColor(0xFF1A1825.toInt())   // ReaderBgDark
            txtColor = ReadiumColor(0xFFE6E1F0.toInt())  // ReaderTextDark
        } else {
            bgColor = ReadiumColor(0xFFFAF8F3.toInt())   // ReaderBgLight
            txtColor = ReadiumColor(0xFF2B2830.toInt())   // ReaderTextLight
        }
        val prefs = EpubPreferences(
            fontSize = s.fontSizeMultiplier,
            lineHeight = s.lineHeightMultiplier,
            publisherStyles = false,
            backgroundColor = bgColor,
            textColor = txtColor
        )
        _uiState.update { it.copy(epubPreferences = prefs) }
    }
}
