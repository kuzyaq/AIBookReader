package com.example.aibookreader.domain.repository

import com.example.aibookreader.domain.model.ReaderBlock

data class ChapterInfo(
    val spineIndex: Int,
    val href: String,
    val title: String?
)

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

    suspend fun saveChapters(bookId: Int, chapters: List<ChapterInfo>)

    suspend fun getChapters(bookId: Int): List<ChapterInfo>

    suspend fun getChapter(bookId: Int, spineIndex: Int): ChapterInfo?
}
