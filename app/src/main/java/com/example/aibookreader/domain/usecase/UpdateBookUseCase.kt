package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.repository.BookRepository
import javax.inject.Inject

class UpdateBookUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(book: Book): Result<Unit> {
        return repository.updateBook(book)
    }
}