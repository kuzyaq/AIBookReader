package com.example.aibookreader.data.local.mapper

import com.example.aibookreader.data.local.entity.BookEntity
import com.example.aibookreader.domain.model.Book

object BookMapper {

    fun toDomain(entity: BookEntity): Book {
        return Book(
            id = entity.id,
            title = entity.title,
            author = entity.author,
            coverImage = entity.coverImage,
            status = entity.status,
            currentPage = entity.currentPage,
            totalPages = entity.totalPages,
            createdAt = entity.createdAt,
            lastReadAt = entity.lastReadAt,
            fileSize = entity.fileSize,
            filePath = entity.filePath,
            fullText = entity.fullText
        )
    }

    fun toEntity(domain: Book): BookEntity {
        return BookEntity(
            id = domain.id,
            title = domain.title,
            author = domain.author,
            fullText = domain.fullText,
            coverImage = domain.coverImage,
            status = domain.status,
            currentPage = domain.currentPage,
            totalPages = domain.totalPages,
            createdAt = domain.createdAt,
            lastReadAt = domain.lastReadAt,
            fileSize = domain.fileSize,
            filePath = domain.filePath
        )
    }

    fun toDomainList(entities: List<BookEntity>): List<Book> {
        return entities.map {toDomain(it)}
    }
}