package com.example.aibookreader.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class ReadingProgressEntity(
    @PrimaryKey
    val bookId: Int,

    /** Readium Locator JSON; для EPUB — основной источник позиции. */
    val locatorJson: String? = null,

    /** Для PDF и legacy-ридера — индекс страницы (0-based). */
    val currentPageIndex: Int = 0,

    val lastReadAt: Long = System.currentTimeMillis(),

    /** Кэш 0..1 для списка книг и синхронизации без парсинга JSON. */
    val progressFraction: Float? = null,

    /** Версия прогресса на сервере (синхронизация). */
    val remoteProgressVersion: Long? = null
)
