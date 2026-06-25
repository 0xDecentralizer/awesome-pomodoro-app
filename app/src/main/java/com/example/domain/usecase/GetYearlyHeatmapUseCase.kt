package com.example.domain.usecase

import com.example.domain.model.DayTotal
import com.example.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetYearlyHeatmapUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(startDay: Long, endDay: Long): Flow<List<DayTotal>> {
        return sessionRepository.getDailyTotals(startDay, endDay)
    }
}
