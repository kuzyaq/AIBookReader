package com.example.aibookreader.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.usecase.AddBookUseCase
import com.example.aibookreader.domain.usecase.DeleteBookUseCase
import com.example.aibookreader.domain.usecase.GetBooksUseCase
import com.example.aibookreader.domain.usecase.ImportPdfBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import android.util.Log
import com.example.aibookreader.data.worker.ImportBookScheduler
import com.example.aibookreader.domain.usecase.ImportEpubBookUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBooksUseCase: GetBooksUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val importPdfBookUseCase: ImportPdfBookUseCase,
    private val importEpubBookUseCase: ImportEpubBookUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,

    private val importBookScheduler: ImportBookScheduler,

    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()


    init {
        observeBooks()
    }

    private fun observeBooks() {
        getBooksUseCase()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { books ->
                _uiState.update { it.copy(books = books, isLoading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun deleteBook(id: Int) {
        viewModelScope.launch {
            deleteBookUseCase(id)
        }
    }
}
