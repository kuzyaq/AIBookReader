package com.example.aibookreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.aibookreader.data.local.entity.ReaderBlockEntity

@Dao
interface ReaderBlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<ReaderBlockEntity>)

    @Query(
        """
        SELECT * FROM reader_blocks
        WHERE bookId = :bookId AND pageIndex = :page
        """
    )
    suspend fun getPage(bookId: Int, page: Int): ReaderBlockEntity?

}