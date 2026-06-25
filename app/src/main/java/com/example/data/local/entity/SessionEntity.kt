package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = LabelEntity::class,
        parentColumns = ["id"],
        childColumns = ["labelId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("labelId"), Index("epochDay")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,             // ChronoUnit.DAYS.between(LocalDate.EPOCH, date)
    val startEpochMs: Long,         // System.currentTimeMillis() at session start
    val durationMinutes: Int,
    val labelId: Long? = null,
    val type: String,               // "POMODORO" or "MANUAL"
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
