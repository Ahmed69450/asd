package com.ovos.arabicassistant.data.mcp

import com.ovos.arabicassistant.domain.model.McpToolCall
import com.ovos.arabicassistant.domain.model.McpToolExecutionResult
import com.ovos.arabicassistant.domain.repository.McpRepository

class McpRepositoryImpl(
    private val mcpClient: McpClient
) : McpRepository {

    override suspend fun getAvailableTools(): List<String> {
        return mcpClient.listTools().map { it.name }
    }

    override suspend fun executeTool(call: McpToolCall): McpToolExecutionResult {
        val result = mcpClient.callTool(call.toolName, call.arguments)
        return McpToolExecutionResult(
            toolName = call.toolName,
            isSuccess = result.isSuccess,
            output = if (result.isSuccess) result.content else (result.error ?: "خطأ غير معروف")
        )
    }
}
