package com.example.data.local.entity

data class DayTotal(
    val epochDay: Long,
    val totalMinutes: Int
)

data class LabelTotal(
    val labelId: Long?,
    val totalMinutes: Int
)

data class HourlyTotal(
    val hour: Int,
    val totalMinutes: Int
)
