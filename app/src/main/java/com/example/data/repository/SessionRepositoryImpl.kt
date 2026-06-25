package com.example.data.repository

import com.example.data.local.dao.LabelDao
import com.example.data.local.dao.SessionDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.DayTotal
import com.example.domain.model.HourlyTotal
import com.example.domain.model.LabelTotal
import com.example.domain.model.Session
import com.example.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val labelDao: LabelDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<Session>> {
        return combine(sessionDao.getAllSessions(), labelDao.getAllLabels()) { entities, labels ->
            val labelMap = labels.associateBy { it.id }.mapValues { it.value.toDomain() }
            entities.map { entity ->
                entity.toDomain(label = labelMap[entity.labelId])
            }
        }
    }

    override fun getSessionsInRange(startDay: Long, endDay: Long): Flow<List<Session>> {
        return combine(sessionDao.getSessionsInRange(startDay, endDay), labelDao.getAllLabels()) { entities, labels ->
            val labelMap = labels.associateBy { it.id }.mapValues { it.value.toDomain() }
            entities.map { entity ->
                entity.toDomain(label = labelMap[entity.labelId])
            }
        }
    }

    override fun getSessionsForDay(epochDay: Long): Flow<List<Session>> {
        return combine(sessionDao.getSessionsForDay(epochDay), labelDao.getAllLabels()) { entities, labels ->
            val labelMap = labels.associateBy { it.id }.mapValues { it.value.toDomain() }
            entities.map { entity ->
                entity.toDomain(label = labelMap[entity.labelId])
            }
        }
    }

    override fun getDailyTotals(startDay: Long, endDay: Long): Flow<List<DayTotal>> {
        return sessionDao.getDailyTotals(startDay, endDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTotalMinutesPerLabel(): Flow<List<LabelTotal>> {
        return combine(sessionDao.getTotalMinutesPerLabel(), labelDao.getAllLabels()) { entities, labels ->
            val labelMap = labels.associateBy { it.id }.mapValues { it.value.toDomain() }
            entities.map { entity ->
                entity.toDomain(label = labelMap[entity.labelId])
            }
        }
    }

    override fun getHourlyDistribution(epochDay: Long): Flow<List<HourlyTotal>> {
        return sessionDao.getHourlyDistribution(epochDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllWorkedEpochDays(): Flow<List<Long>> {
        return sessionDao.getAllWorkedEpochDays()
    }

    override fun getTotalMinutes(): Flow<Int> {
        return sessionDao.getTotalMinutes()
    }

    override fun getTotalWorkedDays(): Flow<Int> {
        return sessionDao.getTotalWorkedDays()
    }

    override suspend fun insertSession(session: Session): Long {
        return sessionDao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session.toEntity())
    }

    override suspend fun deleteSession(session: Session) {
        sessionDao.deleteSession(session.toEntity())
    }

    override suspend fun deleteAllSessions() {
        sessionDao.deleteAllSessions()
    }
}
