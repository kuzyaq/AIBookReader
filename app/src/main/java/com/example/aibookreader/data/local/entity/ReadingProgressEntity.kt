package com.example.aibookreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val currentPage: Int,
    val lastReadAt: Long = System.currentTimeMillis()
)