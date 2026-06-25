package com.example.domain.model

data class Session(
    val id: Long,
    val epochDay: Long,
    val startEpochMs: Long,
    val durationMinutes: Int,
    val labelId: Long?,
    val type: String, // "POMODORO" or "MANUAL"
    val note: String?,
    val createdAt: Long,
    val label: Label? = null
)
