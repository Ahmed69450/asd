package com.ovos.arabicassistant.presentation.state

/**
 * حالات واجهة المستخدم التفاعلية لشاشة السيارة
 */
sealed class AssistantUiState {
    object Idle : AssistantUiState()
    object Listening : AssistantUiState()
    data class Processing(val utterance: String) : AssistantUiState()
    data class Speaking(val text: String, val isHardware: Boolean) : AssistantUiState()
    data class Error(val message: String) : AssistantUiState()
}
