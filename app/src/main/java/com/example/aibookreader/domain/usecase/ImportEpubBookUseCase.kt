package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.repository.BookRepository
import com.example.aibookreader.domain.repository.FileStorageRepository
import com.example.aibookreader.domain.repository.ReaderRepository
import com.example.aibookreader.domain.service.EpubParser
import javax.inject.Inject

class ImportEpubBookUseCase @Inject constructor(

    private val bookRepository: BookRepository,
    private val readerRepository: ReaderRepository,
    private val epubParser: EpubParser,
    private val fileStorageRepository: FileStorageRepository

    ) {
    suspend operator fun invoke(
        filePath: String
    ): Result<Unit> {
        return try {
            val internalPath = fileStorageRepository.copyBookToInternal(filePath)

            val epubData = epubParser.parse(internalPath)

            val coverPath = epubData.coverImageBytes?.let{
                fileStorageRepository.saveCover(it)
            }

            val bookId = bookRepository.finishImport(
                originalPath = filePath,
                newPath = internalPath,
                title = epubData.title ?: "Без названия",
                author = epubData.author ?: "Автор не указан",
                cover = coverPath,
                pages = epubData.pages.size,
                fullText = epubData.fullText
            )

            readerRepository.savePagesHtml(
                bookId = bookId,
                pages = epubData.pages
            )

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}