package com.example.aibookreader.presentation.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aibookreader.domain.usecase.GetBookByIdUseCase
import com.example.aibookreader.domain.usecase.GetReaderPageUseCase
import com.example.aibookreader.domain.usecase.UpdateReadingProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(

    private val getReaderPageUseCase: GetReaderPageUseCase,
    private val updateReadingProgressUseCase: UpdateReadingProgressUseCase,
    private val getBookByIdUseCase: GetBookByIdUseCase


) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentBookId: Int? = null

    fun openBook(
        bookId: Int
    ) {
        if( currentBookId == bookId ) return

        currentBookId = bookId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val book = getBookByIdUseCase(bookId)
            book?.let{
                _uiState.update { state ->
                    state.copy(
                        title = it.title,
                        totalPages = it.totalPages,
                        currentPage = it.currentPage
                    )
                }
                loadPage(it.currentPage)
            }
        }
    }

    fun nextPage() {
        if (_uiState.value.currentPage < _uiState.value.totalPages - 1) {
            loadPage(_uiState.value.currentPage + 1)
        }
    }

    fun previousPage() {
        if (_uiState.value.currentPage > 0) {
            loadPage(_uiState.value.currentPage - 1)
        }
    }

     fun loadPage(page: Int) {
        val bookId = currentBookId ?: return
         android.util.Log.d("ReaderVM", "Запрос в БД: bookId=$bookId, page=$page")

        viewModelScope.launch {
            try{
                val blocks = getReaderPageUseCase(
                    bookId = bookId,
                    page = page
                )
                android.util.Log.d("ReaderVM", "Loaded ${blocks.size} blocks for page $page")

                _uiState.update { it.copy(
                    blocks = blocks,
                    currentPage = page,
                    isLoading = false
                ) }

                updateReadingProgressUseCase(bookId, page)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}