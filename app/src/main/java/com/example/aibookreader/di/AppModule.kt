package com.example.aibookreader.di

import com.example.aibookreader.data.remote.gemini.GeminiApiService
import com.example.aibookreader.data.remote.gemini.GeminiClient
import com.example.aibookreader.data.repository.AiRepositoryImpl
import com.example.aibookreader.data.repository.AuthRepositoryImpl
import com.example.aibookreader.data.repository.BookRepositoryImpl
import com.example.aibookreader.data.repository.FileStorageRepositoryImpl
import com.example.aibookreader.data.repository.ReaderRepositoryImpl
import com.example.aibookreader.domain.repository.AiRepository
import com.example.aibookreader.domain.repository.AuthRepository
import com.example.aibookreader.domain.repository.BookRepository
import com.example.aibookreader.domain.repository.FileStorageRepository
import com.example.aibookreader.domain.repository.ReaderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
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
    abstract fun bindFileStorageRepository(
        fileStorageRepositoryImpl: FileStorageRepositoryImpl
    ): FileStorageRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        aiRepositoryImpl: AiRepositoryImpl
    ): AiRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AiNetworkModule {

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService = GeminiClient.service
}
