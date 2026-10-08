package com.ovos.arabicassistant.data.mcp.transport

import com.ovos.arabicassistant.data.mcp.model.McpRequest
import com.ovos.arabicassistant.data.mcp.model.McpResponse

/**
 * واجهة ناقل رسائل بروتوكول MCP (سواء كان محلياً In-App أو بعيداً SSE)
 */
interface McpTransport {
    suspend fun send(request: McpRequest): McpResponse
}
