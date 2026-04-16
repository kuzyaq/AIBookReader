package com.example.aibookreader.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.util.asset.AssetRetriever
import javax.inject.Singleton

/**
 * Readium Kotlin Toolkit: общие зависимости для разбора EPUB и открытия [Publication].
 * Цепочка: HTTP → загрузка [AssetRetriever] → [DefaultPublicationParser] → [PublicationOpener].
 */
@Module
@InstallIn(SingletonComponent::class)
object ReadiumModule {

    /** Клиент для удалённых ресурсов внутри публикации (шрифты, внешние ссылки). */
    @Provides
    @Singleton
    fun provideHttpClient(): DefaultHttpClient = DefaultHttpClient()

    /** Читает файл/URI как Readium Asset (локальный EPUB на диске — через contentResolver + URL). */
    @Provides
    @Singleton
    fun provideAssetRetriever(
        @ApplicationContext context: Context,
        httpClient: DefaultHttpClient
    ): AssetRetriever = AssetRetriever(context.contentResolver, httpClient)

    /** Парсер контейнера EPUB (OPF, манифест, шифрование и т.д.) в модель публикации. */
    @Provides
    @Singleton
    fun providePublicationParser(
        @ApplicationContext context: Context,
        httpClient: DefaultHttpClient,
        assetRetriever: AssetRetriever
    ): DefaultPublicationParser = DefaultPublicationParser(
        context, httpClient, assetRetriever, pdfFactory = null
    )

    /** Высокоуровневая точка входа: asset → готовая [org.readium.r2.shared.publication.Publication]. */
    @Provides
    @Singleton
    fun providePublicationOpener(
        publicationParser: DefaultPublicationParser
    ): PublicationOpener = PublicationOpener(publicationParser)
}
