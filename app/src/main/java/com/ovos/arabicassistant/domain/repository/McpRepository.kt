package com.ovos.arabicassistant.domain.repository

import com.ovos.arabicassistant.domain.model.McpToolCall
import com.ovos.arabicassistant.domain.model.McpToolExecutionResult

interface McpRepository {
    suspend fun getAvailableTools(): List<String>
    suspend fun executeTool(call: McpToolCall): McpToolExecutionResult
}
