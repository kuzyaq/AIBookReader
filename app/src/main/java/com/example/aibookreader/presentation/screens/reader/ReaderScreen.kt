package com.example.aibookreader.presentation.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.presentation.theme.AIBookReaderTheme

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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTextForSheet by remember { mutableStateOf<String?>(null) }
    var isSheetOpen by remember { mutableStateOf(false) }

    // Создаём тулбар. Callback всегда актуален благодаря Wrapper внутри.
    val aiToolbar = rememberAiTextToolbar { selectedText ->
        selectedTextForSheet = selectedText
        isSheetOpen = true
    }

    LaunchedEffect(Unit) { viewModel.openBook(bookId = bookId) }

    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.totalPages }
    )
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            viewModel.loadPage(pagerState.currentPage)
        }
    }
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.scrollToPage(state.currentPage)
        }
    }

    AIBookReaderTheme(darkTheme = darkMode, isReaderMode = true) {

        // ⚠️ CompositionLocalProvider СНАРУЖИ Scaffold, чтобы тулбар
        // был доступен для всего поддерева, включая SelectionContainer
        CompositionLocalProvider(LocalTextToolbar provides aiToolbar) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                        ReaderTopAppBar(
                            onNavigateBack = onNavigateBack,
                            onToggleTheme  = { darkMode = !darkMode },
                            bookTitle      = state.title ?: "Без названия"
                        )
                    }
                },
                bottomBar = {
                    AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                        ReaderBottomBar(
                            currentPage   = state.currentPage + 1,
                            totalPages    = state.totalPages,
                            onPreviousPage = { viewModel.previousPage() },
                            onNextPage     = { viewModel.nextPage() }
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize()) {

                    // ⚠️ ОДИН SelectionContainer на весь Pager.
                    // Не дублировать внутри EpubPage — это ломает тулбар.
                    SelectionContainer {
                        HorizontalPager(
                            state    = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) { _ ->
                            if (!state.isLoading) {
                                EpubPage(
                                    blocks = state.blocks,
                                    onTap  = { showControls = !showControls }
                                )
                            } else {
                                Box(Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }

                    if (isSheetOpen && selectedTextForSheet != null) {
                        ModalBottomSheet(
                            onDismissRequest = { isSheetOpen = false },
                            sheetState       = sheetState,
                            containerColor   = MaterialTheme.colorScheme.surface,
                            dragHandle       = { BottomSheetDefaults.DragHandle() }
                        ) {
                            AiAssistantSheetContent(
                                text             = selectedTextForSheet!!,
                                onActionSelected = { action ->
                                    isSheetOpen = false
                                    // TODO: viewModel.analyzeText(selectedTextForSheet!!, action)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EpubPage — БЕЗ SelectionContainer внутри
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EpubPage(
    blocks: List<ReaderBlock>,
    onTap : () -> Unit = {}
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is ReaderBlock.Title -> Text(
                    text     = block.text,
                    style    = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 28.sp
                    ),
                    color    = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                is ReaderBlock.Paragraph -> Text(
                    text      = block.text,
                    style     = MaterialTheme.typography.bodyLarge.copy(
                        fontSize      = 18.sp,
                        lineHeight    = 28.sp,
                        letterSpacing = 0.3.sp
                    ),
                    textAlign = TextAlign.Justify,
                    color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    modifier  = Modifier.padding(vertical = 8.dp)
                )
                is ReaderBlock.Image -> AsyncImage(
                    model             = block.src,
                    contentDescription = null,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                is ReaderBlock.Quote -> Text(
                    text     = block.text,
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BottomSheet контент
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AiAssistantSheetContent(text: String, onActionSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text  = "Выбранный текст:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text     = "«$text»",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ListItem(
            headlineContent = { Text("Объяснить значение") },
            leadingContent  = { Icon(Icons.Default.AutoAwesome, null) },
            modifier        = Modifier.clickable { onActionSelected("explain") }
        )
        ListItem(
            headlineContent = { Text("Создать квиз") },
            leadingContent  = { Icon(Icons.Default.Quiz, null) },
            modifier        = Modifier.clickable { onActionSelected("quiz") }
        )
        ListItem(
            headlineContent = { Text("Сделать саммари") },
            leadingContent  = { Icon(Icons.Default.Summarize, null) },
            modifier        = Modifier.clickable { onActionSelected("summary") }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TopAppBar / BottomBar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopAppBar(
    onNavigateBack: () -> Unit,
    onToggleTheme : () -> Unit,
    bookTitle     : String
) {
    TopAppBar(
        title           = { Text(bookTitle, color = MaterialTheme.colorScheme.onPrimary) },
        navigationIcon  = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        actions = {
            IconButton(onClick = onToggleTheme) {
                Icon(Icons.Default.DarkMode, null,
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        colors = topAppBarColors().copy(containerColor = MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun ReaderBottomBar(
    currentPage   : Int,
    totalPages    : Int,
    onPreviousPage: () -> Unit,
    onNextPage    : () -> Unit
) {
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.primary) {
        Row(
            modifier            = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousPage, enabled = currentPage > 1) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = if (currentPage > 1) 1f else 0.6f))
            }
            Text("$currentPage / $totalPages",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
            IconButton(onClick = onNextPage, enabled = currentPage < totalPages) {
                Icon(Icons.Default.ArrowForward, null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = if (currentPage < totalPages) 1f else 0.6f))
            }
        }
    }
}