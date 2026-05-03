package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.repository.BookRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetBooksUseCaseTest {

    private val repository = mockk<BookRepository>()
    private val useCase = GetBooksUseCase(repository)

    @Test
    fun `invoke returns same flow as repository getAllBooks`() = runTest {
        val books = listOf(BookTestFixtures.book(id = 1), BookTestFixtures.book(id = 2))
        every { repository.getAllBooks() } returns flowOf(books)

        val emitted = useCase().first()

        assertEquals(books, emitted)
        verify(exactly = 1) { repository.getAllBooks() }
    }

    @Test
    fun `flow completes after delayed emission with virtual time`() = runTest {
        val books = listOf(BookTestFixtures.book())
        every { repository.getAllBooks() } returns flow {
            delay(1_000)
            emit(books)
        }

        val deferred = async { useCase().first() }
        advanceUntilIdle()

        assertEquals(books, deferred.await())
    }
}
