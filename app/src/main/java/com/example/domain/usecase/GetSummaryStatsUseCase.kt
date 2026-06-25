package com.example.domain.usecase

import com.example.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

data class SummaryStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val daysWorked: Int,
    val allTimeMinutes: Int,
    val todayMinutes: Int
)

class GetSummaryStatsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getStreakUseCase: GetStreakUseCase
) {
    operator fun invoke(): Flow<SummaryStats> {
        val todayEpoch = LocalDate.now().toEpochDay()
        val todaySessionsFlow = sessionRepository.getSessionsForDay(todayEpoch)
        val totalMinutesFlow = sessionRepository.getTotalMinutes()
        val totalWorkedDaysFlow = sessionRepository.getTotalWorkedDays()
        val streakFlow = getStreakUseCase()

        return combine(
            todaySessionsFlow,
            totalMinutesFlow,
            totalWorkedDaysFlow,
            streakFlow
        ) { todaySessions, totalMinutes, totalWorkedDays, streakInfo ->
            val todayMin = todaySessions.sumOf { it.durationMinutes }
            SummaryStats(
                currentStreak = streakInfo.currentStreak,
                bestStreak = streakInfo.bestStreak,
                daysWorked = totalWorkedDays,
                allTimeMinutes = totalMinutes,
                todayMinutes = todayMin
            )
        }
    }
}
