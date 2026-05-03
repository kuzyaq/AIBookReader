package com.example.aibookreader.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aibookreader.data.local.dao.BookDao
import com.example.aibookreader.data.local.dao.ChatHistoryDao
import com.example.aibookreader.data.local.dao.ReadingProgressDao
import com.example.aibookreader.data.local.dao.SyncOutboxDao
import com.example.aibookreader.data.local.entity.ChatMessageEntity
import com.example.aibookreader.data.local.entity.ReadingProgressEntity
import com.example.aibookreader.data.remote.library.ChatBatchRequestDto
import com.example.aibookreader.data.remote.library.ChatMessageItemDto
import com.example.aibookreader.data.local.entity.BookEntity
import com.example.aibookreader.data.remote.library.InitLibraryBookRequestDto
import com.example.aibookreader.data.remote.library.InitLibraryBookResponseDto
import com.example.aibookreader.data.remote.library.LibraryApiService
import com.example.aibookreader.data.remote.library.ProgressPutDto
import com.example.aibookreader.data.sync.SyncOperation
import com.example.aibookreader.domain.model.BookStatus
import com.example.aibookreader.domain.repository.AuthRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.util.UUID
import javax.inject.Named

@HiltWorker
class LibrarySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val libraryApi: LibraryApiService,
    private val syncOutboxDao: SyncOutboxDao,
    private val bookDao: BookDao,
    private val readingProgressDao: ReadingProgressDao,
    private val chatHistoryDao: ChatHistoryDao,
    private val authRepository: AuthRepository,
    @Named("raw") private val rawHttp: OkHttpClient
) : CoroutineWorker(appContext, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        if (authRepository.currentUser.value == null) {
            return Result.success()
        }

        return try {
            val batch = syncOutboxDao.peek(80)
            for (item in batch) {
                val ok = when (item.operation) {
                    SyncOperation.BOOK_UPLOAD -> uploadBook(item.localBookId)
                    SyncOperation.PROGRESS_PUSH -> pushProgress(item.localBookId)
                    SyncOperation.CHAT_PUSH -> pushChat(item.localBookId)
                    else -> true
                }
                if (ok) {
                    syncOutboxDao.deleteById(item.id)
                } else {
                    return Result.retry()
                }
            }

            pullRemoteForAllLinkedBooks()
            Result.success()
        } catch (_: HttpException) {
            Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun uploadBook(localBookId: Int): Boolean {
        val book = bookDao.getBookById(localBookId) ?: return true
        if (book.remoteBookId != null) return true
        if (book.status != BookStatus.READY) return true

        val init = resolveInitOrPresign(localBookId, book) ?: return false

        val file = File(book.filePath)
        if (!file.exists() || !file.isFile) return false

        val mediaType = init.contentType.toMediaType()
        val body = file.asRequestBody(mediaType)
        val req = Request.Builder()
            .url(init.uploadUrl)
            .put(body)
            .header("Content-Type", init.contentType)
            .build()
        rawHttp.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false
        }

        val done = libraryApi.completeBook(init.id)
        bookDao.updateRemoteMeta(localBookId, done.id, done.metadataVersion)
        return true
    }

    private suspend fun resolveInitOrPresign(
        localBookId: Int,
        book: BookEntity
    ): InitLibraryBookResponseDto? {
        val pending = book.pendingRemoteLibraryBookId
        if (pending != null) {
            try {
                return libraryApi.presignUpload(pending)
            } catch (e: HttpException) {
                if (e.code() == 400 || e.code() == 404) {
                    bookDao.clearPendingRemoteLibraryBookId(localBookId)
                } else {
                    return null
                }
            }
        }
        val created = try {
            libraryApi.initBook(
                InitLibraryBookRequestDto(
                    title = book.title,
                    author = book.author,
                    format = book.format.name,
                    fileSize = book.fileSize
                )
            )
        } catch (_: HttpException) {
            return null
        }
        bookDao.setPendingRemoteLibraryBookId(localBookId, created.id)
        return created
    }

    private suspend fun pushProgress(localBookId: Int): Boolean {
        val book = bookDao.getBookById(localBookId) ?: return true
        val rid = book.remoteBookId ?: return true
        val prog = readingProgressDao.getByBookId(localBookId) ?: return true

        val dto = ProgressPutDto(
            locatorJson = prog.locatorJson,
            currentPageIndex = prog.currentPageIndex,
            progressFraction = prog.progressFraction,
            lastReadAtMs = prog.lastReadAt,
            expectedVersion = prog.remoteProgressVersion
        )
        return try {
            val res = libraryApi.putProgress(rid, dto)
            readingProgressDao.updateRemoteProgressVersion(localBookId, res.version)
            true
        } catch (e: HttpException) {
            if (e.code() == 409) {
                val raw = e.response()?.errorBody()?.string().orEmpty()
                val conflict = runCatching { gson.fromJson(raw, com.example.aibookreader.data.remote.library.ProgressResponseDto::class.java) }.getOrNull()
                if (conflict != null && conflict.version > 0) {
                    readingProgressDao.upsert(
                        ReadingProgressEntity(
                            bookId = localBookId,
                            locatorJson = conflict.locatorJson,
                            currentPageIndex = conflict.currentPageIndex,
                            lastReadAt = conflict.lastReadAtMs,
                            progressFraction = conflict.progressFraction,
                            remoteProgressVersion = conflict.version
                        )
                    )
                }
                true
            } else {
                false
            }
        }
    }

    private suspend fun pushChat(localBookId: Int): Boolean {
        val book = bookDao.getBookById(localBookId) ?: return true
        val rid = book.remoteBookId ?: return true

        var unsynced = chatHistoryDao.getUnsynced(localBookId)
        for (row in unsynced) {
            if (row.clientUuid.isNullOrBlank()) {
                val u = UUID.randomUUID().toString()
                chatHistoryDao.setClientUuid(row.id, u)
            }
        }
        unsynced = chatHistoryDao.getUnsynced(localBookId)
        if (unsynced.isEmpty()) return true

        val batch = ChatBatchRequestDto(
            messages = unsynced.map { r ->
                ChatMessageItemDto(
                    clientMessageId = r.clientUuid!!,
                    role = if (r.isUser) "user" else "assistant",
                    content = r.message,
                    createdAtMs = r.timestamp
                )
            }
        )
        return try {
            libraryApi.postChat(rid, batch)
            chatHistoryDao.markSynced(unsynced.map { it.id })
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun pullRemoteForAllLinkedBooks() {
        val books = bookDao.getBooksWithRemote()
        for (book in books) {
            val rid = book.remoteBookId ?: continue
            try {
                val serverProg = libraryApi.getProgress(rid)
                val localProg = readingProgressDao.getByBookId(book.id)
                val localVer = localProg?.remoteProgressVersion ?: 0L
                if (serverProg.version > localVer && serverProg.version > 0L) {
                    readingProgressDao.upsert(
                        ReadingProgressEntity(
                            bookId = book.id,
                            locatorJson = serverProg.locatorJson,
                            currentPageIndex = serverProg.currentPageIndex,
                            lastReadAt = serverProg.lastReadAtMs,
                            progressFraction = serverProg.progressFraction,
                            remoteProgressVersion = serverProg.version
                        )
                    )
                }

                val after = book.lastRemoteChatSyncAt
                val messages = libraryApi.getChat(rid, after)
                var maxServerAt: String? = book.lastRemoteChatSyncAt
                for (m in messages) {
                    if (chatHistoryDao.findByClientUuid(book.id, m.clientMessageId) != null) continue
                    chatHistoryDao.insertMessage(
                        ChatMessageEntity(
                            bookId = book.id,
                            message = m.content,
                            isUser = m.role == "user",
                            timestamp = m.createdAtMs,
                            clientUuid = m.clientMessageId,
                            synced = 1
                        )
                    )
                    if (maxServerAt == null || m.serverCreatedAt > maxServerAt) {
                        maxServerAt = m.serverCreatedAt
                    }
                }
                if (messages.isNotEmpty() && maxServerAt != null) {
                    bookDao.updateLastRemoteChatSyncAt(book.id, maxServerAt)
                }
            } catch (_: Exception) {
            }
        }
    }
}
