package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.repository.BookRepository
import javax.inject.Inject

class GetBookByIdUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(id: Int): Book? {
        return repository.getBookById(id)
    }
}