package com.example.aibookreader.domain.model

import com.example.aibookreader.data.local.entity.BookStatus

data class Book(

    val id: Int,

    val title: String,

    val author: String,

    val coverImage: String?,

    val currentPage: Int,
    val totalPages: Int,

    val createdAt: Long,
    val lastReadAt: Long,

    val fileSize: Long,
    val filePath: String,

    val fullText: String?,

    val status: BookStatus
){
    fun hasCover(): Boolean = coverImage != null

    fun getProgressPercentage(): Float {
        return if (totalPages > 0) {
            ( ( (currentPage + 1).toFloat() / totalPages ) * 100 )
        } else 0f
    }

    fun isFinished(): Boolean = currentPage >= totalPages - 1 && totalPages > 0
}

data class EpubChapter(
    val index: Int,
    val title: String?,
    val content: String, // HTML или Markdown
    val id: String
)