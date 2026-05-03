package com.example.aibookreader.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_outbox",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["localBookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("localBookId"), Index(value = ["createdAt"])]
)
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** BOOK_UPLOAD, PROGRESS_PUSH, CHAT_PUSH */
    val operation: String,
    val localBookId: Int,
    val payload: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
