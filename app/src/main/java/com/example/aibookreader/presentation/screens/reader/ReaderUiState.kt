package com.example.aibookreader.presentation.screens.reader

import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.domain.model.ReaderSettings
import com.example.aibookreader.domain.repository.ChapterInfo

data class ReaderUiState(

    val isLoading: Boolean = true,
    val blocks: List<ReaderBlock> = emptyList(),
    val title: String? = "Без названия",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val error: String? = null,

    val useWebView: Boolean = false,
    val chapters: List<ChapterInfo> = emptyList(),
    val currentChapterIndex: Int = 0,
    val currentPageInChapter: Int = 0,
    val pagesInCurrentChapter: Int = 1,
    val chapterFilePath: String? = null,
    val basePath: String? = null,
    val chapterPageCounts: Map<Int, Int> = emptyMap(),

    val readerSettings: ReaderSettings = ReaderSettings(),
    val isTextSettingsOpen: Boolean = false,

    val selectedText: String? = null,
    val isSheetOpen: Boolean = false,
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isActionMode: Boolean = true,

    val selectionKey: Int = 0
)
