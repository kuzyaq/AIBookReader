package com.example.aibookreader.domain.repository

import com.example.aibookreader.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository{

    fun getAllBooks(): Flow<List<Book>>

    suspend fun getBookById(id: Int): Book?
    suspend fun getBookByPath(filePath: String): Book?

    fun searchBooks(query: String): Flow<List<Book>>

    suspend fun addBook(book: Book): Result<Int>

    suspend fun updateBook(book: Book): Result<Unit>

    suspend fun deleteBook(id: Int): Result<Unit>

    suspend fun markImportFailed(filePath: String)

    suspend fun createImportPlaceholder(
        filePath: String, fileSize: Long
    )

    suspend fun finishImport(
        originalPath: String,
        newPath: String,
        title: String,
        author: String,
        cover: String?,
        pages: Int,
        fullText: String?
    ) : Int

    suspend fun updateReadingProgress(bookId: Int, page: Int): Result<Unit>
}