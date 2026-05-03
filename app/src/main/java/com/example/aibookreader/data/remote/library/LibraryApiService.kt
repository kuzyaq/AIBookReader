package com.example.aibookreader.data.remote.library

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LibraryApiService {

    @GET("library/books")
    suspend fun listBooks(): List<LibraryBookDto>

    @POST("library/books/init")
    suspend fun initBook(@Body body: InitLibraryBookRequestDto): InitLibraryBookResponseDto

    @POST("library/books/{id}/upload-url")
    suspend fun presignUpload(@Path("id") id: String): InitLibraryBookResponseDto

    @POST("library/books/{id}/complete")
    suspend fun completeBook(@Path("id") id: String): LibraryBookDto

    @GET("library/books/{id}/download")
    suspend fun downloadInfo(@Path("id") id: String): DownloadResponseDto

    @GET("library/books/{id}/progress")
    suspend fun getProgress(@Path("id") id: String): ProgressResponseDto

    @PUT("library/books/{id}/progress")
    suspend fun putProgress(@Path("id") id: String, @Body body: ProgressPutDto): ProgressResponseDto

    @POST("library/books/{id}/chat/messages")
    suspend fun postChat(@Path("id") id: String, @Body body: ChatBatchRequestDto)

    @GET("library/books/{id}/chat/messages")
    suspend fun getChat(
        @Path("id") id: String,
        @Query("after") after: String?
    ): List<ChatMessageResponseDto>
}
