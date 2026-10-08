package com.ovos.arabicassistant.data.mcp.transport

import com.ovos.arabicassistant.data.mcp.McpToolRegistry
import com.ovos.arabicassistant.data.mcp.model.McpError
import com.ovos.arabicassistant.data.mcp.model.McpRequest
import com.ovos.arabicassistant.data.mcp.model.McpResponse

/**
 * ناقل الاتصال المحلي المدمج داخل التطبيق (In-App In-Memory Transport)
 * يعالج طلبات JSON-RPC 2.0 مباشرة ويوجهها إلى سجل الأدوات المحلي McpToolRegistry.
 */
class InAppMcpTransport(
    private val toolRegistry: McpToolRegistry
) : McpTransport {

    override suspend fun send(request: McpRequest): McpResponse {
        return when (request.method) {
            "tools/list" -> {
                val tools = toolRegistry.listToolDefinitions()
                McpResponse(
                    id = request.id,
                    result = mapOf("tools" to tools)
                )
            }
            "tools/call" -> {
                val params = request.params ?: return McpResponse(
                    id = request.id,
                    error = McpError(-32602, "المعاملات (params) مفقودة في الطلب.")
                )
                val toolName = params["name"]?.toString() ?: return McpResponse(
                    id = request.id,
                    error = McpError(-32602, "اسم الأداة (name) مطلوب.")
                )
                @Suppress("UNCHECKED_CAST")
                val arguments = (params["arguments"] as? Map<String, Any>) ?: emptyMap()

                val executionResult = toolRegistry.executeTool(toolName, arguments)
                McpResponse(
                    id = request.id,
                    result = mapOf(
                        "isSuccess" to executionResult.isSuccess,
                        "content" to executionResult.content,
                        "error" to executionResult.error
                    )
                )
            }
            else -> {
                McpResponse(
                    id = request.id,
                    error = McpError(-32601, "الطريقة '${request.method}' غير مدعومة.")
                )
            }
        }
    }
}
