package com.ovos.arabicassistant.data.mcp

import com.ovos.arabicassistant.data.mcp.model.McpToolDefinition
import com.ovos.arabicassistant.data.mcp.model.McpToolResult

/**
 * واجهة أداة MCP الموحدة
 */
interface McpTool {
    val definition: McpToolDefinition
    suspend fun execute(arguments: Map<String, Any>): McpToolResult
}
