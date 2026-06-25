package com.example.domain.repository

import com.example.domain.model.DayTotal
import com.example.domain.model.HourlyTotal
import com.example.domain.model.LabelTotal
import com.example.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>
    fun getSessionsInRange(startDay: Long, endDay: Long): Flow<List<Session>>
    fun getSessionsForDay(epochDay: Long): Flow<List<Session>>
    fun getDailyTotals(startDay: Long, endDay: Long): Flow<List<DayTotal>>
    fun getTotalMinutesPerLabel(): Flow<List<LabelTotal>>
    fun getHourlyDistribution(epochDay: Long): Flow<List<HourlyTotal>>
    fun getAllWorkedEpochDays(): Flow<List<Long>>
    fun getTotalMinutes(): Flow<Int>
    fun getTotalWorkedDays(): Flow<Int>
    suspend fun insertSession(session: Session): Long
    suspend fun updateSession(session: Session)
    suspend fun deleteSession(session: Session)
    suspend fun deleteAllSessions()
}
