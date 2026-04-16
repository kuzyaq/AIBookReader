package com.example.aibookreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.example.aibookreader.data.local.entity.ChatMessageEntity
import com.example.aibookreader.data.local.entity.PendingAiRetryEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun getChatHistory(bookId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_history WHERE bookId = :bookId")
    suspend fun clearChatHistory(bookId: Int)

    @Query("SELECT COUNT(*) FROM chat_history WHERE bookId = :bookId")
    suspend fun getMessageCount(bookId: Int): Int

    @Insert(onConflict = REPLACE)
    suspend fun upsertPendingRetry(entity: PendingAiRetryEntity)

    @Query("SELECT * FROM pending_ai_retry WHERE bookId = :bookId LIMIT 1")
    fun observePendingRetry(bookId: Int): Flow<PendingAiRetryEntity?>

    @Query("DELETE FROM pending_ai_retry WHERE bookId = :bookId")
    suspend fun clearPendingRetry(bookId: Int)
}


