package com.example.aibookreader.domain.repository

interface FileStorageRepository {
    suspend fun saveCover(byteArray: ByteArray): String? // Возвращает путь к файлу
    suspend fun copyBookToInternal(uriString: String): String // Возвращает новый путь
}