package com.example.aibookreader.data.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import javax.inject.Inject

class ImportBookScheduler @Inject constructor(
    private val workManager: WorkManager
){

    fun importBook(filePath: String) {

        val data = workDataOf(
            ImportBookWorker.KEY_FILE_PATH to filePath
        )

        val request = OneTimeWorkRequestBuilder<ImportBookWorker>()
            .setInputData(data)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "import_$filePath",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}