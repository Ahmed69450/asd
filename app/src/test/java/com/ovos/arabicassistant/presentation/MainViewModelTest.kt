package com.ovos.arabicassistant.presentation

import com.ovos.arabicassistant.domain.model.RouteResult
import com.ovos.arabicassistant.domain.usecase.ProcessVoiceInputUseCase
import com.ovos.arabicassistant.presentation.state.AssistantUiState
import com.ovos.arabicassistant.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial ui state is Idle`() {
        val useCase = ProcessVoiceInputUseCase(
            router = { RouteResult("fallback", 0f) },
            carRepo = object : com.ovos.arabicassistant.domain.repository.CarControlRepository {
                override suspend fun executeAction(action: com.ovos.arabicassistant.domain.model.CarAction) = true
            },
            memoryRepo = object : com.ovos.arabicassistant.domain.repository.MemoryRepository {
                override suspend fun getRelevantFacts(query: String, limit: Int) = emptyList<com.ovos.arabicassistant.domain.model.MemoryFact>()
                override suspend fun saveFact(fact: com.ovos.arabicassistant.domain.model.MemoryFact) = 1L
            },
            llmGenerator = { _, _ -> flowOf("رد") }
        )

        val viewModel = MainViewModel(useCase)
        assertEquals(AssistantUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `sets state to listening when mic is activated`() {
        val useCase = ProcessVoiceInputUseCase(
            router = { RouteResult("fallback", 0f) },
            carRepo = object : com.ovos.arabicassistant.domain.repository.CarControlRepository {
                override suspend fun executeAction(action: com.ovos.arabicassistant.domain.model.CarAction) = true
            },
            memoryRepo = object : com.ovos.arabicassistant.domain.repository.MemoryRepository {
                override suspend fun getRelevantFacts(query: String, limit: Int) = emptyList<com.ovos.arabicassistant.domain.model.MemoryFact>()
                override suspend fun saveFact(fact: com.ovos.arabicassistant.domain.model.MemoryFact) = 1L
            },
            llmGenerator = { _, _ -> flowOf("رد") }
        )

        val viewModel = MainViewModel(useCase)
        viewModel.onMicActivated()
        assertEquals(AssistantUiState.Listening, viewModel.uiState.value)
    }
}
