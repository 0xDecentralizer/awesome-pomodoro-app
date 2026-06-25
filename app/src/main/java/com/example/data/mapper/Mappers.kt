package com.example.data.mapper

import com.example.data.local.entity.LabelEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.DayTotal as EntityDayTotal
import com.example.data.local.entity.HourlyTotal as EntityHourlyTotal
import com.example.data.local.entity.LabelTotal as EntityLabelTotal
import com.example.domain.model.DayTotal as DomainDayTotal
import com.example.domain.model.HourlyTotal as DomainHourlyTotal
import com.example.domain.model.LabelTotal as DomainLabelTotal
import com.example.domain.model.Label
import com.example.domain.model.Session

fun LabelEntity.toDomain(): Label {
    return Label(
        id = id,
        name = name,
        emoji = emoji,
        colorHex = colorHex,
        isPredefined = isPredefined,
        sortOrder = sortOrder,
        createdAt = createdAt
    )
}

fun Label.toEntity(): LabelEntity {
    return LabelEntity(
        id = id,
        name = name,
        emoji = emoji,
        colorHex = colorHex,
        isPredefined = isPredefined,
        sortOrder = sortOrder,
        createdAt = createdAt
    )
}

fun SessionEntity.toDomain(label: Label? = null): Session {
    return Session(
        id = id,
        epochDay = epochDay,
        startEpochMs = startEpochMs,
        durationMinutes = durationMinutes,
        labelId = labelId,
        type = type,
        note = note,
        createdAt = createdAt,
        label = label
    )
}

fun Session.toEntity(): SessionEntity {
    return SessionEntity(
        id = id,
        epochDay = epochDay,
        startEpochMs = startEpochMs,
        durationMinutes = durationMinutes,
        labelId = labelId,
        type = type,
        note = note,
        createdAt = createdAt
    )
}

fun EntityDayTotal.toDomain(): DomainDayTotal {
    return DomainDayTotal(
        epochDay = epochDay,
        totalMinutes = totalMinutes
    )
}

fun EntityHourlyTotal.toDomain(): DomainHourlyTotal {
    return DomainHourlyTotal(
        hour = hour,
        totalMinutes = totalMinutes
    )
}

fun EntityLabelTotal.toDomain(label: Label? = null): DomainLabelTotal {
    return DomainLabelTotal(
        labelId = labelId,
        totalMinutes = totalMinutes,
        labelName = label?.name ?: "Unlabeled",
        labelEmoji = label?.emoji ?: "🍅",
        labelColorHex = label?.colorHex ?: "#6366F1"
    )
}
