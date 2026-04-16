package com.example.aibookreader.domain.model

data class Book(

    val id: Int,

    val title: String,

    val author: String,

    val coverImage: String?,

    /** Для PDF — страница; для EPUB WebView-ридера — индекс главы; для Readium смотри locator + progressFraction. */
    val currentPage: Int,

    val totalPages: Int,

    val createdAt: Long,
    val lastReadAt: Long,

    val fileSize: Long,
    val filePath: String,

    val status: BookStatus,

    val extractedDir: String? = null,
    val opfBasePath: String? = null,
    val locator: String? = null,

    val format: BookFormat = BookFormat.EPUB,

    /** Кэш доли прочитанного 0..1 (список, синхронизация). */
    val progressFraction: Float? = null,

    val remoteBookId: String? = null,
    val remoteBookVersion: Long? = null,
    val remoteProgressVersion: Long? = null
) {
    fun hasCover(): Boolean = coverImage != null

    fun getProgressPercentage(): Float {
        progressFraction?.let { return (it * 100f).coerceIn(0f, 100f) }
        locator?.let { json ->
            try {
                val loc = org.json.JSONObject(json)
                val locations = loc.optJSONObject("locations")
                val totalProgression = locations?.optDouble("totalProgression")
                if (totalProgression != null && !totalProgression.isNaN()) {
                    return (totalProgression * 100).toFloat().coerceIn(0f, 100f)
                }
            } catch (_: Exception) { }
        }
        return if (totalPages > 0) {
            (((currentPage + 1).toFloat() / totalPages) * 100f).coerceIn(0f, 100f)
        } else 0f
    }

    fun isFinished(): Boolean {
        progressFraction?.let { return it >= 0.95f }
        locator?.let { json ->
            try {
                val loc = org.json.JSONObject(json)
                val locations = loc.optJSONObject("locations")
                val totalProgression = locations?.optDouble("totalProgression")
                if (totalProgression != null && !totalProgression.isNaN()) {
                    return totalProgression >= 0.95
                }
            } catch (_: Exception) { }
        }
        return currentPage >= totalPages - 1 && totalPages > 0
    }
}

data class EpubChapter(
    val index: Int,
    val title: String?,
    val content: String,
    val id: String
)
