package com.example.aibookreader.di

import com.example.aibookreader.data.epub.EpubParserImpl
import com.example.aibookreader.data.repository.BookRepositoryImpl
import com.example.aibookreader.data.repository.FileStorageRepositoryImpl
import com.example.aibookreader.data.repository.ReaderRepositoryImpl
import com.example.aibookreader.domain.repository.BookRepository
import com.example.aibookreader.domain.repository.FileStorageRepository
import com.example.aibookreader.domain.repository.ReaderRepository
import com.example.aibookreader.domain.service.EpubParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(
        repositoryImpl: BookRepositoryImpl
    ): BookRepository

    @Binds
    @Singleton
    abstract fun bindReaderRepository(
        repositoryImpl: ReaderRepositoryImpl
    ): ReaderRepository

    @Binds
    @Singleton
    abstract fun bindEpubParser(
        epubParserImpl: EpubParserImpl
    ): EpubParser

    @Binds
    @Singleton
    abstract fun bindFileStorageRepository(
        fileStorageRepositoryImpl: FileStorageRepositoryImpl
    ): FileStorageRepository
}