package com.example.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.DayTotal
import com.example.domain.model.LabelTotal
import com.example.domain.usecase.SummaryStats
import com.example.domain.repository.SessionRepository
import com.example.domain.usecase.GetSummaryStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getSummaryStatsUseCase: GetSummaryStatsUseCase
) : ViewModel() {

    // Aggregate stats (streaks, total hours, etc.)
    val summaryStats: StateFlow<SummaryStats?> = getSummaryStatsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Last 7 days focus minutes
    val weeklyTotals: StateFlow<List<DayTotal>> = run {
        val end = LocalDate.now().toEpochDay()
        val start = LocalDate.now().minusDays(6).toEpochDay()
        sessionRepository.getDailyTotals(start, end)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Label distribution breakdown
    val labelBreakdown: StateFlow<List<LabelTotal>> = sessionRepository.getTotalMinutesPerLabel()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Heatmap: entire current month
    val monthlyTotals: StateFlow<List<DayTotal>> = run {
        val start = LocalDate.now().withDayOfMonth(1).toEpochDay()
        val end = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
        sessionRepository.getDailyTotals(start, end)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
}
