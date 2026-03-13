package com.example.aibookreader.data.repository

import android.content.Context
import android.net.Uri
import com.example.aibookreader.domain.repository.FileStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import androidx.core.net.toUri
import java.io.FileInputStream


class FileStorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileStorageRepository{
    override suspend fun saveCover(byteArray: ByteArray): String? {
        try {
            val fileName = "cover_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            file.outputStream().use { it.write(byteArray) }

            return file.absolutePath
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun copyBookToInternal(uriString: String): String {
        val sourceUri = uriString.toUri()

        val fileName = "book_${System.currentTimeMillis()}.epub"
        val destFile = File(context.filesDir, fileName)


        val inputStream = if (uriString.startsWith("/")) {
            // Если это локальный путь (из кэша)
            FileInputStream(File(uriString))
        } else {
            // Если это всё еще content:// URI
            context.contentResolver.openInputStream(sourceUri)
        }

        inputStream?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Не удалось открыть файл")

        return destFile.absolutePath
    }
}