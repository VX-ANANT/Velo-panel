package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiApiService
import com.example.data.api.RetrofitClient
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

data class GeminiModelOption(
    val id: String,
    val displayName: String,
    val category: String,
    val description: String
)

object GeminiModelRegistry {
    val AVAILABLE_MODELS = listOf(
        GeminiModelOption(
            id = "gemini-3.5-flash",
            displayName = "Gemini 3.5 Flash",
            category = "Flash (Recommended)",
            description = "Fast, multimodal, high accuracy for daily admin tasks"
        ),
        GeminiModelOption(
            id = "gemini-3.6-flash-preview",
            displayName = "Gemini 3.6 Flash",
            category = "Flash Preview",
            description = "Next-gen ultra fast reasoning flash model"
        ),
        GeminiModelOption(
            id = "gemini-3.7-flash",
            displayName = "Gemini 3.7 Flash",
            category = "Flash Latest",
            description = "Latest generation flash architecture with instant streaming"
        ),
        GeminiModelOption(
            id = "gemini-3.1-pro-preview",
            displayName = "Gemini 3.1 Pro",
            category = "Pro Reasoning",
            description = "Complex logic, deep analysis, dispute arbitration"
        ),
        GeminiModelOption(
            id = "gemini-3.5-pro",
            displayName = "Gemini 3.5 Pro",
            category = "Pro Advanced",
            description = "Advanced reasoning and deep math calculations"
        ),
        GeminiModelOption(
            id = "gemini-3.6-pro-preview",
            displayName = "Gemini 3.6 Pro",
            category = "Pro Preview",
            description = "State of the art preview reasoning"
        ),
        GeminiModelOption(
            id = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            category = "Stable Flash",
            description = "Proven stable generation with low latency"
        ),
        GeminiModelOption(
            id = "gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            category = "Stable Pro",
            description = "Proven stable pro reasoning model"
        )
    )
}

interface GeminiRepository {
    suspend fun generateWith35Flash(
        prompt: String,
        systemInstruction: String? = null
    ): Result<String>

    suspend fun generateWith36Flash(
        prompt: String,
        systemInstruction: String? = null
    ): Result<String>

    suspend fun generateWith37Flash(
        prompt: String,
        systemInstruction: String? = null
    ): Result<String>

    suspend fun generateText(
        prompt: String,
        model: String = "gemini-3.5-flash",
        systemInstruction: String? = null
    ): Result<String>

    suspend fun sendChatMessage(
        history: List<Content>,
        systemInstruction: String? = null,
        model: String = "gemini-3.5-flash",
        enableThinking: Boolean = false
    ): Result<String>

    suspend fun generateStreamingText(
        prompt: String,
        model: String = "gemini-3.5-flash"
    ): Flow<String>

    suspend fun analyzeImage(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        model: String = "gemini-3.5-flash"
    ): Result<String>
}

