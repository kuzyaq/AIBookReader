package com.example.aibookreader.domain.usecase

import android.content.Context
import com.example.aibookreader.data.pdf.PdfProcessor
import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.model.BookFormat
import com.example.aibookreader.domain.model.BookStatus
import com.example.aibookreader.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ImportPdfBookUseCase @Inject constructor(
    private val repository: BookRepository,
    private val pdfProcessor: PdfProcessor
) {
    suspend operator fun invoke(
        filePath: String,
        context: Context,
        remoteBookId: String? = null
    ): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val pdfData = pdfProcessor.processPdf(filePath, context)
                val book = createBookFromPdf(pdfData, filePath, remoteBookId)
                repository.addBook(book)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun createBookFromPdf(
        pdfData: PdfProcessor.PdfData,
        filePath: String,
        remoteBookId: String?
    ): Book {
        return Book(
            id = 0,
            title = pdfData.title?.takeIf { it.isNotBlank() } ?: "Без названия",
            author = pdfData.author?.takeIf { it.isNotBlank() } ?: "Неизвестный автор",
            coverImage = pdfData.coverImagePath,
            currentPage = 0,
            totalPages = pdfData.totalPages,
            filePath = filePath,
            fileSize = File(filePath).length(),
            createdAt = System.currentTimeMillis(),
            lastReadAt = System.currentTimeMillis(),
            status = BookStatus.READY,
            format = BookFormat.PDF,
            remoteBookId = remoteBookId,
            remoteBookVersion = remoteBookId?.let { 1L }
        )
    }
}
