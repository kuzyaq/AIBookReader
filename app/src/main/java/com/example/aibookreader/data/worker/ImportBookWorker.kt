package com.example.aibookreader.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.aibookreader.domain.repository.BookRepository
import com.example.aibookreader.domain.usecase.ImportEpubBookUseCase
import com.example.aibookreader.domain.usecase.ImportPdfBookUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class ImportBookWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,

    private val repository: BookRepository,
    private val importEpubBookUseCase: ImportEpubBookUseCase,
    private val importPdfBookUseCase: ImportPdfBookUseCase

) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH)
            ?: return Result.failure()


        return try {

            val fileSize = File(filePath).takeIf { it.exists() }?.length() ?: 0L
            repository.createImportPlaceholder(
                filePath,
                fileSize
            )

            val isEpub = filePath.endsWith(".epub", ignoreCase = true)
            val isPdf = filePath.endsWith(".pdf", ignoreCase = true)
            val remoteBookId = inputData.getString(KEY_REMOTE_BOOK_ID)

            val useCaseResult = when {
                isEpub -> importEpubBookUseCase(filePath, remoteBookId)

                isPdf -> importPdfBookUseCase(filePath, applicationContext, remoteBookId)

                else -> {
                    repository.markImportFailed(filePath)

                    return Result.failure(
                        Data.Builder()
                            .putString("error", "Unsupported format")
                            .build()
                    )
                }
            }

            useCaseResult.fold(
                onSuccess = {
                    Result.success()
                },
                onFailure = { exception ->
                    repository.markImportFailed(filePath)

                    Result.failure(
                        Data.Builder()
                            .putString("error", exception.message)
                            .build()
                    )
                }
            )

        } catch (e: Exception) {
            repository.markImportFailed(filePath)

            Result.failure(
                Data.Builder()
                    .putString("error", e.message)
                    .build()
            )
        }
    }

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_REMOTE_BOOK_ID = "remote_book_id"
    }
}