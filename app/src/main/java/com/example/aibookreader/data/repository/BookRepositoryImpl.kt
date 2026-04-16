package com.example.aibookreader.data.repository

import android.util.Log
import com.example.aibookreader.data.local.dao.BookDao
import com.example.aibookreader.data.local.dao.ReadingProgressDao
import com.example.aibookreader.data.local.entity.BookEntity
import com.example.aibookreader.data.local.entity.ReadingProgressEntity
import com.example.aibookreader.data.local.mapper.BookMapper
import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.model.BookFormat
import com.example.aibookreader.domain.model.BookStatus
import com.example.aibookreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val readingProgressDao: ReadingProgressDao
) : BookRepository {

    override fun getAllBooks(): Flow<List<Book>> {
        return combine(
            bookDao.getAllBooks(),
            readingProgressDao.observeAll()
        ) { books, progressRows ->
            val progressByBookId = progressRows.associateBy { it.bookId }
            BookMapper.toDomainList(books, progressByBookId)
        }
    }

    override suspend fun getBookById(id: Int): Book? {
        return try {
            val entity = bookDao.getBookById(id) ?: return null
            val progress = readingProgressDao.getByBookId(id)
            BookMapper.toDomain(entity, progress)
        } catch (e: SQLException) {
            Log.e("BookRepImpl", "Ошибка получения книги с ID $id")
            throw e
        }
    }

    override suspend fun getBookByPath(filePath: String): Book? {
        return try {
            val entity = bookDao.getBookByPath(filePath) ?: return null
            val progress = readingProgressDao.getByBookId(entity.id)
            BookMapper.toDomain(entity, progress)
        } catch (e: SQLException) {
            Log.e("BookRepImpl", "Ошибка получения книги с path $filePath")
            throw e
        }
    }

    override fun searchBooks(query: String): Flow<List<Book>> {
        return combine(
            bookDao.searchBooks(query),
            readingProgressDao.observeAll()
        ) { books, progressRows ->
            val progressByBookId = progressRows.associateBy { it.bookId }
            BookMapper.toDomainList(books, progressByBookId)
        }
    }

    override suspend fun addBook(book: Book): Result<Int> {
        return try {
            val insertedId = bookDao.insertBook(BookMapper.toEntity(book.copy(id = 0))).toInt()
            readingProgressDao.upsert(
                BookMapper.toProgressEntity(
                    book.copy(id = insertedId)
                )
            )
            Result.success(insertedId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBook(book: Book): Result<Unit> {
        return try {
            bookDao.updateBook(BookMapper.toEntity(book))
            readingProgressDao.upsert(BookMapper.toProgressEntity(book))
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
        val format = if (filePath.endsWith(".pdf", ignoreCase = true)) BookFormat.PDF else BookFormat.EPUB
        val entity = BookEntity(
            title = File(filePath).nameWithoutExtension,
            author = "",
            filePath = filePath,
            fileSize = fileSize,
            status = BookStatus.IMPORTING,
            totalPages = 0,
            format = format
        )
        val insertedId = bookDao.insertBook(entity).toInt()
        val now = System.currentTimeMillis()
        readingProgressDao.upsert(
            ReadingProgressEntity(
                bookId = insertedId,
                lastReadAt = now
            )
        )
    }

    override suspend fun finishImport(
        originalPath: String,
        newPath: String,
        title: String,
        author: String,
        cover: String?,
        pages: Int,
        extractedDir: String?,
        opfBasePath: String?,
        format: BookFormat
    ): Int {
        val fileSize = File(newPath).takeIf { it.exists() }?.length() ?: 0L
        bookDao.finishImport(
            originalPath = originalPath,
            newPath = newPath,
            title = title,
            author = author,
            cover = cover,
            pages = pages,
            status = BookStatus.READY,
            extractedDir = extractedDir,
            opfBasePath = opfBasePath,
            fileSize = fileSize,
            format = format
        )
        val book = bookDao.getBookByPath(newPath) ?: throw IllegalStateException("Book not found after import")
        val existing = readingProgressDao.getByBookId(book.id)
        readingProgressDao.upsert(
            ReadingProgressEntity(
                bookId = book.id,
                locatorJson = existing?.locatorJson,
                currentPageIndex = existing?.currentPageIndex ?: 0,
                lastReadAt = existing?.lastReadAt ?: System.currentTimeMillis(),
                progressFraction = existing?.progressFraction,
                remoteProgressVersion = existing?.remoteProgressVersion
            )
        )
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
            val now = System.currentTimeMillis()
            val existing = readingProgressDao.getByBookId(bookId)
            readingProgressDao.upsert(
                ReadingProgressEntity(
                    bookId = bookId,
                    locatorJson = existing?.locatorJson,
                    currentPageIndex = page,
                    lastReadAt = now,
                    progressFraction = existing?.progressFraction,
                    remoteProgressVersion = existing?.remoteProgressVersion
                )
            )
            bookDao.updateLastReadAt(bookId, now)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveLocator(bookId: Int, locatorJson: String?) {
        val now = System.currentTimeMillis()
        val existing = readingProgressDao.getByBookId(bookId)
        val fraction = fractionFromLocatorJson(locatorJson)
        readingProgressDao.upsert(
            ReadingProgressEntity(
                bookId = bookId,
                locatorJson = locatorJson,
                currentPageIndex = existing?.currentPageIndex ?: 0,
                lastReadAt = now,
                progressFraction = fraction ?: existing?.progressFraction,
                remoteProgressVersion = existing?.remoteProgressVersion
            )
        )
        bookDao.updateLastReadAt(bookId, now)
    }

    private fun fractionFromLocatorJson(locatorJson: String?): Float? {
        if (locatorJson.isNullOrBlank()) return null
        return try {
            val tp = JSONObject(locatorJson).optJSONObject("locations")?.optDouble("totalProgression")
                ?: return null
            if (tp.isNaN()) null else tp.toFloat().coerceIn(0f, 1f)
        } catch (_: Exception) {
            null
        }
    }
}
