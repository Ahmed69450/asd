package com.ovos.arabicassistant.domain

import com.ovos.arabicassistant.domain.model.*
import com.ovos.arabicassistant.domain.repository.CarControlRepository
import com.ovos.arabicassistant.domain.repository.MemoryRepository
import com.ovos.arabicassistant.domain.usecase.ProcessVoiceInputUseCase
import com.ovos.arabicassistant.domain.usecase.RouteSemanticIntentUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessVoiceInputUseCaseTest {

    private class FakeCarControlRepository : CarControlRepository {
        var lastExecutedAction: CarAction? = null
        override suspend fun executeAction(action: CarAction): Boolean {
            lastExecutedAction = action
            return true
        }
    }

    private class FakeMemoryRepository : MemoryRepository {
        override suspend fun getRelevantFacts(query: String, limit: Int): List<MemoryFact> {
            return listOf(MemoryFact(1L, "user_pref", "حرارة", "السائق يفضل درجة 22", System.currentTimeMillis()))
        }
        override suspend fun saveFact(fact: MemoryFact): Long = 1L
    }

    @Test
    fun `when confidence is 70 percent or higher routes to car hardware directly`() = runBlocking {
        val fakeCarRepo = FakeCarControlRepository()
        val fakeMemoryRepo = FakeMemoryRepository()

        // Stub usecase returning high-confidence hardware route
        val useCase = ProcessVoiceInputUseCase(
            router = { RouteResult(intent = "ac_temp_down", confidence = 0.85f, isHardware = true, parameters = mapOf("temp" to "-1")) },
            carRepo = fakeCarRepo,
            memoryRepo = fakeMemoryRepo,
            llmGenerator = { _, _ -> flowOf("رد النموذج") }
        )

        val result = useCase.execute("خفض حرارة المكيف")

        assertEquals("تم خفض درجة حرارة المكيف بنجاح.", result.spokenResponse)
        assertTrue(result.isHardwareAction)
        assertEquals(CarActionType.AC_TEMP, fakeCarRepo.lastExecutedAction?.type)
    }

    @Test
    fun `when confidence is below 70 percent falls back to LLM with memory injection`() = runBlocking {
        val fakeCarRepo = FakeCarControlRepository()
        val fakeMemoryRepo = FakeMemoryRepository()

        val useCase = ProcessVoiceInputUseCase(
            router = { RouteResult(intent = "unknown", confidence = 0.40f, isHardware = false) },
            carRepo = fakeCarRepo,
            memoryRepo = fakeMemoryRepo,
            llmGenerator = { prompt, context -> flowOf("الجواب من النموذج اللغوي مع السياق: $context") }
        )

        val result = useCase.execute("ما هي عاصمة سلطنة عمان؟")

        assertTrue(result.spokenResponse.contains("الجواب من النموذج اللغوي"))
        assertEquals(false, result.isHardwareAction)
    }
}
