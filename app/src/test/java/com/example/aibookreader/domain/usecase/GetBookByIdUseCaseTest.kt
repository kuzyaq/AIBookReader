package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetBookByIdUseCaseTest {

    private val repository = mockk<BookRepository>()
    private val useCase = GetBookByIdUseCase(repository)

    @Test
    fun `returns book when repository finds it`() = runTest {
        val book = BookTestFixtures.book(id = 42)
        coEvery { repository.getBookById(42) } returns book

        val result = useCase(42)

        assertEquals(book, result)
        coVerify(exactly = 1) { repository.getBookById(42) }
    }

    @Test
    fun `returns null when repository has no book`() = runTest {
        coEvery { repository.getBookById(99) } returns null

        val result = useCase(99)

        assertNull(result)
        coVerify(exactly = 1) { repository.getBookById(99) }
    }

    @Test
    fun `suspend call runs on StandardTestDispatcher when scope uses it`() = runTest(StandardTestDispatcher()) {
        val book = BookTestFixtures.book(id = 7)
        coEvery { repository.getBookById(7) } returns book

        assertEquals(book, useCase(7))
    }
}
