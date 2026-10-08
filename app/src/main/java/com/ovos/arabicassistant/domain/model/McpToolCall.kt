package com.ovos.arabicassistant.domain.model

/**
 * تمثيل استدعاء أداة عبر بروتوكول MCP
 */
data class McpToolCall(
    val toolName: String,
    val arguments: Map<String, Any> = emptyMap()
)

data class McpToolExecutionResult(
    val toolName: String,
    val isSuccess: Boolean,
    val output: String
)
