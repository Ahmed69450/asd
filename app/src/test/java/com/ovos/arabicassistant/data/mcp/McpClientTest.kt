package com.ovos.arabicassistant.data.mcp

import com.ovos.arabicassistant.data.mcp.model.McpRequest
import com.ovos.arabicassistant.data.mcp.tools.ScreenAwarenessTool
import com.ovos.arabicassistant.data.mcp.tools.WebSearchTool
import com.ovos.arabicassistant.data.mcp.transport.InAppMcpTransport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpClientTest {

    private class FakeWebSearchTool : WebSearchTool(null) {
        override suspend fun execute(query: String): String {
            return "نتيجة البحث عن: $query (عاصمة المملكة العربية السعودية هي الرياض)."
        }
    }

    private class FakeScreenAwarenessTool : ScreenAwarenessTool({ "تطبيق الخرائط معروض والوجهة مكة المكرمة" })

    @Test
    fun `lists available tools including web search and screen awareness`() = runBlocking {
        val registry = McpToolRegistry()
        registry.registerTool(FakeWebSearchTool())
        registry.registerTool(FakeScreenAwarenessTool())

        val transport = InAppMcpTransport(registry)
        val client = McpClient(transport)

        val tools = client.listTools()
        assertEquals(2, tools.size)
        assertTrue(tools.any { it.name == "web_search" })
        assertTrue(tools.any { it.name == "screen_awareness" })
    }

    @Test
    fun `dispatches tool call via JSON-RPC protocol`() = runBlocking {
        val registry = McpToolRegistry()
        registry.registerTool(FakeWebSearchTool())
        registry.registerTool(FakeScreenAwarenessTool())

        val transport = InAppMcpTransport(registry)
        val client = McpClient(transport)

        val result = client.callTool("screen_awareness", emptyMap())
        assertTrue(result.isSuccess)
        assertTrue(result.content.contains("تطبيق الخرائط"))

        val webResult = client.callTool("web_search", mapOf("query" to "الرياض"))
        assertTrue(webResult.isSuccess)
        assertTrue(webResult.content.contains("الرياض"))
    }
}
