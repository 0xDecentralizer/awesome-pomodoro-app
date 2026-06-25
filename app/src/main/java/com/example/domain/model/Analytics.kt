package com.example.domain.model

data class DayTotal(
    val epochDay: Long,
    val totalMinutes: Int
)

data class LabelTotal(
    val labelId: Long?,
    val totalMinutes: Int,
    val labelName: String = "Unlabeled",
    val labelEmoji: String = "🍅",
    val labelColorHex: String = "#6366F1"
)

data class HourlyTotal(
    val hour: Int,
    val totalMinutes: Int
)
