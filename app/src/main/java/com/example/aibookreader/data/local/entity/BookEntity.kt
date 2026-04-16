package com.example.aibookreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aibookreader.domain.model.BookFormat
import com.example.aibookreader.domain.model.BookStatus

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val author: String,
    val filePath: String,

    val coverImage: String? = null,

    val status: BookStatus = BookStatus.IMPORTING,

    /** Для PDF — число страниц; для EPUB — число элементов спайна (глав/файлов потока). */
    val totalPages: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis(),

    val fileSize: Long = 0,
    val extractedDir: String? = null,
    val opfBasePath: String? = null,

    val format: BookFormat = BookFormat.EPUB,

    /** Идентификатор записи на сервере (библиотека / файл), для синхронизации между устройствами. */
    val remoteBookId: String? = null,
    /** Версия метаданных с сервера (конфликты / ETag). */
    val remoteBookVersion: Long? = null
)
