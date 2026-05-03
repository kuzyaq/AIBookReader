package com.example.aibookreader.data.sync

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aibookreader.data.worker.LibrarySyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibrarySyncScheduler @Inject constructor(
    private val workManager: WorkManager
) {

    fun schedulePeriodic() {
        val periodic = PeriodicWorkRequestBuilder<LibrarySyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            "library_sync_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
    }

    fun requestImmediateSync() {
        val req = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            "library_sync_immediate",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }
}
