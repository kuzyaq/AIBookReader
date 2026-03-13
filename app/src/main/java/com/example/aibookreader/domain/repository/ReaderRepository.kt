package com.example.aibookreader.domain.repository

import com.example.aibookreader.domain.model.ReaderBlock

interface ReaderRepository {

    suspend fun getPageBlocks(
        bookId: Int,
        page: Int
    ): List<ReaderBlock>

    suspend fun getPageHtml(
        bookId: Int,
        page: Int
    ): String?

    suspend fun savePagesHtml(bookId: Int, pages: List<String>)
}