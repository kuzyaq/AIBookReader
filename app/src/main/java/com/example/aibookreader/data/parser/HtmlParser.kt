package com.example.aibookreader.data.parser

import com.example.aibookreader.domain.model.ReaderBlock
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object HtmlParser {

    fun parse(html: String): List<ReaderBlock> {
        val document = Jsoup.parseBodyFragment(html)
        val blocks = mutableListOf<ReaderBlock>()

        val children = document.body().children()

        // Если это плоский текст без тегов (на всякий случай)
        if (children.isEmpty()) {
            val text = document.body().text().trim()
            if (text.isNotBlank()) {
                blocks.add(ReaderBlock.Paragraph(text))
            }
            return blocks
        }

        // Парсим каждый элемент первого уровня
        for (element in children) {
            parseElement(element)?.let { blocks.add(it) }
        }

        return blocks
    }

    private fun parseElement(element: Element): ReaderBlock? {
        val tagName = element.tagName().lowercase()
        val text = element.text().trim()

        // Пропускаем пустые элементы (если это не картинка)
        if (text.isEmpty() && tagName != "img") {
            return null
        }

        return when (tagName) {
            "p", "div", "span" -> {
                ReaderBlock.Paragraph(text)
            }
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val level = tagName.substring(1).toIntOrNull() ?: 1
                ReaderBlock.Title(text, level)
            }
            "blockquote" -> {
                ReaderBlock.Quote(text)
            }
            "img", "image" -> {
                // В EPUB картинки часто имеют сложный путь внутри архива.
                // Пока мы парсим только имя файла, позже нужно будет доставать их из архива.
                val src = element.attr("src").ifEmpty { element.attr("href") }
                if (src.isNotEmpty()) ReaderBlock.Image(src) else null
            }
            // Обрабатываем списки (ol, ul)
            "ul", "ol" -> {
                // Собираем элементы списка в один параграф (или можно создать отдельный ReaderBlock.List)
                val listText = element.select("li").joinToString("\n") { "• ${it.text()}" }
                if (listText.isNotBlank()) ReaderBlock.Paragraph(listText) else null
            }
            else -> {
                // Если тег неизвестен (например, <b>, <i>), просто берем его текст
                ReaderBlock.Paragraph(text)
            }
        }
    }
}