class GeminiRepositoryImpl(
    private val apiService: GeminiApiService = RetrofitClient.service
) : GeminiRepository {

    private val defaultSystemInstruction = """
        You are the official Velorix eSports Assistant & Admin Support AI. 
        You assist tournament organizers and players with rule sets, bracket management, prize pool calculations, anti-cheat reviews, and dispute resolutions.
        Be concise, accurate, and professional.
    """.trimIndent()

    private fun extractErrorMessage(e: Throwable): String {
        if (e is HttpException) {
            if (e.code() == 429) {
                return "Gemini API rate limit reached. Retrying automatically..."
            }
            val raw = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) { null }

            if (!raw.isNullOrBlank()) {
                try {
                    val json = Json { ignoreUnknownKeys = true }
                    val obj = json.parseToJsonElement(raw).jsonObject
                    val message = obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    if (!message.isNullOrBlank()) {
                        if (message.contains("rate", ignoreCase = true) || message.contains("quota", ignoreCase = true) || message.contains("ResourceExhausted", ignoreCase = true)) {
                            return "Gemini AI rate limit reached. Please wait a moment or try another model."
                        }
                        return message
                    }
                } catch (_: Exception) {}
                return raw
            }
        }
        val msg = e.localizedMessage ?: e.message ?: "Unknown error"
        if (msg.contains("rate limit", ignoreCase = true) || msg.contains("429")) {
            return "Gemini AI rate limit reached. Please wait a moment."
        }
        return msg
    }

    private suspend fun <T> executeWithRateLimitRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 600,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val isRateLimit = (e is HttpException && e.code() == 429) ||
                        (e.message?.contains("rate", ignoreCase = true) == true) ||
                        (e.message?.contains("quota", ignoreCase = true) == true) ||
                        (e.message?.contains("ResourceExhausted", ignoreCase = true) == true)
                if (isRateLimit && attempt < maxAttempts) {
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay *= 2
                } else if (!isRateLimit) {
                    throw e
                }
            }
        }
        throw lastException ?: Exception("Request failed after retries.")
    }

    override suspend fun generateWith35Flash(
        prompt: String,
        systemInstruction: String?
    ): Result<String> = generateText(
        prompt = prompt,
        model = "gemini-3.5-flash",
        systemInstruction = systemInstruction
    )

    override suspend fun generateWith36Flash(
        prompt: String,
        systemInstruction: String?
    ): Result<String> = generateText(
        prompt = prompt,
        model = "gemini-3.6-flash-preview",
        systemInstruction = systemInstruction
    )

    override suspend fun generateWith37Flash(
        prompt: String,
        systemInstruction: String?
    ): Result<String> = generateText(
        prompt = prompt,
        model = "gemini-3.7-flash",
        systemInstruction = systemInstruction
    )

    override suspend fun generateText(
        prompt: String,
        model: String,
        systemInstruction: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(IllegalStateException("Please set your Gemini API key in the AI Studio Secrets panel."))
            }

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)), role = "user")
                ),
                systemInstruction = (systemInstruction ?: defaultSystemInstruction).let {
                    Content(parts = listOf(Part(text = it)))
                }
            )

            val response = executeWithRateLimitRetry {
                apiService.generateContent(model = model, apiKey = apiKey, request = request)
            }
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("No content generated by Gemini."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(extractErrorMessage(e), e))
        }
    }

    override suspend fun sendChatMessage(
        history: List<Content>,
        systemInstruction: String?,
        model: String,
        enableThinking: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("Please set your Gemini API key in the AI Studio Secrets panel."))
        }

        val cleanContents = history.map { c ->
            Content(
                role = if (c.role == "user" || c.role == "model") c.role else "user",
                parts = c.parts.map { p -> Part(text = p.text) }
            )
        }

        val sysInst = (systemInstruction ?: defaultSystemInstruction).let {
            Content(parts = listOf(Part(text = it)))
        }

        // Try primary call with requested model and configuration
        try {
            val config = if (enableThinking) {
                GenerationConfig(thinkingConfig = ThinkingConfig(thinkingLevel = "high"))
            } else null

            val request = GenerateContentRequest(
                contents = cleanContents,
                generationConfig = config,
                systemInstruction = sysInst
            )

            val response = executeWithRateLimitRetry {
                apiService.generateContent(model = model, apiKey = apiKey, request = request)
            }
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                return@withContext Result.success(text)
            }
        } catch (e: Exception) {
            // If thinkingConfig caused 400 or model failed, retry with standard configuration and fallback model
            if (enableThinking) {
                try {
                    val fallbackRequest = GenerateContentRequest(
                        contents = cleanContents,
                        generationConfig = null,
                        systemInstruction = sysInst
                    )
                    val response = executeWithRateLimitRetry {
                        apiService.generateContent(model = model, apiKey = apiKey, request = fallbackRequest)
                    }
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (text != null) {
                        return@withContext Result.success(text)
                    }
                } catch (_: Exception) {}
            }

            // Retry with standard gemini-3.5-flash as guaranteed fallback
            if (model != "gemini-3.5-flash") {
                try {
                    val retryRequest = GenerateContentRequest(
                        contents = cleanContents,
                        generationConfig = null,
                        systemInstruction = sysInst
                    )
                    val retryResponse = executeWithRateLimitRetry {
                        apiService.generateContent(model = "gemini-3.5-flash", apiKey = apiKey, request = retryRequest)
                    }
                    val retryText = retryResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (retryText != null) {
                        return@withContext Result.success(retryText)
                    }
                } catch (_: Exception) {}
            }

            return@withContext Result.failure(Exception(extractErrorMessage(e), e))
        }

        Result.failure(Exception("No response received from Gemini."))
    }

    override suspend fun generateStreamingText(
        prompt: String,
        model: String
    ): Flow<String> = flow {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            emit("Error: GEMINI_API_KEY is not configured in secrets.")
            return@flow
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)), role = "user")
            )
        )

        try {
            val responseBody = apiService.generateContentStream(model = model, apiKey = apiKey, request = request)
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
            responseBody.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line?.trim()
                    if (!currentLine.isNullOrBlank()) {
                        try {
                            val chunk = json.parseToJsonElement(currentLine).jsonObject
                            val text = chunk["candidates"]?.jsonArray
                                ?.getOrNull(0)?.jsonObject
                                ?.get("content")?.jsonObject
                                ?.get("parts")?.jsonArray
                                ?.getOrNull(0)?.jsonObject
                                ?.get("text")?.jsonPrimitive?.content
                            if (text != null) {
                                emit(text)
                            }
                        } catch (_: Exception) {
                            // Non-json or SSE delimiter lines
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("\n[Error: ${extractErrorMessage(e)}]")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun analyzeImage(
        prompt: String,
        imageBase64: String,
        mimeType: String,
        model: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured in secrets."))
            }

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = mimeType, data = imageBase64))
                        ),
                        role = "user"
                    )
                )
            )

            val response = executeWithRateLimitRetry {
                apiService.generateContent(model = model, apiKey = apiKey, request = request)
            }
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("No analysis result returned by Gemini."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(extractErrorMessage(e), e))
        }
    }
}
