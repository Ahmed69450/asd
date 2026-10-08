package com.ovos.arabicassistant.data.mcp

import com.ovos.arabicassistant.data.mcp.model.McpToolDefinition
import com.ovos.arabicassistant.data.mcp.model.McpToolResult
import java.util.concurrent.ConcurrentHashMap

/**
 * سجل أدوات MCP المتاحة داخل التطبيق
 */
class McpToolRegistry {

    private val tools = ConcurrentHashMap<String, McpTool>()

    fun registerTool(tool: McpTool) {
        tools[tool.definition.name] = tool
    }

    fun listToolDefinitions(): List<McpToolDefinition> {
        return tools.values.map { it.definition }
    }

    suspend fun executeTool(name: String, arguments: Map<String, Any>): McpToolResult {
        val tool = tools[name] ?: return McpToolResult(
            isSuccess = false,
            content = "",
            error = "الأداة '$name' غير مسجلة في النظام."
        )

        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            McpToolResult(
                isSuccess = false,
                content = "",
                error = "فشل تنفيذ أداة '$name': ${e.message}"
            )
        }
    }
}
