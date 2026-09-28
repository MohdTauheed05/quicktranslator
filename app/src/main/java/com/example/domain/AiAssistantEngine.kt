package com.example.domain.ai

import com.example.BuildConfig
import com.example.domain.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AiTone(val title: String, val iconLabel: String, val promptInstruction: String) {
    PROFESSIONAL("Business", "💼", "Rewrite this message in a highly professional, polite, and corporate tone suitable for international business trade and WhatsApp negotiations:"),
    FRIENDLY("Friendly", "💬", "Rewrite this message in a warm, natural, friendly, and approachable conversational tone:"),
    GRAMMAR_FIX("Grammar", "✨", "Fix all grammatical errors, typos, and awkward punctuation in this text while keeping the original meaning:"),
    CONCISE("Concise", "⚡", "Condense and shorten this message to be crisp, direct, and quick to read on mobile:"),
    ELABORATE("Polite", "📝", "Expand this message politely with complete sentences, courteous etiquette, and clear professional context:")
}

object AiAssistantEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = (field.get(null) as? String) ?: ""
            if (key.isBlank() || key.startsWith("MY_GEMINI")) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Rewrites user text according to the selected AI tone using Gemini 3.5 Flash,
     * with smart fallback to offline transformation if no API key is set.
     */
    suspend fun rewrite(text: String, tone: AiTone, targetLang: Language? = null): String = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isBlank()) return@withContext ""

        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val prompt = buildString {
                    append("You are an intelligent messaging assistant.\n")
                    append("${tone.promptInstruction}\n\n")
                    if (targetLang != null && targetLang != Language.AUTO) {
                        append("Ensure the final output is in ${targetLang.name} (${targetLang.nativeName}).\n")
                    }
                    append("Return ONLY the final rewritten text without preamble, quotes, bullet points, or markdown formatting.\n\n")
                    append("Original text:\n\"$clean\"")
                }

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
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
                    val content = candidate?.optJSONObject("content")
                    val partText = content?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.trim()
                    if (!partText.isNullOrBlank()) {
                        return@withContext partText.removeSurrounding("\"")
                    }
                }
            } catch (e: Exception) {
                // Fall back to offline rule-based transformation
            }
        }

        // Smart offline rule-based transform
        return@withContext offlineRewrite(clean, tone)
    }

    /**
     * Generates 3 contextual AI smart replies for a copied chat message.
     */
    suspend fun generateSmartReplies(incomingMessage: String): List<String> = withContext(Dispatchers.IO) {
        val clean = incomingMessage.trim()
        if (clean.isBlank()) return@withContext emptyList()

        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    You are an intelligent WhatsApp AI assistant.
                    Given this received message: "$clean"
                    Generate exactly 3 short, natural, helpful quick replies.
                    Format: Return exactly 3 lines, one reply per line. No numbers, no bullet points, no quotes.
                """.trimIndent()

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
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
                    val content = candidate?.optJSONObject("content")
                    val partText = content?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.trim()
                    if (!partText.isNullOrBlank()) {
                        val lines = partText.lines().map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() }
                        if (lines.isNotEmpty()) return@withContext lines.take(3)
                    }
                }
            } catch (e: Exception) {
                // Fall back to offline smart replies
            }
        }

        return@withContext listOf(
            "Thank you, received and noted!",
            "I will review and get back to you shortly.",
            "Could you please share more details regarding this?"
        )
    }

    private fun offlineRewrite(text: String, tone: AiTone): String {
        return when (tone) {
            AiTone.PROFESSIONAL -> {
                "Dear Partner, with reference to our discussion, $text. Please let us know if this meets your requirements."
            }
            AiTone.FRIENDLY -> {
                "Hey! Just wanted to share that $text. Hope you're doing well! 😊"
            }
            AiTone.GRAMMAR_FIX -> {
                val capitalized = text.trim().replaceFirstChar { it.uppercase() }
                if (capitalized.endsWith(".") || capitalized.endsWith("!") || capitalized.endsWith("?")) {
                    capitalized
                } else {
                    "$capitalized."
                }
            }
            AiTone.CONCISE -> {
                text.split("\n", ".").firstOrNull { it.isNotBlank() }?.trim() ?: text
            }
            AiTone.ELABORATE -> {
                "Thank you for reaching out. We would like to inform you that $text. We remain at your disposal for any further assistance."
            }
        }
    }
}
