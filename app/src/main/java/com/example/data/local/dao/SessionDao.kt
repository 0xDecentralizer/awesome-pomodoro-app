package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DayTotal
import com.example.data.local.entity.HourlyTotal
import com.example.data.local.entity.LabelTotal
import com.example.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startEpochMs DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE epochDay BETWEEN :startDay AND :endDay ORDER BY startEpochMs DESC")
    fun getSessionsInRange(startDay: Long, endDay: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE epochDay = :epochDay ORDER BY startEpochMs DESC")
    fun getSessionsForDay(epochDay: Long): Flow<List<SessionEntity>>

    @Query("SELECT epochDay, SUM(durationMinutes) as totalMinutes FROM sessions WHERE epochDay BETWEEN :startDay AND :endDay GROUP BY epochDay")
    fun getDailyTotals(startDay: Long, endDay: Long): Flow<List<DayTotal>>

    @Query("SELECT labelId, SUM(durationMinutes) as totalMinutes FROM sessions GROUP BY labelId")
    fun getTotalMinutesPerLabel(): Flow<List<LabelTotal>>

    @Query("SELECT CAST(strftime('%H', datetime(startEpochMs / 1000, 'unixepoch', 'localtime')) AS INTEGER) as hour, SUM(durationMinutes) as totalMinutes FROM sessions WHERE epochDay = :epochDay GROUP BY hour")
    fun getHourlyDistribution(epochDay: Long): Flow<List<HourlyTotal>>

    @Query("SELECT DISTINCT epochDay FROM sessions ORDER BY epochDay DESC")
    fun getAllWorkedEpochDays(): Flow<List<Long>>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM sessions")
    fun getTotalMinutes(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT epochDay) FROM sessions")
    fun getTotalWorkedDays(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}
