package com.example.aibookreader.domain.usecase

import com.example.aibookreader.domain.model.Book
import com.example.aibookreader.domain.model.BookFormat
import com.example.aibookreader.domain.model.BookStatus

object BookTestFixtures {

    fun book(
        id: Int = 1,
        title: String = "Test Book",
        author: String = "Test Author",
        currentPage: Int = 0,
        totalPages: Int = 100,
        status: BookStatus = BookStatus.READY,
        format: BookFormat = BookFormat.EPUB,
        filePath: String = "/tmp/test.epub"
    ) = Book(
        id = id,
        title = title,
        author = author,
        coverImage = null,
        currentPage = currentPage,
        totalPages = totalPages,
        createdAt = 0L,
        lastReadAt = 0L,
        fileSize = 1024L,
        filePath = filePath,
        status = status,
        format = format
    )
}
