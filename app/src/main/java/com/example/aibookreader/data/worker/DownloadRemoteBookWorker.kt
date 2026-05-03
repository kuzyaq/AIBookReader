package com.example.aibookreader.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.aibookreader.data.remote.library.LibraryApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Named

@HiltWorker
class DownloadRemoteBookWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val libraryApi: LibraryApiService,
    private val importBookScheduler: ImportBookScheduler,
    @Named("raw") private val rawHttp: OkHttpClient
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val remoteId = inputData.getString(KEY_REMOTE_ID) ?: return Result.failure()
        val ext = inputData.getString(KEY_EXT)?.trim('.')?.lowercase() ?: "epub"
        return try {
            val info = libraryApi.downloadInfo(remoteId)
            val out = File(applicationContext.cacheDir, "remote_${remoteId}.$ext")
            val req = Request.Builder().url(info.url).get().build()
            rawHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return Result.retry()
                resp.body?.byteStream()?.use { input ->
                    out.outputStream().use { input.copyTo(it) }
                } ?: return Result.retry()
            }
            importBookScheduler.importBook(out.absolutePath, remoteId)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_REMOTE_ID = "remote_id"
        const val KEY_EXT = "ext"
    }
}
