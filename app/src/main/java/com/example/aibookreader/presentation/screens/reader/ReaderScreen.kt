package com.example.aibookreader.presentation.screens.reader

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetDefaults
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
import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.presentation.screens.reader.components.AiAssistantSheetContent
import com.example.aibookreader.presentation.screens.reader.components.EpubPage
import com.example.aibookreader.presentation.screens.reader.components.ReaderBottomBar
import com.example.aibookreader.presentation.screens.reader.components.ReaderTopAppBar
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

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,

    )

    LaunchedEffect(Unit) { viewModel.openBook(bookId = bookId) }

    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.totalPages }
    )

    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && !initialScrollDone) {
            if (pagerState.currentPage != state.currentPage) {
                pagerState.scrollToPage(state.currentPage)
            }
            initialScrollDone = true
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                if (initialScrollDone && page != state.currentPage) {
                    viewModel.loadPage(page)
                }
            }
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
                        currentPage = state.currentPage + 1,
                        totalPages = state.totalPages,
                        onPreviousPage = { viewModel.previousPage() },
                        onNextPage = { viewModel.nextPage() },
                        onAiClick = { viewModel.openAiFromBottomBar() }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    userScrollEnabled = !state.isSheetOpen
                ) { _ ->
                    if (!state.isLoading) {
                        // Используем SelectableTextView с кастомным ActionMode.Callback
                        EpubPage(
                            blocks = state.blocks,
                            onTap = { showControls = !showControls },
                            onAiSelected = { selectedText ->
                                viewModel.setSelectedText(selectedText)
                            },
                            selectionKey = state.selectionKey
                        )
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }
                }

                if (state.isSheetOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.closeSheet() },
                        sheetState = sheetState
                    ) {
                        AiAssistantSheetContent(
                            uiState = state,
                            onActionClick = { action -> viewModel.onAiActionClick(action) },
                            onSendMessage = { text -> viewModel.sendChatMessage(text) },
                            onClearHistory = { viewModel.clearChatHistory() },
                            onSwitchToChat = { viewModel.switchToChat() },
                            onSwitchToActions = { viewModel.switchToActions() }
                        )
                    }
                }
            }
        }
    }
}