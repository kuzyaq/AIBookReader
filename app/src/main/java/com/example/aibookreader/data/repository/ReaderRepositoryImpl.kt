package com.example.aibookreader.data.repository

import com.example.aibookreader.data.local.dao.ChapterDao
import com.example.aibookreader.data.local.dao.ReaderBlockDao
import com.example.aibookreader.data.local.entity.ChapterEntity
import com.example.aibookreader.data.local.entity.ReaderBlockEntity
import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.domain.repository.ChapterInfo
import com.example.aibookreader.domain.repository.ReaderRepository
import com.example.aibookreader.data.parser.HtmlParser
import javax.inject.Inject

class ReaderRepositoryImpl @Inject constructor(
    private val dao: ReaderBlockDao,
    private val chapterDao: ChapterDao
) : ReaderRepository {

    override suspend fun getPageBlocks(
        bookId: Int,
        page: Int
    ): List<ReaderBlock> {
        val entity = dao.getPage(bookId, page) ?: return emptyList()
        return HtmlParser.parse(entity.html)
    }

    override suspend fun getPageHtml(
        bookId: Int,
        page: Int
    ): String? {
        val entity = dao.getPage(bookId = bookId, page = page)
        return entity?.html
    }

    override suspend fun savePagesHtml(
        bookId: Int,
        pages: List<String>
    ) {
        val blocks = pages.mapIndexed { index, html ->
            ReaderBlockEntity(
                bookId = bookId,
                pageIndex = index,
                html = html
            )
        }
        dao.insertBlocks(blocks)
    }

    override suspend fun saveChapters(bookId: Int, chapters: List<ChapterInfo>) {
        chapterDao.deleteChapters(bookId)
        chapterDao.insertChapters(
            chapters.map { ch ->
                ChapterEntity(
                    bookId = bookId,
                    spineIndex = ch.spineIndex,
                    href = ch.href,
                    title = ch.title
                )
            }
        )
    }

    override suspend fun getChapters(bookId: Int): List<ChapterInfo> {
        return chapterDao.getChapters(bookId).map { entity ->
            ChapterInfo(
                spineIndex = entity.spineIndex,
                href = entity.href,
                title = entity.title
            )
        }
    }

    override suspend fun getChapter(bookId: Int, spineIndex: Int): ChapterInfo? {
        return chapterDao.getChapter(bookId, spineIndex)?.let { entity ->
            ChapterInfo(
                spineIndex = entity.spineIndex,
                href = entity.href,
                title = entity.title
            )
        }
    }
}
