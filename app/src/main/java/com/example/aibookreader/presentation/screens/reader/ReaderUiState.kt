package com.example.aibookreader.presentation.screens.reader

import com.example.aibookreader.domain.model.ReaderBlock

data class ReaderUiState (

    val isLoading: Boolean = true,

    val blocks: List<ReaderBlock> = emptyList(),

    val title: String? = "Без названия",

    val currentPage: Int = 0,

    val totalPages: Int = 0,

    val error: String? = null
)