//package com.example.aibookreader.di
//
//import com.example.aibookreader.domain.repository.BookRepository
//import com.example.aibookreader.domain.usecase.AddBookUseCase
//import com.example.aibookreader.domain.usecase.DeleteBookUseCase
//import com.example.aibookreader.domain.usecase.GetBookByIdUseCase
//import com.example.aibookreader.domain.usecase.GetBooksUseCase
//import com.example.aibookreader.domain.usecase.SearchBooksUseCase
//import com.example.aibookreader.domain.usecase.UpdateBookUseCase
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object ViewModelModule {
//
//    @Provides
//    @Singleton
//    fun provideGetBooksUseCase(
//        repository: BookRepository
//    ): GetBooksUseCase = GetBooksUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideGetBookByIdUseCase(
//        repository: BookRepository
//    ): GetBookByIdUseCase = GetBookByIdUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideAddBookUseCase(
//        repository: BookRepository
//    ): AddBookUseCase = AddBookUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideUpdateBookUseCase(
//        repository: BookRepository
//    ): UpdateBookUseCase = UpdateBookUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideDeleteBookUseCase(
//        repository: BookRepository
//    ): DeleteBookUseCase = DeleteBookUseCase(repository)
//
//    @Provides
//    @Singleton
//    fun provideSearchBooksUseCase(
//        repository: BookRepository
//    ): SearchBooksUseCase = SearchBooksUseCase(repository)
//}
//
