package com.example.aibookreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reader_blocks")
data class ReaderBlockEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val bookId: Int,

    val pageIndex: Int,

    val html: String
)