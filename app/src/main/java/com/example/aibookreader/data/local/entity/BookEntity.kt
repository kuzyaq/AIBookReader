package com.example.aibookreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val author: String,
    val filePath: String,

    val coverImage: String? = null,

    val status: BookStatus = BookStatus.IMPORTING,

    val currentPage: Int = 0,
    val totalPages: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis(),

    val fileSize: Long = 0,
    val fullText: String? = null
)

enum class BookStatus {
    IMPORTING,
    READY,
    FAILED
}