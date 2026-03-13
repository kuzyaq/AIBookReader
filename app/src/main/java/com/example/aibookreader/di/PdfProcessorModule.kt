package com.example.aibookreader.di

import com.example.aibookreader.data.pdf.PdfProcessor
import com.example.aibookreader.data.pdf.PdfProcessorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfProcessorModule {

    @Binds
    @Singleton
    abstract fun bindPdfProcessor(impl: PdfProcessorImpl): PdfProcessor
}