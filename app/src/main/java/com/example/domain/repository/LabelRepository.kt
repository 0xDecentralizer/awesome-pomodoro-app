package com.example.domain.repository

import com.example.domain.model.Label
import kotlinx.coroutines.flow.Flow

interface LabelRepository {
    fun getAllLabels(): Flow<List<Label>>
    fun getLabelById(id: Long): Flow<Label?>
    suspend fun insertLabel(label: Label): Long
    suspend fun updateLabel(label: Label)
    suspend fun deleteLabelById(id: Long)
    suspend fun deleteAllCustomLabels()
}
