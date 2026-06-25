package com.example.domain.usecase

import com.example.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int
)

class GetStreakUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): Flow<StreakInfo> {
        return sessionRepository.getAllWorkedEpochDays().map { workedDays ->
            calculateStreak(workedDays)
        }
    }

    private fun calculateStreak(workedDays: List<Long>): StreakInfo {
        if (workedDays.isEmpty()) return StreakInfo(0, 0)

        // Ensure distinct and sorted in descending order (most recent first)
        val sortedDays = workedDays.distinct().sortedDescending()

        val today = LocalDate.now().toEpochDay()
        val yesterday = today - 1

        // Current streak calculation
        var currentStreak = 0
        if (sortedDays.contains(today) || sortedDays.contains(yesterday)) {
            val startDay = if (sortedDays.contains(today)) today else yesterday
            var expectedDay = startDay
            for (day in sortedDays) {
                // Skip days that are in the future of our anchor
                if (day > startDay) continue
                
                if (day == expectedDay) {
                    currentStreak++
                    expectedDay--
                } else if (day < expectedDay) {
                    // Gap found, break the current streak traversal
                    break
                }
            }
        }

        // Best streak calculation (all-time longest run)
        var bestStreak = 0
        var tempStreak = 0
        var expectedDay: Long? = null

        // Since sortedDays is descending, we look for consecutive descending numbers
        for (day in sortedDays) {
            val exp = expectedDay
            if (exp == null) {
                tempStreak = 1
                expectedDay = day - 1
            } else if (day == exp) {
                tempStreak++
                expectedDay = day - 1
            } else {
                if (tempStreak > bestStreak) {
                    bestStreak = tempStreak
                }
                tempStreak = 1
                expectedDay = day - 1
            }
        }
        if (tempStreak > bestStreak) {
            bestStreak = tempStreak
        }

        return StreakInfo(
            currentStreak = currentStreak,
            bestStreak = bestStreak
        )
    }
}
