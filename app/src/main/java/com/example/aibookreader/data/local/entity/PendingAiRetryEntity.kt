package com.example.aibookreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_ai_retry")
data class PendingAiRetryEntity(
    @PrimaryKey val bookId: Int,
    val prompt: String,
    val userMessage: String,
    val errorMessage: String,
    val updatedAt: Long = System.currentTimeMillis()
)
