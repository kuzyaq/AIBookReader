package com.example.aibookreader.data.remote.gemini

import com.example.aibookreader.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// ─────────────────────────────────────────────────────────────────
// Модели запроса
// ─────────────────────────────────────────────────────────────────

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

// ─────────────────────────────────────────────────────────────────
// Модели ответа
// ─────────────────────────────────────────────────────────────────

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// ─────────────────────────────────────────────────────────────────
// Retrofit-интерфейс
// ─────────────────────────────────────────────────────────────────

interface GeminiApiService {

    @POST("v1beta/models/gemini-3.1-flash-lite-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String = BuildConfig.GEMINI_API_KEY,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// ─────────────────────────────────────────────────────────────────
// Singleton-клиент
// ─────────────────────────────────────────────────────────────────

object GeminiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }
        .build()

    val service: GeminiApiService = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApiService::class.java)
}

// ─────────────────────────────────────────────────────────────────
// Вспомогательная функция — собрать запрос и достать текст ответа
// ─────────────────────────────────────────────────────────────────

fun buildRequest(prompt: String) = GeminiRequest(
    contents = listOf(
        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
    )
)

fun GeminiResponse.extractText(): String? =
    candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text