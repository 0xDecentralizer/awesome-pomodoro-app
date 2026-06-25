package com.example.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.dao.LabelDao
import com.example.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "focuslog_database"
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    val now = System.currentTimeMillis()
                    db.execSQL("INSERT INTO labels (name, emoji, colorHex, isPredefined, sortOrder, createdAt) VALUES ('Study', '📚', '#3B82F6', 1, 1, $now)")
                    db.execSQL("INSERT INTO labels (name, emoji, colorHex, isPredefined, sortOrder, createdAt) VALUES ('Work', '💻', '#6366F1', 1, 2, $now)")
                    db.execSQL("INSERT INTO labels (name, emoji, colorHex, isPredefined, sortOrder, createdAt) VALUES ('Exercise', '🏋️', '#F97316', 1, 3, $now)")
                    db.execSQL("INSERT INTO labels (name, emoji, colorHex, isPredefined, sortOrder, createdAt) VALUES ('Reading', '📖', '#10B981', 1, 4, $now)")
                    db.execSQL("INSERT INTO labels (name, emoji, colorHex, isPredefined, sortOrder, createdAt) VALUES ('Creative', '🎨', '#EC4899', 1, 5, $now)")
                    db.execSQL("INSERT INTO labels (name, emoji, colorHex, isPredefined, sortOrder, createdAt) VALUES ('Mindful', '🧘', '#14B8A6', 1, 6, $now)")
                }
            }
        }).build()
    }

    @Provides
    fun provideLabelDao(database: AppDatabase): LabelDao {
        return database.labelDao()
    }

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }
}
