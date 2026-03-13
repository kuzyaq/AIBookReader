package com.example.aibookreader.domain.usecase

import android.content.Context
import com.example.aibookreader.data.pdf.PdfProcessor
import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ImportPdfBookUseCase @Inject constructor(
    private val repository: BookRepository,
    private val pdfProcessor: PdfProcessor
) {
    suspend operator fun invoke(filePath: String, context: Context): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                // Обрабатываем PDF и получаем метаданные
                val pdfData = pdfProcessor.processPdf(filePath, context)

                // Создаем книгу с точным количеством страниц из PDF
                val book = createBookFromPdf(pdfData, filePath)

                // Сохраняем в БД
                repository.addBook(book)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun createBookFromPdf(pdfData: PdfProcessor.PdfData, filePath: String): Book {
        return Book(
            title = pdfData.title?.takeIf { it.isNotBlank() } ?: "Без названия",
            author = pdfData.author?.takeIf { it.isNotBlank() } ?: "Неизвестный автор",
            fullText = pdfData.content ?: "", // Можно оставить пустым, загружать постранично
            coverImage = pdfData.coverImagePath,
            currentPage = 1,
            totalPages = pdfData.totalPages, // ← Используем точное количество из PDF!
            filePath = filePath,
            fileSize = File(filePath).length(),
            id = TODO(),
            createdAt = TODO(),
            lastReadAt = TODO(),
            status = TODO(),
        )
    }
}