package com.example.aibookreader.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.aibookreader.data.local.RoomConverters
import com.example.aibookreader.data.local.dao.BookDao
import com.example.aibookreader.data.local.dao.ChapterDao
import com.example.aibookreader.data.local.dao.ChatHistoryDao
import com.example.aibookreader.data.local.dao.ReaderBlockDao
import com.example.aibookreader.data.local.dao.ReadingProgressDao
import com.example.aibookreader.data.local.dao.SyncOutboxDao
import com.example.aibookreader.data.local.entity.BookEntity
import com.example.aibookreader.data.local.entity.ChapterEntity
import com.example.aibookreader.data.local.entity.ChatMessageEntity
import com.example.aibookreader.data.local.entity.PendingAiRetryEntity
import com.example.aibookreader.data.local.entity.ReaderBlockEntity
import com.example.aibookreader.data.local.entity.ReadingProgressEntity
import com.example.aibookreader.data.local.entity.SyncOutboxEntity


@Database(
    entities = [
        BookEntity::class,
        ReadingProgressEntity::class,
        ReaderBlockEntity::class,
        ChatMessageEntity::class,
        ChapterEntity::class,
        PendingAiRetryEntity::class,
        SyncOutboxEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun readerBlockDao(): ReaderBlockDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun chapterDao(): ChapterDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        const val DATABASE_NAME = "book_reader_database"
    }
}
