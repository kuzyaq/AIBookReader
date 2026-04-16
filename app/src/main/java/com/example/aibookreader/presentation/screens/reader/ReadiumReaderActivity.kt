package com.example.aibookreader.presentation.screens.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.aibookreader.databinding.ActivityReadiumReaderBinding
import com.example.aibookreader.presentation.screens.reader.components.AiAssistantSheetContent
import com.example.aibookreader.presentation.theme.AIBookReaderTheme
import com.example.aibookreader.presentation.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Экран чтения EPUB: тяжёлый UI (Compose) вокруг [ReadiumReaderFragment].
 * Readium сам рисует книгу; Activity ждёт [EpubNavigatorFactory] из ViewModel и тогда вставляет фрагмент.
 */
@AndroidEntryPoint
class ReadiumReaderActivity : FragmentActivity() {

    private lateinit var binding: ActivityReadiumReaderBinding
    private val viewModel: ReadiumReaderViewModel by viewModels()
    private var readerFragmentAdded = false

    @Inject
    lateinit var themeManager: ThemeManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadiumReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            readerFragmentAdded = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) != null
        }

        viewModel.openBook()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    if (!readerFragmentAdded && state.navigatorFactory != null) {
                        supportFragmentManager.commit {
                            replace(binding.fragmentHost.id, ReadiumReaderFragment::class.java, null, FRAGMENT_TAG)
                            setReorderingAllowed(true)
                        }
                        readerFragmentAdded = true
                    }
                }
            }
        }

        setupTopBar()
        setupBottomBar()
        setupOverlay()
    }

    private fun setupTopBar() {
        binding.composeTopBar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsState()

                AIBookReaderTheme(darkTheme = state.isDarkMode, isReaderMode = true) {
                    AnimatedVisibility(
                        visible = state.showControls,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                    ) {
                        Surface(color = MaterialTheme.colorScheme.primary) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Назад",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Text(
                                    text = state.book?.title ?: "Книга",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.toggleTheme() }) {
                                    Icon(
                                        if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Тема",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                IconButton(onClick = { /* TODO: bookmarks */ }) {
                                    Icon(
                                        Icons.Default.BookmarkBorder,
                                        contentDescription = "Закладки",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setupBottomBar() {
        binding.composeBottomBar.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsState()

                AIBookReaderTheme(darkTheme = state.isDarkMode, isReaderMode = true) {
                    AnimatedVisibility(
                        visible = state.showControls,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        Surface(color = MaterialTheme.colorScheme.primary) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { viewModel.goBackward() },
                                    enabled = state.currentPage > 1
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack, null,
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(
                                            alpha = if (state.currentPage > 1) 1f else 0.4f
                                        )
                                    )
                                }

                                IconButton(onClick = { viewModel.openSettings() }) {
                                    Icon(
                                        Icons.Default.TextFields, "Настройки текста",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                Text(
                                    text = "${state.currentPage} / ${state.totalPages}",
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                IconButton(onClick = { viewModel.openAiSheet() }) {
                                    Icon(
                                        Icons.Default.AutoAwesome, "ИИ",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.goForward() },
                                    enabled = state.currentPage < state.totalPages
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward, null,
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(
                                            alpha = if (state.currentPage < state.totalPages) 1f else 0.4f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setupOverlay() {
        binding.composeOverlay.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsState()

                val needsOverlay = state.isLoading || state.error != null || state.isSettingsOpen || state.isSheetOpen
                binding.composeOverlay.visibility = if (needsOverlay) View.VISIBLE else View.GONE

                AIBookReaderTheme(darkTheme = state.isDarkMode, isReaderMode = true) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (state.isLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        state.error?.let { error ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        if (state.isSettingsOpen) {
                            val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                            ModalBottomSheet(
                                onDismissRequest = { viewModel.closeSettings() },
                                sheetState = settingsSheetState
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = 24.dp)
                                        .navigationBarsPadding()
                                ) {
                                    Text(
                                        "Настройки текста",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(bottom = 20.dp)
                                    )

                                    Text(
                                        "Размер шрифта: ${(state.fontSizeMultiplier * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Slider(
                                        value = state.fontSizeMultiplier.toFloat(),
                                        onValueChange = { viewModel.setFontSize(it.toDouble()) },
                                        valueRange = 0.5f..3.0f,
                                        steps = 9,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    Text(
                                        "Межстрочный интервал: ${String.format("%.1f", state.lineHeightMultiplier)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Slider(
                                        value = state.lineHeightMultiplier.toFloat(),
                                        onValueChange = { viewModel.setLineHeight(it.toDouble()) },
                                        valueRange = 1.0f..2.5f,
                                        steps = 5,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }

                        if (state.isSheetOpen) {
                            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                            ModalBottomSheet(
                                onDismissRequest = { viewModel.closeSheet() },
                                sheetState = sheetState
                            ) {
                                AiAssistantSheetContent(
                                    uiState = ReaderUiState(
                                        selectedText = state.selectedText,
                                        isSheetOpen = state.isSheetOpen,
                                        isAiLoading = state.isAiLoading,
                                        aiError = state.aiError,
                                        chatMessages = state.chatMessages,
                                        isActionMode = state.isActionMode
                                    ),
                                    onActionClick = { viewModel.onAiActionClick(it) },
                                    onSendMessage = { viewModel.sendChatMessage(it) },
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
    }

    companion object {
        private const val FRAGMENT_TAG = "reader_fragment"
        private const val EXTRA_BOOK_ID = "bookId"

        fun createIntent(context: Context, bookId: Int): Intent =
            Intent(context, ReadiumReaderActivity::class.java).apply {
                putExtra(EXTRA_BOOK_ID, bookId)
            }
    }
}
