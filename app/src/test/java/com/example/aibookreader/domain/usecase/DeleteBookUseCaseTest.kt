package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteBookUseCaseTest {

    private val repository = mockk<BookRepository>()
    private val useCase = DeleteBookUseCase(repository)

    @Test
    fun `returns repository result on success`() = runTest {
        coEvery { repository.deleteBook(3) } returns Result.success(Unit)

        val result = useCase(3)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.deleteBook(3) }
    }

    @Test
    fun `returns repository failure unchanged`() = runTest {
        val error = IllegalStateException("db locked")
        coEvery { repository.deleteBook(1) } returns Result.failure(error)

        val result = useCase(1)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() === error)
    }
}
