package com.example.aibookreader.data.worker

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRemoteBookScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun enqueue(remoteId: String, ext: String) {
        val data = Data.Builder()
            .putString(DownloadRemoteBookWorker.KEY_REMOTE_ID, remoteId)
            .putString(DownloadRemoteBookWorker.KEY_EXT, ext)
            .build()
        val req = OneTimeWorkRequestBuilder<DownloadRemoteBookWorker>()
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork(
            "download_remote_$remoteId",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }
}
