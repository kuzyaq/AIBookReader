package com.example.aibookreader.presentation.screens.reader

import com.example.aibookreader.domain.model.ChatMessage
import com.example.aibookreader.domain.model.ReaderBlock



data class ReaderUiState (

    // Состояние книги
    val isLoading: Boolean = true,
    val blocks: List<ReaderBlock> = emptyList(),
    val title: String? = "Без названия",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val error: String? = null,

    // Состояние ИИ
    val selectedText: String? = null,
    val isSheetOpen: Boolean = false,
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(), // История чата в шторке

)