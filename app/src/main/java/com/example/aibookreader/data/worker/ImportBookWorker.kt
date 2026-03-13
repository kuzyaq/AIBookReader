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

@HiltWorker
class ImportBookWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,

    private val repository: BookRepository,
    private val importEpubBookUseCase: ImportEpubBookUseCase,
    private val importPdfBookUseCase: ImportPdfBookUseCase

) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        android.util.Log.d("ImportBookWorker", "WORKER STARTED")

        val filePath = inputData.getString(KEY_FILE_PATH)
            ?: return Result.failure()


        return try {

            repository.createImportPlaceholder(
                filePath,
                0
            )

            val isEpub = filePath.contains(".epub", ignoreCase = true) ||
                    applicationContext.contentResolver.getType(android.net.Uri.parse(filePath)) == "application/epub+zip"

            val useCaseResult = when {
                isEpub -> importEpubBookUseCase(filePath)

                filePath.endsWith(".epub", true) ->
                    importEpubBookUseCase(filePath)

                filePath.endsWith(".pdf", true) ->
                    importPdfBookUseCase(filePath, applicationContext)

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
                    android.util.Log.d("ImportBookWorker", "SUCCESS")
                    Result.success()
                },
                onFailure = { exception ->
                    android.util.Log.e("ImportBookWorker", "UseCase failed: ${exception.message}", exception)
                    repository.markImportFailed(filePath)

                    Result.failure(
                        Data.Builder()
                            .putString("error", exception.message)
                            .build()
                    )
                }
            )

        } catch (e: Exception) {
            android.util.Log.e("ImportBookWorker", "Import failed", e)
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
    }
}