package com.example.aibookreader.domain.usecase

import com.example.aibookreader.data.epub.EpubExtractor
import com.example.aibookreader.data.epub.OpfParser
import com.example.aibookreader.domain.model.BookFormat
import com.example.aibookreader.domain.repository.BookRepository
import com.example.aibookreader.domain.repository.ChapterInfo
import com.example.aibookreader.domain.repository.FileStorageRepository
import com.example.aibookreader.domain.repository.ReaderRepository
import java.io.File
import javax.inject.Inject

class ImportEpubBookUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val readerRepository: ReaderRepository,
    private val epubExtractor: EpubExtractor,
    private val fileStorageRepository: FileStorageRepository
) {
    suspend operator fun invoke(filePath: String, remoteBookId: String? = null): Result<Unit> {
        return try {
            val internalPath = fileStorageRepository.copyBookToInternal(filePath)

            val extractedDir = epubExtractor.extract(internalPath)
            val manifest = OpfParser.parse(extractedDir)

            val coverPath = manifest.coverPath?.let { path ->
                File(path).readBytes().let { bytes ->
                    fileStorageRepository.saveCover(bytes)
                }
            }

            val bookId = bookRepository.finishImport(
                originalPath = filePath,
                newPath = internalPath,
                title = manifest.title,
                author = manifest.author,
                cover = coverPath,
                pages = manifest.spineItems.size,
                extractedDir = extractedDir.absolutePath,
                opfBasePath = manifest.basePath.absolutePath,
                format = BookFormat.EPUB,
                remoteBookId = remoteBookId
            )

            val chapters = manifest.spineItems.mapIndexed { index, spine ->
                ChapterInfo(
                    spineIndex = index,
                    href = spine.href,
                    title = null
                )
            }
            readerRepository.saveChapters(bookId, chapters)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
