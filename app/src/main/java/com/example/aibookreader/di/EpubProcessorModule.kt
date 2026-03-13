//package com.example.aibookreader.di
//
//import com.example.aibookreader.data.epub.EpubParser
//import com.example.aibookreader.data.epub.EpubProcessorImpl
//import dagger.Binds
//import dagger.Module
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//
//@Module
//@InstallIn(SingletonComponent::class)
//abstract class EpubParserModule {
//
//    @Binds
//    @Singleton
//    abstract fun bindEpubParser(impl: EpubParserImpl) : EpubParser
//}