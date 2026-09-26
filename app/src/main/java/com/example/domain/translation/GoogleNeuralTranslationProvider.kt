package com.example.domain.translation

import com.example.domain.model.Language
import com.example.domain.model.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GoogleNeuralTranslationProvider : TranslationProvider {

    override val providerId: String = "google_neural"
    override val providerName: String = "Google Neural Engine"
    override val isAvailable: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    override suspend fun translate(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        isBusinessMode: Boolean
    ): TranslationResult = withContext(Dispatchers.IO) {
        val cleanInput = text.trim()
        if (cleanInput.isEmpty()) {
            return@withContext TranslationResult(
                originalText = "",
                translatedText = "",
                detectedSourceLanguage = sourceLang,
                targetLanguage = targetLang,
                isDemo = false
            )
        }

        val normalizedInput = normalizeRomanizedEntities(cleanInput)

        val protectedData = if (isBusinessMode) {
            BusinessTranslator.protectTokens(normalizedInput)
        } else {
            ProtectedCommercialText(normalizedInput, emptyMap(), emptyList())
        }

        val sourceCode = if (sourceLang == Language.AUTO) "auto" else sourceLang.code
        val targetCode = targetLang.code

        try {
            val encodedQuery = URLEncoder.encode(protectedData.maskedText, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=at&sl=$sourceCode&tl=$targetCode&dt=t&q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AndroidTranslate/5.3.0.RC02.130475354-53000263 5.1 phone TRANSLATE_OPM5_TEST_1")
                .header("Accept", "*/*")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string().orEmpty()
                val parsed = parseTranslationResponse(bodyString)

                if (parsed != null && parsed.translatedText.isNotBlank()) {
                    val finalTranslation = if (isBusinessMode && protectedData.tokenMap.isNotEmpty()) {
                        BusinessTranslator.restoreTokens(parsed.translatedText, protectedData.tokenMap)
                    } else {
                        parsed.translatedText
                    }

                    val detectedLang = if (sourceLang == Language.AUTO && parsed.detectedSourceCode.isNotBlank()) {
                        Language.fromCode(parsed.detectedSourceCode)
                    } else if (sourceLang != Language.AUTO) {
                        sourceLang
                    } else {
                        LanguageDetector.detectLanguage(cleanInput)
                    }

                    return@withContext TranslationResult(
                        originalText = cleanInput,
                        translatedText = finalTranslation,
                        detectedSourceLanguage = detectedLang,
                        targetLanguage = targetLang,
                        isDemo = false,
                        providerName = providerName,
                        isBusinessModeApplied = isBusinessMode && protectedData.extractedEntities.isNotEmpty(),
                        preservedTokens = protectedData.extractedEntities
                    )
                }
            }
        } catch (e: Exception) {
            // Network or parsing error, proceed to fallback
        }

        // Secondary Online Fallback: MyMemory API
        try {
            val srcParam = if (sourceLang == Language.AUTO) LanguageDetector.detectLanguage(cleanInput).code else sourceLang.code
            val langPair = "$srcParam|$targetCode"
            val encodedQuery = URLEncoder.encode(protectedData.maskedText, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=$langPair"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "QuickTranslateApp/1.0 (Android)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string().orEmpty()
                val json = org.json.JSONObject(bodyString)
                val respData = json.optJSONObject("responseData")
                val translated = respData?.optString("translatedText")

                if (!translated.isNullOrBlank() && !translated.startsWith("MYMEMORY WARNING")) {
                    val finalTranslation = if (isBusinessMode && protectedData.tokenMap.isNotEmpty()) {
                        BusinessTranslator.restoreTokens(translated, protectedData.tokenMap)
                    } else {
                        translated
                    }

                    val detectedLang = if (sourceLang == Language.AUTO) {
                        Language.fromCode(srcParam)
                    } else {
                        sourceLang
                    }

                    return@withContext TranslationResult(
                        originalText = cleanInput,
                        translatedText = finalTranslation,
                        detectedSourceLanguage = detectedLang,
                        targetLanguage = targetLang,
                        isDemo = false,
                        providerName = "Global Neural Network",
                        isBusinessModeApplied = isBusinessMode && protectedData.extractedEntities.isNotEmpty(),
                        preservedTokens = protectedData.extractedEntities
                    )
                }
            }
        } catch (e: Exception) {
            // Secondary fallback failed
        }

        // Local Offline phrase matching fallback
        val offlineFallback = MockTranslationProvider()
        offlineFallback.translate(cleanInput, sourceLang, targetLang, isBusinessMode)
    }

    private data class ParsedResponse(
        val translatedText: String,
        val detectedSourceCode: String
    )

    private fun parseTranslationResponse(rawJson: String): ParsedResponse? {
        return try {
            val rootArray = JSONArray(rawJson)
            val segmentsArray = rootArray.optJSONArray(0) ?: return null

            val sb = StringBuilder()
            for (i in 0 until segmentsArray.length()) {
                val segment = segmentsArray.optJSONArray(i)
                if (segment != null) {
                    val translatedPart = segment.optString(0, "")
                    if (translatedPart != "null") {
                        sb.append(translatedPart)
                    }
                }
            }

            var detectedCode = ""
            if (rootArray.length() > 2) {
                detectedCode = rootArray.optString(2, "")
            }

            ParsedResponse(
                translatedText = sb.toString().trim(),
                detectedSourceCode = detectedCode
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private val GULF_AND_GLOBAL_PLACES = listOf(
            "ajman" to "Ajman",
            "dubai" to "Dubai",
            "sharjah" to "Sharjah",
            "abu dhabi" to "Abu Dhabi",
            "abudhabi" to "Abu Dhabi",
            "al ain" to "Al Ain",
            "alain" to "Al Ain",
            "deira" to "Deira",
            "bur dubai" to "Bur Dubai",
            "burdubai" to "Bur Dubai",
            "fujairah" to "Fujairah",
            "ras al khaimah" to "Ras Al Khaimah",
            "rak" to "Ras Al Khaimah",
            "umm al quwain" to "Umm Al Quwain",
            "uaq" to "Umm Al Quwain",
            "jumeirah" to "Jumeirah",
            "karama" to "Karama",
            "satwa" to "Satwa",
            "riyadh" to "Riyadh",
            "jeddah" to "Jeddah",
            "dammam" to "Dammam",
            "khobar" to "Khobar",
            "mecca" to "Mecca",
            "makkah" to "Makkah",
            "medina" to "Medina",
            "madinah" to "Madinah",
            "doha" to "Doha",
            "muscat" to "Muscat",
            "kuwait" to "Kuwait",
            "bahrain" to "Bahrain",
            "manama" to "Manama",
            "delhi" to "Delhi",
            "mumbai" to "Mumbai",
            "karachi" to "Karachi",
            "lahore" to "Lahore",
            "islamabad" to "Islamabad",
            "dhaka" to "Dhaka",
            "kathmandu" to "Kathmandu"
        )

        fun normalizeRomanizedEntities(input: String): String {
            var text = input
            // 1. Capitalize proper nouns & city names so neural model recognizes named entities
            for ((query, properName) in GULF_AND_GLOBAL_PLACES) {
                val pattern = "(?i)\\b${java.util.regex.Pattern.quote(query)}\\b".toRegex()
                text = text.replace(pattern, properName)
            }
            // 2. Normalize informal Hinglish time contractions e.g. standalone "aj" -> "aaj" (today in Hindi/Urdu)
            text = text.replace("(?i)\\baj\\b".toRegex(), "aaj")
            return text
        }
    }
}
