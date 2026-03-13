package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.repository.BookRepository
import javax.inject.Inject

class DeleteBookUseCase @Inject constructor(
    private val repository: BookRepository
) {
    suspend operator fun invoke(id: Int): Result<Unit> {
        return repository.deleteBook(id)
    }
}