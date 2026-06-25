package com.example.domain.usecase

import com.example.domain.model.Session
import com.example.domain.repository.SessionRepository
import javax.inject.Inject

class SaveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(session: Session): Long {
        return sessionRepository.insertSession(session)
    }
}
