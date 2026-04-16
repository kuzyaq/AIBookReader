package com.example.aibookreader.data.local

import androidx.room.TypeConverter
import com.example.aibookreader.domain.model.BookFormat
import com.example.aibookreader.domain.model.BookStatus

class RoomConverters {

    @TypeConverter
    fun bookStatusToString(s: BookStatus): String = s.name

    @TypeConverter
    fun stringToBookStatus(value: String): BookStatus = BookStatus.valueOf(value)

    @TypeConverter
    fun bookFormatToString(f: BookFormat): String = f.name

    @TypeConverter
    fun stringToBookFormat(value: String): BookFormat = BookFormat.valueOf(value)
}
