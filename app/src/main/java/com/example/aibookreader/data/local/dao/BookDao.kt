package com.example.aibookreader.data.local.dao

import androidx.room.*
import com.example.aibookreader.data.local.entity.BookEntity
import com.example.aibookreader.domain.model.BookFormat
import com.example.aibookreader.domain.model.BookStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastReadAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Int): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :filePath")
    suspend fun getBookByPath(filePath: String): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET status = :status WHERE filePath = :filePath")
    suspend fun updateStatus(filePath: String, status: BookStatus)

    @Query(
        """
        UPDATE books
        SET title = :title,
            author = :author,
            filePath = :newPath,
            coverImage = :cover,
            totalPages = :pages,
            status = :status,
            extractedDir = :extractedDir,
            opfBasePath = :opfBasePath,
            fileSize = :fileSize,
            format = :format
        WHERE filePath = :originalPath
        """
    )
    suspend fun finishImport(
        originalPath: String,
        newPath: String,
        title: String,
        author: String,
        cover: String?,
        pages: Int,
        status: BookStatus,
        extractedDir: String? = null,
        opfBasePath: String? = null,
        fileSize: Long,
        format: BookFormat
    )

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Int)

    @Query("UPDATE books SET lastReadAt = :timestamp WHERE id = :bookId")
    suspend fun updateLastReadAt(bookId: Int, timestamp: Long = System.currentTimeMillis())
}
