package com.ovos.arabicassistant.data.mcp

import com.ovos.arabicassistant.data.mcp.model.*
import com.ovos.arabicassistant.data.mcp.transport.McpTransport
import java.util.UUID

/**
 * عميل بروتوكول سياق النماذج (Model Context Protocol Client)
 * يدير استكشاف الأدوات (tools/list) واستدعاءها (tools/call) عبر طبقة الناقل.
 */
class McpClient(
    private val transport: McpTransport
) {

    suspend fun listTools(): List<McpToolDefinition> {
        val request = McpRequest(
            id = UUID.randomUUID().toString(),
            method = "tools/list"
        )
        val response = transport.send(request)
        if (response.error != null) {
            return emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        val resultObj = response.result as? Map<String, Any> ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return (resultObj["tools"] as? List<McpToolDefinition>) ?: emptyList()
    }

    suspend fun callTool(name: String, arguments: Map<String, Any> = emptyMap()): McpToolResult {
        val request = McpRequest(
            id = UUID.randomUUID().toString(),
            method = "tools/call",
            params = mapOf(
                "name" to name,
                "arguments" to arguments
            )
        )

        val response = transport.send(request)
        if (response.error != null) {
            return McpToolResult(
                isSuccess = false,
                content = "",
                error = response.error.message
            )
        }

        @Suppress("UNCHECKED_CAST")
        val resultObj = response.result as? Map<String, Any>
        return if (resultObj != null) {
            McpToolResult(
                isSuccess = (resultObj["isSuccess"] as? Boolean) ?: true,
                content = resultObj["content"]?.toString() ?: "",
                error = resultObj["error"]?.toString()
            )
        } else {
            McpToolResult(
                isSuccess = true,
                content = response.result?.toString() ?: ""
            )
        }
    }
}
