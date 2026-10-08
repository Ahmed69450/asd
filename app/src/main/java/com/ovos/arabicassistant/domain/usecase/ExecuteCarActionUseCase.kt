package com.ovos.arabicassistant.domain.usecase

import com.ovos.arabicassistant.domain.model.CarAction
import com.ovos.arabicassistant.domain.repository.CarControlRepository

/**
 * حالة الاستخدام لتنفيذ أوامر عتاد السيارة الفعلية
 */
class ExecuteCarActionUseCase(
    private val carControlRepository: CarControlRepository
) {
    suspend operator fun invoke(action: CarAction): Boolean {
        return carControlRepository.executeAction(action)
    }
}
