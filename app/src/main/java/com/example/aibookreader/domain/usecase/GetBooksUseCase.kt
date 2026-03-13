package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBooksUseCase @Inject constructor(
    private val repository: BookRepository
) {
    operator fun invoke(): Flow<List<Book>>{
        return repository.getAllBooks()
    }
}