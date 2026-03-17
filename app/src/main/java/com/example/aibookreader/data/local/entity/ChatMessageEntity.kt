package com.example.aibookreader.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "chat_history",
    indices = [Index("bookId")]
)
data class ChatMessageEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)