package com.ovos.arabicassistant.data.mcp.tools

import com.ovos.arabicassistant.data.mcp.McpTool
import com.ovos.arabicassistant.data.mcp.model.McpToolDefinition
import com.ovos.arabicassistant.data.mcp.model.McpToolResult

/**
 * أداة الوعي بمحتوى الشاشة عبر بروتوكول MCP
 * تستخرج سياق التطبيق المفتوح حالياً وعناصر واجهة المستخدم المعروضة على الشاشة.
 */
open class ScreenAwarenessTool(
    private val screenContextProvider: () -> String
) : McpTool {

    override val definition: McpToolDefinition = McpToolDefinition(
        name = "screen_awareness",
        description = "استخراج محتوى وتفاصيل النصوص والعناصر المعروضة حالياً على شاشة السيارة",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "detail_level" to mapOf(
                    "type" to "string",
                    "description" to "مستوى التفصيل (summary أو full)"
                )
            )
        )
    )

    open fun getScreenContent(): String {
        return screenContextProvider()
    }

    override suspend fun execute(arguments: Map<String, Any>): McpToolResult {
        val content = getScreenContent()
        return McpToolResult(
            isSuccess = true,
            content = if (content.isNotBlank()) content else "الشاشة الحالية لا تحتوي على نصوص مقروءة أو الخدمة غير مفعلة."
        )
    }
}
