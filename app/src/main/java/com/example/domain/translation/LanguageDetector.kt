package com.example.domain.translation

import com.example.domain.model.Language

object LanguageDetector {

    fun detectLanguage(text: String): Language {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Language.ENGLISH

        val totalChars = trimmed.length
        var arabicCount = 0
        var devanagariCount = 0
        var cjkCount = 0
        var cyrillicCount = 0

        for (ch in trimmed) {
            val block = Character.UnicodeBlock.of(ch)
            when {
                block == Character.UnicodeBlock.ARABIC ||
                block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
                block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
                block == Character.UnicodeBlock.ARABIC_SUPPLEMENT -> {
                    arabicCount++
                }
                block == Character.UnicodeBlock.DEVANAGARI -> {
                    devanagariCount++
                }
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                block == Character.UnicodeBlock.HIRAGANA ||
                block == Character.UnicodeBlock.KATAKANA ||
                block == Character.UnicodeBlock.HANGUL_SYLLABLES -> {
                    cjkCount++
                }
                block == Character.UnicodeBlock.CYRILLIC ||
                block == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY -> {
                    cyrillicCount++
                }
            }
        }

        // 1. Arabic script (Arabic or Urdu)
        if (arabicCount > 0 && arabicCount >= totalChars / 4) {
            val urduSpecificChars = listOf('ٹ', 'ڈ', 'ڑ', 'ں', 'ے', 'ہ', 'ھ', 'چ', 'پ', 'ژ', 'گ')
            val hasUrduChar = trimmed.any { it in urduSpecificChars }
            return if (hasUrduChar) Language.URDU else Language.ARABIC
        }

        // 2. Devanagari script (Hindi or Nepali)
        if (devanagariCount > 0 && devanagariCount >= totalChars / 4) {
            val nepaliMarkers = listOf("छ", "छन्", "थियो", "गर्नुहोस्", "तपाईं", "होला", "भयो")
            val isNepali = nepaliMarkers.any { trimmed.contains(it) }
            return if (isNepali) Language.NEPALI else Language.HINDI
        }

        // 3. Chinese
        if (cjkCount > 0 && cjkCount >= totalChars / 4) {
            return Language.CHINESE
        }

        // 4. Cyrillic (Russian)
        if (cyrillicCount > 0 && cyrillicCount >= totalChars / 4) {
            return Language.RUSSIAN
        }

        // 5. Latin-based detection
        val lowerText = trimmed.lowercase()
        val words = lowerText.split(Regex("[\\s,.;:!?\"'()\\-]+")).filter { it.isNotBlank() }

        // French
        if (trimmed.any { it in "éèêëàâùûçœ" } ||
            words.any { it in listOf("bonjour", "salut", "merci", "comment", "vous", "allez", "oui", "non", "avec", "pour", "c'est") }) {
            return Language.FRENCH
        }

        // Spanish
        if (trimmed.contains("¿") || trimmed.contains("¡") || trimmed.contains("ñ") ||
            words.any { it in listOf("hola", "cómo", "como", "estás", "estas", "gracias", "por", "favor", "buenos", "días", "dias", "bien") }) {
            return Language.SPANISH
        }

        // German
        if (trimmed.any { it in "äöüß" } ||
            words.any { it in listOf("hallo", "guten", "tag", "wie", "geht", "danke", "bitte", "ja", "nein", "und") }) {
            return Language.GERMAN
        }

        // Portuguese
        if (trimmed.any { it in "ãõ" } ||
            words.any { it in listOf("olá", "ola", "obrigado", "obrigada", "você", "voce", "tudo", "bom", "como", "vai") }) {
            return Language.PORTUGUESE
        }

        // Turkish
        if (trimmed.any { it in "ğış" } ||
            words.any { it in listOf("merhaba", "nasılsın", "nasilsin", "teşekkürler", "tesekkurler", "evet", "hayır", "lütfen") }) {
            return Language.TURKISH
        }

        // Indonesian / Malay
        if (words.any { it in listOf("selamat", "pagi", "terima", "kasih", "kabar", "apa", "khabar", "bagaimana", "sama-sama") }) {
            return Language.INDONESIAN
        }

        // Italian
        if (words.any { it in listOf("ciao", "grazie", "come", "stai", "buongiorno", "prego", "per", "favore") }) {
            return Language.ITALIAN
        }

        // Roman Hindi / Hinglish (e.g. "kaise ho", "khana khaya", "aj mujhe ajman jaana hai")
        val romanHindiWords = listOf(
            "kaise", "kaisa", "kaisi", "kya", "kaha", "kahan", "khana", "khaya", "kahay",
            "rahe", "raha", "rahi", "theek", "thik", "accha", "achha", "bhai", "dost",
            "aap", "tum", "hum", "karo", "kare", "karna", "bolo", "batao", "sun", "chalo",
            "jaldi", "shukriya", "namaste", "paisa", "kitna", "kitne", "mera", "meri", "mere",
            "tera", "teri", "tere", "apna", "apni", "apne", "bahut", "bohot", "nahi", "haan",
            "mujhe", "tujhe", "tumhe", "aapko", "humko", "jaana", "jana", "aana", "dena", "lena",
            "milna", "hai", "hain", "ho", "hu", "aj", "aaj", "kal", "parso", "ab", "abhi",
            "kidhar", "idhar", "udhar", "kyu", "kyun", "kuch", "baat", "dekh"
        )
        if (words.any { it in romanHindiWords }) {
            return Language.HINDI
        }

        // Default Latin to English
        return Language.ENGLISH
    }
}
