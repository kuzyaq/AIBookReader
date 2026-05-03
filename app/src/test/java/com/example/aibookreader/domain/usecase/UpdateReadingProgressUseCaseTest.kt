package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateReadingProgressUseCaseTest {

    private val bookRepository = mockk<BookRepository>(relaxed = true)
    private val useCase = UpdateReadingProgressUseCase(bookRepository)

    @Test
    fun `delegates to bookRepository with same ids and page`() = runTest {
        coEvery { bookRepository.updateReadingProgress(10, 25) } returns Result.success(Unit)

        useCase(bookId = 10, page = 25)

        coVerify(exactly = 1) { bookRepository.updateReadingProgress(bookId = 10, page = 25) }
    }
}
