package com.example.aibookreader.presentation.screens.home

import com.example.aibookreader.domain.model.Book

data class HomeUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)