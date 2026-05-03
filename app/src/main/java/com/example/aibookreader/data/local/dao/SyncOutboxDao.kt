package com.example.aibookreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.aibookreader.data.local.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {

    @Query("SELECT * FROM sync_outbox ORDER BY id ASC LIMIT :limit")
    suspend fun peek(limit: Int = 50): List<SyncOutboxEntity>

    @Insert
    suspend fun insert(entity: SyncOutboxEntity): Long

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_outbox WHERE operation = :op AND localBookId = :bookId")
    suspend fun deleteByOperationAndBook(op: String, bookId: Int)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun count(): Int
}
