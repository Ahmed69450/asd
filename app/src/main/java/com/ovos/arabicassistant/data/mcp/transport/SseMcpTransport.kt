package com.ovos.arabicassistant.data.mcp.transport

import com.ovos.arabicassistant.data.mcp.model.McpError
import com.ovos.arabicassistant.data.mcp.model.McpRequest
import com.ovos.arabicassistant.data.mcp.model.McpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ناقل الاتصال بالخوادم البعيدة عبر بروتوكول SSE و HTTP لـ MCP
 */
class SseMcpTransport(
    private val endpointUrl: String,
    private val httpClient: OkHttpClient? = null
) : McpTransport {

    private val client: OkHttpClient by lazy {
        httpClient ?: OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun send(request: McpRequest): McpResponse = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", request.id)
                put("method", request.method)
                if (request.params != null) {
                    put("params", JSONObject(request.params))
                }
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext McpResponse(
                        id = request.id,
                        error = McpError(response.code, "فشل طلب MCP عن بعد: ${response.message}")
                    )
                }

                val bodyStr = response.body?.string() ?: "{}"
                val resJson = JSONObject(bodyStr)
                McpResponse(
                    id = resJson.optString("id", request.id),
                    result = resJson.opt("result")
                )
            }
        } catch (e: Exception) {
            McpResponse(
                id = request.id,
                error = McpError(-32603, "خطأ شبكة أثناء استدعاء خادم MCP: ${e.message}")
            )
        }
    }
}
