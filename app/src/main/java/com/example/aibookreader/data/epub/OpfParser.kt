package com.example.aibookreader.data.epub

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File

data class EpubManifest(
    val title: String,
    val author: String,
    val coverPath: String?,
    val spineItems: List<SpineItem>,
    val basePath: File
)

data class SpineItem(
    val id: String,
    val href: String,
    val mediaType: String = "application/xhtml+xml"
)

object OpfParser {

    fun parse(extractedDir: File): EpubManifest {
        val opfRelPath = findOpfPath(extractedDir)
        val opfFile = File(extractedDir, opfRelPath)
        val basePath = opfFile.parentFile ?: extractedDir

        val doc = Jsoup.parse(opfFile, "UTF-8", "", Parser.xmlParser())

        val title = doc.select("metadata title, metadata dc\\:title").text().ifBlank { "Без названия" }
        val author = doc.select("metadata creator, metadata dc\\:creator").text().ifBlank { "Неизвестный автор" }

        val manifestItems = mutableMapOf<String, Pair<String, String>>()
        doc.select("manifest item").forEach { item ->
            val id = item.attr("id")
            val href = item.attr("href")
            val mediaType = item.attr("media-type")
            manifestItems[id] = href to mediaType
        }

        val spineItems = doc.select("spine itemref").mapNotNull { itemref ->
            val idref = itemref.attr("idref")
            val (href, mediaType) = manifestItems[idref] ?: return@mapNotNull null
            SpineItem(id = idref, href = href, mediaType = mediaType)
        }

        val coverPath = findCoverPath(doc, manifestItems, basePath)

        return EpubManifest(
            title = title,
            author = author,
            coverPath = coverPath,
            spineItems = spineItems,
            basePath = basePath
        )
    }

    private fun findOpfPath(extractedDir: File): String {
        val containerFile = File(extractedDir, "META-INF/container.xml")
        if (!containerFile.exists()) {
            return findOpfFallback(extractedDir)
        }

        val doc = Jsoup.parse(containerFile, "UTF-8", "", Parser.xmlParser())
        val rootfile = doc.select("rootfile").firstOrNull()
        return rootfile?.attr("full-path") ?: findOpfFallback(extractedDir)
    }

    private fun findOpfFallback(dir: File): String {
        val opf = dir.walkTopDown().find { it.extension.equals("opf", ignoreCase = true) }
        return opf?.relativeTo(dir)?.path
            ?: throw IllegalStateException("OPF file not found in EPUB")
    }

    private fun findCoverPath(
        doc: org.jsoup.nodes.Document,
        manifestItems: Map<String, Pair<String, String>>,
        basePath: File
    ): String? {
        val coverMeta = doc.select("metadata meta[name=cover]").attr("content")
        if (coverMeta.isNotBlank()) {
            val (href, _) = manifestItems[coverMeta] ?: return null
            val coverFile = File(basePath, href)
            if (coverFile.exists()) return coverFile.absolutePath
        }

        val coverItem = manifestItems.entries.find { (id, pair) ->
            val (_, mediaType) = pair
            (id.contains("cover", ignoreCase = true) && mediaType.startsWith("image/"))
        }
        if (coverItem != null) {
            val coverFile = File(basePath, coverItem.value.first)
            if (coverFile.exists()) return coverFile.absolutePath
        }

        return null
    }
}
