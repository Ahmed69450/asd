package com.ovos.arabicassistant.data.mcp.tools

import com.ovos.arabicassistant.data.mcp.McpTool
import com.ovos.arabicassistant.data.mcp.model.McpToolDefinition
import com.ovos.arabicassistant.data.mcp.model.McpToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * أداة البحث المباشر في الويب عبر بروتوكول MCP
 * تجلب معلومات فورية وموجزة وموثقة عن الأسئلة العامة والأحداث عبر الإنترنت.
 */
open class WebSearchTool(
    private val httpClient: OkHttpClient? = null
) : McpTool {

    private val client: OkHttpClient by lazy {
        httpClient ?: OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    override val definition: McpToolDefinition = McpToolDefinition(
        name = "web_search",
        description = "جلب معلومات حية من الإنترنت عن الأحداث الجارية، الأخبار، أو المعارف العامة",
        inputSchema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "query" to mapOf(
                    "type" to "string",
                    "description" to "عبارة البحث المطلوبة"
                )
            ),
            "required" to listOf("query")
        )
    )

    open suspend fun execute(query: String): String = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext "استعلام فارغ."

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CarVoiceAssistant/1.0 (Android)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "تعذر الوصول إلى شبكة الإنترنت (رمز الخطأ: ${response.code})."
                }

                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val abstractText = json.optString("AbstractText", "")
                val answer = json.optString("Answer", "")

                when {
                    answer.isNotBlank() -> answer
                    abstractText.isNotBlank() -> abstractText
                    else -> "تم فحص الويب، ولكن لم تتوفر إجابة ملخصة مباشرة لـ: $trimmed."
                }
            }
        } catch (e: Exception) {
            "فشل الاتصال بالإنترنت أثناء البحث: ${e.message}"
        }
    }

    override suspend fun execute(arguments: Map<String, Any>): McpToolResult {
        val query = arguments["query"]?.toString() ?: ""
        val resultText = execute(query)
        return McpToolResult(
            isSuccess = true,
            content = resultText
        )
    }
}
