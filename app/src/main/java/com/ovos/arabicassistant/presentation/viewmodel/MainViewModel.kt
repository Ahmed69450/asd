package com.ovos.arabicassistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovos.arabicassistant.domain.model.DynamicContext
import com.ovos.arabicassistant.domain.usecase.ProcessVoiceInputUseCase
import com.ovos.arabicassistant.domain.usecase.ProcessedVoiceResult
import com.ovos.arabicassistant.presentation.state.AssistantUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "assistant"
    val text: String,
    val isHardware: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(
    private val processVoiceInputUseCase: ProcessVoiceInputUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun onMicActivated() {
        _uiState.value = AssistantUiState.Listening
    }

    fun onUserUtterance(
        utterance: String,
        dynamicContext: DynamicContext? = null,
        onSpeakRequired: ((String) -> Unit)? = null
    ) {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = AssistantUiState.Idle
            return
        }

        // إضافة رسالة المستخدم للسجل
        val updatedList = _messages.value.toMutableList().apply {
            add(ChatMessage(sender = "user", text = trimmed))
        }
        _messages.value = updatedList

        _uiState.value = AssistantUiState.Processing(trimmed)

        viewModelScope.launch {
            try {
                val result: ProcessedVoiceResult = processVoiceInputUseCase.execute(trimmed, dynamicContext)

                // إضافة رد المساعد للسجل
                val assistantMessages = _messages.value.toMutableList().apply {
                    add(ChatMessage(sender = "assistant", text = result.spokenResponse, isHardware = result.isHardwareAction))
                }
                _messages.value = assistantMessages

                _uiState.value = AssistantUiState.Speaking(result.spokenResponse, result.isHardwareAction)

                // نطق الرد عبر Piper TTS
                onSpeakRequired?.invoke(result.spokenResponse)
            } catch (e: Exception) {
                _uiState.value = AssistantUiState.Error(e.message ?: "حدث خطأ غير متوقع")
            }
        }
    }

    fun onSpeechCompleted() {
        _uiState.value = AssistantUiState.Idle
    }
}
