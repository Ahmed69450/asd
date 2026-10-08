package com.ovos.arabicassistant.data.mcp.model

/**
 * رسائل بروتوكول سياق النماذج المتوافقة مع مواصفة JSON-RPC 2.0
 */
data class McpRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: Map<String, Any>? = null
)

data class McpResponse(
    val jsonrpc: String = "2.0",
    val id: String,
    val result: Any? = null,
    val error: McpError? = null
)

data class McpError(
    val code: Int,
    val message: String
)

data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any> = emptyMap()
)

data class McpToolResult(
    val isSuccess: Boolean,
    val content: String,
    val error: String? = null
)
