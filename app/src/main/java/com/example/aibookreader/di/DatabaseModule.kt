package com.example.aibookreader.di

import android.content.Context
import androidx.room.Room
import com.example.aibookreader.data.local.dao.BookDao
import com.example.aibookreader.data.local.dao.ReaderBlockDao
import com.example.aibookreader.data.local.dao.ReadingProgressDao
import com.example.aibookreader.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideBookDao(database: AppDatabase): BookDao {
        return database.bookDao()
    }

    @Provides
    @Singleton
    fun provideReaderBlockDao(database: AppDatabase): ReaderBlockDao {
        return database.readerBlockDao()
    }

    @Provides
    @Singleton
    fun provideReadingProgressDao(database: AppDatabase): ReadingProgressDao{
        return database.readingProgressDao()
    }
}