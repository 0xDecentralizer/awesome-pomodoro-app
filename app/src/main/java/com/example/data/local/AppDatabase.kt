package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.LabelDao
import com.example.data.local.dao.SessionDao
import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.SessionEntity

@Database(entities = [LabelEntity::class, SessionEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun labelDao(): LabelDao
    abstract fun sessionDao(): SessionDao
}
