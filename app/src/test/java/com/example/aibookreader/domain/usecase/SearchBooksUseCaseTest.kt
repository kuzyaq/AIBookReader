package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.repository.BookRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchBooksUseCaseTest {

    private val repository = mockk<BookRepository>()
    private val useCase = SearchBooksUseCase(repository)

    @Test
    fun `forwards query to repository and emits results`() = runTest {
        val query = "kotlin"
        val books = listOf(BookTestFixtures.book(title = "Kotlin in Action"))
        every { repository.searchBooks(query) } returns flowOf(books)

        val result = useCase(query).first()

        assertEquals(books, result)
        verify(exactly = 1) { repository.searchBooks(query) }
    }

    @Test
    fun `blank query still delegated to repository`() = runTest {
        every { repository.searchBooks("") } returns flowOf(emptyList())

        val result = useCase("").first()

        assertTrue(result.isEmpty())
        verify { repository.searchBooks("") }
    }
}
