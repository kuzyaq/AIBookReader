package com.example.aibookreader.data.local.dao

import androidx.room.*
import com.example.aibookreader.data.local.entity.ReadingProgressEntity


@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId ORDER BY lastReadAt DESC LIMIT 1")
    suspend fun getLatestProgress(bookId: Int): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progressEntity: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteProgressForBook(bookId: Int)
}