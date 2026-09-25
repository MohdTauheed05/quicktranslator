package com.example.domain.translation

import com.example.BuildConfig
import com.example.domain.model.Language
import com.example.domain.model.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiTranslationProvider(
    private val fallbackMockProvider: MockTranslationProvider = MockTranslationProvider()
) : TranslationProvider {

    override val providerId: String = "gemini_ai"
    override val providerName: String = "Google Gemini Neural Engine"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override val isAvailable: Boolean
        get() {
            return try {
                val apiKey = getApiKey()
                apiKey.isNotBlank() && !apiKey.startsWith("MY_GEMINI")
            } catch (e: Exception) {
                false
            }
        }

    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun translate(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        isBusinessMode: Boolean
    ): TranslationResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey.startsWith("MY_GEMINI")) {
            return@withContext fallbackMockProvider.translate(text, sourceLang, targetLang, isBusinessMode)
        }

        val protected = if (isBusinessMode) {
            BusinessTranslator.protectTokens(text)
        } else {
            ProtectedCommercialText(text, emptyMap(), emptyList())
        }

        val prompt = buildString {
            append("You are QuickTranslate, an ultra-fast translation engine.\n")
            append("Translate the following message into ${targetLang.name} (${targetLang.nativeName}).\n")
            if (sourceLang != Language.AUTO) {
                append("The source language is ${sourceLang.name}.\n")
            }
            if (isBusinessMode) {
                append("CRITICAL BUSINESS MODE INSTRUCTIONS:\n")
                append("- Preserve all exact quantities, prices, currency, Incoterms (FOB/CIF), SKU, PO numbers, and percentages.\n")
                append("- Tokens enclosed in '___COMM_...___' MUST be preserved exactly as written without translating or altering them.\n")
            }
            append("Return ONLY the final translated text without any explanation, quotes, or markdown wrappers.\n\n")
            append("Text to translate:\n")
            append(protected.maskedText)
        }

        try {
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext fallbackMockProvider.translate(text, sourceLang, targetLang, isBusinessMode)
            }

            val responseString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawTranslation = parts?.optJSONObject(0)?.optString("text")?.trim()

            if (rawTranslation.isNullOrBlank()) {
                return@withContext fallbackMockProvider.translate(text, sourceLang, targetLang, isBusinessMode)
            }

            val finalTranslation = if (isBusinessMode && protected.tokenMap.isNotEmpty()) {
                BusinessTranslator.restoreTokens(rawTranslation, protected.tokenMap)
            } else {
                rawTranslation
            }

            TranslationResult(
                originalText = text,
                translatedText = finalTranslation,
                detectedSourceLanguage = sourceLang,
                targetLanguage = targetLang,
                isDemo = false,
                providerName = providerName,
                isBusinessModeApplied = isBusinessMode && protected.extractedEntities.isNotEmpty(),
                preservedTokens = protected.extractedEntities
            )
        } catch (e: Exception) {
            fallbackMockProvider.translate(text, sourceLang, targetLang, isBusinessMode)
        }
    }
}
