package com.ovos.arabicassistant.data.car

import com.ovos.arabicassistant.domain.model.CarAction
import com.ovos.arabicassistant.domain.repository.CarControlRepository

class CarControlRepositoryImpl(
    private val carControlManager: CarControlManager
) : CarControlRepository {

    override suspend fun executeAction(action: CarAction): Boolean {
        return carControlManager.execute(action)
    }
}
