package com.example.aibookreader.data.epub

import com.example.aibookreader.domain.model.ParsedEpubData
import com.example.aibookreader.domain.service.EpubParser
import com.github.mertakdut.Reader
import com.github.mertakdut.exception.OutOfPagesException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject

class EpubParserImpl @Inject constructor() : EpubParser {

    // Оптимальный размер "страницы" (куска HTML), чтобы не перегружать Compose
    // Это не физическая страница экрана, а блок для скролла в HorizontalPager
    private val CHARS_PER_PAGE = 3000

    override suspend fun parse(filePath: String): ParsedEpubData =
        withContext(Dispatchers.IO) {
            val reader = Reader()
            // Увеличиваем лимит, чтобы грузить главу целиком
            reader.setMaxContentPerSection(100000)
            // ВАЖНО: Нам нужен HTML, а не просто текст
            reader.setIsIncludingTextContent(false)
            reader.setFullContent(filePath)

            val info = reader.infoPackage
            val title = info.metadata.title ?: "Unknown Title"
            val author = info.metadata.creator ?: "Unknown Author"
            val coverBytes = try { reader.coverImage } catch (e: Exception) { null }

            val pages = mutableListOf<String>()
            val fullText = StringBuilder()
            var index = 0

            while (true) {
                try {
                    val section = reader.readSection(index)

                    // 1. Берем сырой HTML (section.sectionContent)
                    val rawHtml = section.sectionContent ?: ""

                    // 2. Очищаем от стилей, скриптов и хедера (оставляем только <body>)
                    val cleanBodyHtml = cleanHtml(rawHtml)

                    // 3. Извлекаем голый текст для AI-анализа (fullText)
                    val sectionText = Jsoup.parse(cleanBodyHtml).text()
                    if (sectionText.isNotBlank()) {
                        fullText.append(sectionText).append("\n")
                    }

                    // 4. Разбиваем длинную главу на более мелкие "страницы" (chunks)
                    // стараясь не разрывать теги
                    val chunks = splitHtmlIntoPages(cleanBodyHtml)
                    pages.addAll(chunks)

                    index++
                } catch (e: OutOfPagesException) {
                    break
                }
            }

            ParsedEpubData(
                title = title,
                author = author,
                coverImageBytes = coverBytes,
                pages = pages, // Теперь тут лежат куски HTML
                fullText = fullText.toString() // Тут плоский текст для поиска/AI
            )
        }

    /**
     * Очищает HTML от ненужных тегов, оставляя только семантическую структуру
     */
    private fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        val doc = Jsoup.parse(html)
        // Удаляем стили, скрипты, SVG и скрытые элементы
        doc.select("style, script, svg, head").remove()
        return doc.body().html()
    }

    /**
     * Разбивает длинный HTML на куски (страницы) по верхнеуровневым тегам.
     * Не разрывает абзацы пополам.
     */
    private fun splitHtmlIntoPages(html: String): List<String> {
        if (html.isBlank()) return emptyList()

        val doc = Jsoup.parseBodyFragment(html)
        val children = doc.body().children()

        if (children.isEmpty()) return listOf(html)

        val pages = mutableListOf<String>()
        var currentPageHtml = StringBuilder()
        var currentLength = 0

        for (element in children) {
            val elementHtml = element.outerHtml()
            val elementTextLength = element.text().length

            // Если добавление этого элемента превысит лимит, и текущая страница не пуста
            if (currentLength + elementTextLength > CHARS_PER_PAGE && currentLength > 0) {
                // Сохраняем текущую страницу
                pages.add(currentPageHtml.toString())
                // Начинаем новую
                currentPageHtml = StringBuilder()
                currentLength = 0
            }

            currentPageHtml.append(elementHtml).append("\n")
            currentLength += elementTextLength
        }

        // Добавляем остаток
        if (currentPageHtml.isNotEmpty()) {
            pages.add(currentPageHtml.toString())
        }

        return pages
    }
}