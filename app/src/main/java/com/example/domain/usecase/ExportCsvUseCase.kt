package com.example.domain.usecase

import com.example.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportCsvUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend fun execute(): String {
        val sessions = sessionRepository.getAllSessions().first()
        val builder = StringBuilder()
        // Headers: date, start_datetime, duration_minutes, label_name, type, note
        builder.append("date,start_datetime,duration_minutes,label_name,type,note\n")

        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        for (session in sessions) {
            val dateStr = LocalDate.ofEpochDay(session.epochDay).format(dateFormatter)
            val startDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(session.startEpochMs),
                ZoneId.systemDefault()
            ).format(dateTimeFormatter)

            val duration = session.durationMinutes
            val labelName = session.label?.name ?: "Unlabeled"
            val type = session.type
            val note = session.note?.replace("\"", "\"\"") ?: ""
            val escapedNote = if (note.contains(",") || note.contains("\n") || note.contains("\"")) {
                "\"$note\""
            } else {
                note
            }

            builder.append("$dateStr,$startDateTime,$duration,$labelName,$type,$escapedNote\n")
        }
        return builder.toString()
    }
}
