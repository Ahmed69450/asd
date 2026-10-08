package com.ovos.arabicassistant.domain.repository

import com.ovos.arabicassistant.domain.model.CarAction

interface CarControlRepository {
    suspend fun executeAction(action: CarAction): Boolean
}
