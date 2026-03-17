package com.example.aibookreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.example.aibookreader.data.local.entity.ChatMessageEntity
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
}


