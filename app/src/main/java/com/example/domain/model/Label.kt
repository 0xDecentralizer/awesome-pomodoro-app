package com.example.domain.model

data class Label(
    val id: Long,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val isPredefined: Boolean,
    val sortOrder: Int,
    val createdAt: Long
)
