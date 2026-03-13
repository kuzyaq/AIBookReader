package com.example.aibookreader.domain.usecase

import androidx.room.Query
import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchBooksUseCase @Inject constructor(
    private val repository: BookRepository
) {
    operator fun invoke(query: String): Flow<List<Book>> {
        return repository.searchBooks(query)
    }
}