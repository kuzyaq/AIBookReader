package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.repository.BookRepository
import javax.inject.Inject

class UpdateReadingProgressUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {

    suspend operator fun invoke(
        bookId: Int,
        page: Int
    ) {

        bookRepository.updateReadingProgress(
            bookId = bookId,
            page = page
        )

    }

}