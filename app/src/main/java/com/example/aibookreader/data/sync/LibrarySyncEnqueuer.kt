package com.example.aibookreader.data.sync

import com.example.aibookreader.data.local.dao.SyncOutboxDao
import com.example.aibookreader.data.local.entity.SyncOutboxEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibrarySyncEnqueuer @Inject constructor(
    private val syncOutboxDao: SyncOutboxDao,
    private val scheduler: LibrarySyncScheduler
) {

    suspend fun enqueueBookUpload(localBookId: Int) {
        syncOutboxDao.deleteByOperationAndBook(SyncOperation.BOOK_UPLOAD, localBookId)
        syncOutboxDao.insert(
            SyncOutboxEntity(operation = SyncOperation.BOOK_UPLOAD, localBookId = localBookId)
        )
        scheduler.requestImmediateSync()
    }

    suspend fun enqueueProgressPush(localBookId: Int) {
        syncOutboxDao.deleteByOperationAndBook(SyncOperation.PROGRESS_PUSH, localBookId)
        syncOutboxDao.insert(
            SyncOutboxEntity(operation = SyncOperation.PROGRESS_PUSH, localBookId = localBookId)
        )
        scheduler.requestImmediateSync()
    }

    suspend fun enqueueChatPush(localBookId: Int) {
        syncOutboxDao.deleteByOperationAndBook(SyncOperation.CHAT_PUSH, localBookId)
        syncOutboxDao.insert(
            SyncOutboxEntity(operation = SyncOperation.CHAT_PUSH, localBookId = localBookId)
        )
        scheduler.requestImmediateSync()
    }
}
