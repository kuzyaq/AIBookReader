package com.example.aibookreader.domain.model

/**
 * Прогресс чтения (соответствует серверной сущности для синхронизации между устройствами).
 */
data class ReadingProgress(
    val bookId: Int,
    val locatorJson: String? = null,
    val currentPageIndex: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis(),
    val progressFraction: Float? = null,
    val remoteProgressVersion: Long? = null
)
