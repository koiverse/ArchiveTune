/*
 * ArchiveTune (2026)
 * Â© Chartreux Westia â€” github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.concurrent.TimeUnit
import moe.koiverse.archivetune.constants.AiProvider
import org.json.JSONArray
import org.json.JSONObject

class AiServiceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

object AiTextService {
    private const val OpenAiEndpoint = "https://api.openai.com/v1/chat/completions"
    private const val GeminiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
    private const val ClaudeEndpoint = "https://api.anthropic.com/v1/messages"

    private val client =
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(20, TimeUnit.SECONDS)
                    readTimeout(60, TimeUnit.SECONDS)
                    writeTimeout(60, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                }
            }
        }

    suspend fun test(config: AiServiceConfig) {
        val response = complete(
            config = config,
            systemPrompt = "You are a health check endpoint. Reply with OK only.",
            userPrompt = "Reply exactly OK.",
            temperature = 0.0,
            maxTokens = 32,
        ).trim()
        if (!response.equals("OK", ignoreCase = true)) {
            throw AiServiceException("AI API returned an unexpected test response")
        }
    }

    suspend fun translateLines(
        config: AiServiceConfig,
        targetLanguage: String,
        lines: List<String>,
        formatName: String,
    ): List<String> {
        if (lines.isEmpty()) return emptyList()
        val payload = JSONArray()
        lines.forEach { payload.put(it) }
        val response = complete(
            config = config,
            systemPrompt = """
                You are an expert song lyrics translator.
                Translate each input string into $targetLanguage with natural, accurate lyric phrasing.
                Preserve meaning, tone, profanity level, names, repeated hooks, and line-level intent.
                Do not add timestamps, IDs, XML, markdown, explanations, or extra lines.
                Return only a JSON array of strings with exactly ${lines.size} items in the same order.
                The caller will reconstruct the $formatName lyrics container separately.
            """.trimIndent(),
            userPrompt = payload.toString(),
            temperature = 0.15,
            maxTokens = 8192,
        )
        val array = extractJsonArray(response)
        require(array.length() == lines.size) { "AI response changed the lyric segment count" }
        return List(array.length()) { index -> array.optString(index) }
    }

    suspend fun complete(
        config: AiServiceConfig,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.2,
        maxTokens: Int = 4096,
    ): String {
        if (!config.canCallApi) throw AiServiceException("AI provider is not configured")
        return when (config.provider) {
            AiProvider.CHATGPT -> completeOpenAiCompatible(
                endpoint = OpenAiEndpoint,
                apiKey = config.apiKey,
                model = "gpt-5.4",
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = temperature,
                maxTokens = maxTokens,
            )

            AiProvider.CUSTOM -> completeOpenAiCompatible(
                endpoint = config.customEndpoint,
                apiKey = config.apiKey,
                model = "gpt-5.4",
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = temperature,
                maxTokens = maxTokens,
            )

            AiProvider.GEMINI -> completeGemini(
                apiKey = config.apiKey,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = temperature,
                maxTokens = maxTokens,
            )

            AiProvider.CLAUDE -> completeClaude(
                apiKey = config.apiKey,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = temperature,
                maxTokens = maxTokens,
            )

            AiProvider.NONE -> throw AiServiceException("AI provider is disabled")
        }
    }

    private suspend fun completeOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
        maxTokens: Int,
    ): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userPrompt))
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", temperature)
            .put("max_tokens", maxTokens)
            .toString()
        val response = client.post(endpoint.trim()) {
            header("Authorization", "Bearer ${apiKey.trim()}")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw apiException(response.status.value, raw)
        val json = JSONObject(raw)
        val content = json
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.takeIf { it.isNotBlank() }
        return content ?: throw AiServiceException("AI API returned an empty response")
    }

    private suspend fun completeGemini(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
        maxTokens: Int,
    ): String {
        val endpoint = "$GeminiEndpoint?key=${apiKey.trim()}"
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put("text", "$systemPrompt\n\n$userPrompt"),
                        ),
                    ),
                ),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", temperature)
                    .put("maxOutputTokens", maxTokens),
            )
            .toString()
        val response = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw apiException(response.status.value, raw)
        val content = JSONObject(raw)
            .optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.takeIf { it.isNotBlank() }
        return content ?: throw AiServiceException("AI API returned an empty response")
    }

    private suspend fun completeClaude(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
        maxTokens: Int,
    ): String {
        val body = JSONObject()
            .put("model", "claude-3-haiku-20240307")
            .put("max_tokens", maxTokens)
            .put("temperature", temperature)
            .put("system", systemPrompt)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", userPrompt),
                ),
            )
            .toString()
        val response = client.post(ClaudeEndpoint) {
            header("x-api-key", apiKey.trim())
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw apiException(response.status.value, raw)
        val content = JSONObject(raw)
            .optJSONArray("content")
            ?.let { array ->
                buildString {
                    for (index in 0 until array.length()) {
                        val part = array.optJSONObject(index) ?: continue
                        if (part.optString("type") == "text") append(part.optString("text"))
                    }
                }
            }
            ?.takeIf { it.isNotBlank() }
        return content ?: throw AiServiceException("AI API returned an empty response")
    }

    private fun apiException(
        status: Int,
        raw: String,
    ): AiServiceException {
        val message = runCatching { JSONObject(raw).readErrorMessage() }.getOrNull()
            ?: raw.take(240).ifBlank { "HTTP $status" }
        return AiServiceException("AI API failed ($status): $message")
    }
}
