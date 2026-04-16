package com.example.aibookreader.presentation.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aibookreader.presentation.screens.reader.components.AiAssistantSheetContent
import com.example.aibookreader.presentation.screens.reader.components.EpubPage
import com.example.aibookreader.presentation.screens.reader.components.EpubWebViewPage
import com.example.aibookreader.presentation.screens.reader.components.ReaderBottomBar
import com.example.aibookreader.presentation.screens.reader.components.ReaderTopAppBar
import com.example.aibookreader.presentation.screens.reader.components.TextSettingsSheetContent
import com.example.aibookreader.presentation.theme.AIBookReaderTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
    bookId: Int,
) {
    val state by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }

    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textSettingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(Unit) { viewModel.openBook(bookId = bookId) }

    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.totalPages }
    )

    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && !initialScrollDone) {
            if (pagerState.currentPage != state.currentPage)
                pagerState.scrollToPage(state.currentPage)
            initialScrollDone = true
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                if (initialScrollDone && page != state.currentPage)
                    viewModel.loadPage(page)
            }
    }

    val pageLabel: String
    val hasPrevious: Boolean
    val hasNext: Boolean
    if (state.useWebView) {
        val pg = state.currentPageInChapter.coerceAtLeast(0) + 1
        pageLabel = "Гл. ${state.currentChapterIndex + 1}/${state.chapters.size} • $pg/${state.pagesInCurrentChapter}"
        hasPrevious = state.currentChapterIndex > 0 || state.currentPageInChapter > 0
        hasNext = state.currentChapterIndex < state.chapters.size - 1 ||
                state.currentPageInChapter < state.pagesInCurrentChapter - 1
    } else {
        pageLabel = "${state.currentPage + 1} / ${state.totalPages}"
        hasPrevious = state.currentPage > 0
        hasNext = state.currentPage < state.totalPages - 1
    }

    AIBookReaderTheme(darkTheme = darkMode, isReaderMode = true) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                    ReaderTopAppBar(
                        onNavigateBack = onNavigateBack,
                        onToggleTheme = { darkMode = !darkMode },
                        bookTitle = state.title ?: "Без названия"
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                    ReaderBottomBar(
                        pageLabel = pageLabel,
                        hasPrevious = hasPrevious,
                        hasNext = hasNext,
                        onPreviousPage = { viewModel.previousPage() },
                        onNextPage = { viewModel.nextPage() },
                        onAiClick = { viewModel.openAiFromBottomBar() },
                        onTextSettingsClick = { viewModel.openTextSettings() }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {

                val chapterFile = state.chapterFilePath
                if (state.useWebView && !state.isLoading && chapterFile != null) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        EpubWebViewPage(
                            chapterFilePath = chapterFile,
                            basePath = state.basePath ?: "",
                            backgroundColor = MaterialTheme.colorScheme.background,
                            textColor = MaterialTheme.colorScheme.onBackground,
                            readerSettings = state.readerSettings,
                            currentPageInChapter = state.currentPageInChapter,
                            onTotalPagesCalculated = { viewModel.onPagesInChapterCalculated(it) },
                            onPageChanged = { viewModel.onPageInChapterChanged(it) },
                            onAiRequested = { viewModel.setSelectedText(it) },
                            onTap = { showControls = !showControls },
                            onSwipeLeft = { viewModel.nextChapter() },
                            onSwipeRight = { viewModel.previousChapter() }
                        )
                    }
                } else if (!state.useWebView) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize().padding(padding),
                        userScrollEnabled = !state.isSheetOpen && !state.isTextSettingsOpen
                    ) { _ ->
                        if (!state.isLoading) {
                            EpubPage(
                                blocks = state.blocks,
                                settings = state.readerSettings,
                                onTap = { showControls = !showControls },
                                onAiSelected = { viewModel.setSelectedText(it) },
                                selectionKey = state.selectionKey
                            )
                        } else {
                            Box(Modifier.fillMaxSize()) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                    }
                } else if (state.isLoading) {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }

                if (state.isSheetOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.closeSheet() },
                        sheetState = aiSheetState
                    ) {
                        AiAssistantSheetContent(
                            uiState = state,
                            onActionClick = { viewModel.onAiActionClick(it) },
                            onSendMessage = { viewModel.sendChatMessage(it) },
                            onClearHistory = { viewModel.clearChatHistory() },
                            onSwitchToChat = { viewModel.switchToChat() },
                            onSwitchToActions = { viewModel.switchToActions() }
                        )
                    }
                }

                if (state.isTextSettingsOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.closeTextSettings() },
                        sheetState = textSettingsSheetState
                    ) {
                        TextSettingsSheetContent(
                            settings = state.readerSettings,
                            onSettingsChange = { viewModel.updateReaderSettings(it) },
                            onReset = { viewModel.resetReaderSettings() }
                        )
                    }
                }
            }
        }
    }
}
