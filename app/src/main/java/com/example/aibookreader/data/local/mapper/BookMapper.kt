package com.example.aibookreader.data.local.mapper

import com.example.aibookreader.data.local.entity.BookEntity
import com.example.aibookreader.data.local.entity.ReadingProgressEntity
import com.example.aibookreader.domain.model.Book

object BookMapper {

    fun toDomain(entity: BookEntity, progress: ReadingProgressEntity?): Book {
        val p = progress
        return Book(
            id = entity.id,
            title = entity.title,
            author = entity.author,
            coverImage = entity.coverImage,
            status = entity.status,
            currentPage = p?.currentPageIndex ?: 0,
            totalPages = entity.totalPages,
            createdAt = entity.createdAt,
            lastReadAt = entity.lastReadAt,
            fileSize = entity.fileSize,
            filePath = entity.filePath,
            extractedDir = entity.extractedDir,
            opfBasePath = entity.opfBasePath,
            locator = p?.locatorJson,
            format = entity.format,
            progressFraction = p?.progressFraction,
            remoteBookId = entity.remoteBookId,
            remoteBookVersion = entity.remoteBookVersion,
            remoteProgressVersion = p?.remoteProgressVersion
        )
    }

    fun toEntity(domain: Book): BookEntity {
        return BookEntity(
            id = domain.id,
            title = domain.title,
            author = domain.author,
            coverImage = domain.coverImage,
            status = domain.status,
            totalPages = domain.totalPages,
            createdAt = domain.createdAt,
            lastReadAt = domain.lastReadAt,
            fileSize = domain.fileSize,
            filePath = domain.filePath,
            extractedDir = domain.extractedDir,
            opfBasePath = domain.opfBasePath,
            format = domain.format,
            remoteBookId = domain.remoteBookId,
            remoteBookVersion = domain.remoteBookVersion
        )
    }

    fun toProgressEntity(domain: Book): ReadingProgressEntity {
        return ReadingProgressEntity(
            bookId = domain.id,
            locatorJson = domain.locator,
            currentPageIndex = domain.currentPage,
            lastReadAt = domain.lastReadAt,
            progressFraction = domain.progressFraction,
            remoteProgressVersion = domain.remoteProgressVersion
        )
    }

    fun toDomainList(entities: List<BookEntity>, progressByBookId: Map<Int, ReadingProgressEntity>): List<Book> {
        return entities.map { toDomain(it, progressByBookId[it.id]) }
    }
}
