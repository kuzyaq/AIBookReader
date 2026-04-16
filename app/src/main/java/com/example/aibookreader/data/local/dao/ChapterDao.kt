package com.example.aibookreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.aibookreader.data.local.entity.ChapterEntity

@Dao
interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY spineIndex ASC")
    suspend fun getChapters(bookId: Int): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND spineIndex = :spineIndex")
    suspend fun getChapter(bookId: Int, spineIndex: Int): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId")
    suspend fun getChapterCount(bookId: Int): Int

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChapters(bookId: Int)
}
