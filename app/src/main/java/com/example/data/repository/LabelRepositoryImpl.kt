package com.example.data.repository

import com.example.data.local.dao.LabelDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Label
import com.example.domain.repository.LabelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LabelRepositoryImpl @Inject constructor(
    private val labelDao: LabelDao
) : LabelRepository {

    override fun getAllLabels(): Flow<List<Label>> {
        return labelDao.getAllLabels().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getLabelById(id: Long): Flow<Label?> {
        return labelDao.getLabelById(id).map { it?.toDomain() }
    }

    override suspend fun insertLabel(label: Label): Long {
        return labelDao.insertLabel(label.toEntity())
    }

    override suspend fun updateLabel(label: Label) {
        labelDao.updateLabel(label.toEntity())
    }

    override suspend fun deleteLabelById(id: Long) {
        labelDao.deleteLabelById(id)
    }

    override suspend fun deleteAllCustomLabels() {
        labelDao.deleteAllCustomLabels()
    }
}
