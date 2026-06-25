package com.example.domain.usecase

import com.example.domain.model.HourlyTotal
import com.example.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDailyStatsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(epochDay: Long): Flow<List<HourlyTotal>> {
        return sessionRepository.getHourlyDistribution(epochDay)
    }
}
