package com.example.aibookreader.data.repository

import com.example.aibookreader.data.local.dao.ReaderBlockDao
import com.example.aibookreader.data.local.entity.ReaderBlockEntity
import com.example.aibookreader.domain.model.ReaderBlock
import com.example.aibookreader.domain.repository.ReaderRepository
import com.example.aibookreader.data.parser.HtmlParser
import javax.inject.Inject

class ReaderRepositoryImpl @Inject constructor(
    private val dao: ReaderBlockDao
) : ReaderRepository {

    override suspend fun getPageBlocks(
        bookId: Int,
        page: Int
    ): List<ReaderBlock> {
        val exists = dao.getPage(bookId, page)
        android.util.Log.d("ReaderRepo", "Блок для $bookId, $page найден? ${exists != null}")

        val entity = dao.getPage(bookId, page)
            ?: return emptyList()

        val parsedBlocks = HtmlParser.parse(entity.html)
        android.util.Log.d("ReaderRepo", "Разбор HTML: длина=${entity.html.length}, блоков найдено=${parsedBlocks.size}")

        return parsedBlocks
    }

    override suspend fun getPageHtml(
        bookId: Int,
        page: Int
    ): String? {

        val entity = dao.getPage(
            bookId = bookId,
            page = page
        )

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

}