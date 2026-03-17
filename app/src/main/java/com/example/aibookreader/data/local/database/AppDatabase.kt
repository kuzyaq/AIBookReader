package com.example.aibookreader.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aibookreader.data.local.dao.BookDao
import com.example.aibookreader.data.local.dao.ChatHistoryDao
import com.example.aibookreader.data.local.dao.ReaderBlockDao
import com.example.aibookreader.data.local.dao.ReadingProgressDao
import com.example.aibookreader.data.local.entity.BookEntity
import com.example.aibookreader.data.local.entity.ChatMessageEntity
import com.example.aibookreader.data.local.entity.ReaderBlockEntity
import com.example.aibookreader.data.local.entity.ReadingProgressEntity


@Database(
    entities = [BookEntity::class, ReadingProgressEntity::class, ReaderBlockEntity::class, ChatMessageEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun readerBlockDao(): ReaderBlockDao
    abstract fun readingProgressDao(): ReadingProgressDao

    abstract fun chatHistoryDao(): ChatHistoryDao


    companion object{
        const val DATABASE_NAME = "book_reader_database"
    }
}