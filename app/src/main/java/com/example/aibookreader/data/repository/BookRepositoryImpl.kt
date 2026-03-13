package com.example.aibookreader.data.repository

import android.util.Log
import com.example.aibookreader.data.local.dao.BookDao
import com.example.aibookreader.data.local.entity.BookEntity
import com.example.aibookreader.data.local.entity.BookStatus
import com.example.aibookreader.data.local.mapper.BookMapper
import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {

    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { BookMapper.toDomainList(it) }
    }

    override suspend fun getBookById(id: Int): Book? {
        return try {
            bookDao.getBookById(id)?.let { BookMapper.toDomain(it) }
        } catch (e: SQLException) {
            Log.e("BookRepImpl", "Ошибка получения книги с ID $id")
            throw e
        }
    }

    override suspend fun getBookByPath(filePath: String): Book? {
        return try {
            bookDao.getBookByPath(filePath)?.let { BookMapper.toDomain(it) }
        } catch (e: SQLException) {
            Log.e("BookRepImpl", "Ошибка получения книги с path $filePath")
            throw e
        }
    }

    override fun searchBooks(query: String): Flow<List<Book>> {
        return bookDao.searchBooks(query).map { BookMapper.toDomainList(it) }
    }

    override suspend fun addBook(book: Book): Result<Int> {
        return try {
            val entityId = bookDao.insertBook(BookMapper.toEntity(book))
            Result.success(entityId.toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBook(book: Book): Result<Unit> {
        return try {
            bookDao.updateBook(BookMapper.toEntity(book))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBook(id: Int): Result<Unit> {
        return try {
            bookDao.deleteBookById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createImportPlaceholder(
        filePath: String,
        fileSize: Long
    ) {
        val entity = BookEntity(
            title = File(filePath).nameWithoutExtension,
            author = "",
            filePath = filePath,
            fileSize = fileSize,
            status = BookStatus.IMPORTING,
            totalPages = 0,
            currentPage = 0
        )

        bookDao.insertBook(entity)
    }

    override suspend fun finishImport(
        originalPath: String,
        newPath: String,
        title: String,
        author: String,
        cover: String?,
        pages: Int,
        fullText: String?
    ) : Int {
        bookDao.finishImport(
            originalPath = originalPath,
            newPath = newPath,
            title = title,
            author = author,
            cover = cover,
            pages = pages,
            text = fullText,
            status = BookStatus.READY
        )

        val book = bookDao.getBookByPath(newPath) ?: throw IllegalStateException("Book not found after import")
        return book.id
    }

    override suspend fun markImportFailed(filePath: String) {
        bookDao.updateStatus(filePath, BookStatus.FAILED)
    }

    override suspend fun updateReadingProgress(
        bookId: Int,
        page: Int
    ): Result<Unit> {
        return try {
            bookDao.updateReadingProgress(bookId, page)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}