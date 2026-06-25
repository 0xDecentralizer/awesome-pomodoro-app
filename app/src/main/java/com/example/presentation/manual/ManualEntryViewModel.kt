package com.example.presentation.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Label
import com.example.domain.model.Session
import com.example.domain.repository.LabelRepository
import com.example.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val labelRepository: LabelRepository
) : ViewModel() {

    val allSessions: StateFlow<List<Session>> = sessionRepository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val labels: StateFlow<List<Label>> = labelRepository.getAllLabels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var lastDeletedSession: Session? = null

    fun deleteSession(session: Session) {
        lastDeletedSession = session
        viewModelScope.launch {
            sessionRepository.deleteSession(session)
        }
    }

    fun restoreLastDeletedSession() {
        val sessionToRestore = lastDeletedSession ?: return
        viewModelScope.launch {
            sessionRepository.insertSession(sessionToRestore)
            lastDeletedSession = null
        }
    }

    fun saveManualSession(
        date: LocalDate,
        durationMinutes: Int,
        labelId: Long?,
        note: String?
    ) {
        viewModelScope.launch {
            // Calculate start epoch milliseconds based on the chosen date and current time (or standard start time)
            val dateTime = date.atTime(LocalTime.now())
            val startEpochMs = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val session = Session(
                id = 0,
                epochDay = date.toEpochDay(),
                startEpochMs = startEpochMs,
                durationMinutes = durationMinutes,
                labelId = labelId,
                type = "MANUAL",
                note = note?.ifBlank { null },
                createdAt = System.currentTimeMillis()
            )
            sessionRepository.insertSession(session)
        }
    }

    fun addCustomLabel(name: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            labelRepository.insertLabel(
                Label(
                    id = 0,
                    name = name,
                    emoji = emoji,
                    colorHex = colorHex,
                    isPredefined = false,
                    sortOrder = 10,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}
