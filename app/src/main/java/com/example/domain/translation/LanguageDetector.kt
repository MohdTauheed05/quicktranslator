package com.example.domain.translation

import com.example.domain.model.Language

object LanguageDetector {

    fun detectLanguage(text: String): Language {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Language.ENGLISH

        var arabicScriptCount = 0
        var devanagariCount = 0
        var hanziCount = 0
        var hiraganaKatakanaCount = 0
        var hangulCount = 0
        var cyrillicCount = 0
        var bengaliCount = 0
        var latinCount = 0

        var hasUrduSpecificChars = false
        var hasPersianSpecificChars = false

        for (ch in trimmed) {
            val code = ch.code
            when {
                code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0x08A0..0x08FF -> {
                    arabicScriptCount++
                    // Check for Urdu specific characters: ٹ ڈ ڑ ں ے ھ
                    if (ch == 'ٹ' || ch == 'ڈ' || ch == 'ڑ' || ch == 'ں' || ch == 'ے' || ch == 'ھ') {
                        hasUrduSpecificChars = true
                    }
                    // Check for Persian specific characters: پ چ ژ گ
                    if (ch == 'پ' || ch == 'چ' || ch == 'ژ' || ch == 'گ') {
                        hasPersianSpecificChars = true
                    }
                }
                code in 0x0900..0x097F -> devanagariCount++
                code in 0x0980..0x09FF -> bengaliCount++
                code in 0x3040..0x309F || code in 0x30A0..0x30FF -> hiraganaKatakanaCount++
                code in 0x4E00..0x9FFF -> hanziCount++
                code in 0xAC00..0xD7AF || code in 0x1100..0x11FF -> hangulCount++
                code in 0x0400..0x04FF -> cyrillicCount++
                (code in 'a'.code..'z'.code) || (code in 'A'.code..'Z'.code) ||
                ch in "áéíóúàèìòùâêîôûäëïöüñçãõşğıçßÁÉÍÓÚÀÈÌÒÙÂÊÎÔÛÄËÏÖÜÑÇÃÕŞĞIÇ" -> latinCount++
            }
        }

        val totalChars = trimmed.length.coerceAtLeast(1)

        // 1. Arabic Script
        if (arabicScriptCount > 0 && arabicScriptCount >= totalChars / 4) {
            val lower = trimmed.lowercase()
            if (hasUrduSpecificChars || lower.contains("آپ") || lower.contains("کیسے") || lower.contains("شکریہ")) {
                return Language.URDU
            }
            if (hasPersianSpecificChars || lower.contains("شما") || lower.contains("چطورید") || lower.contains("ممنون")) {
                return Language.PERSIAN
            }
            return Language.ARABIC
        }

        // 2. Devanagari (Hindi vs Nepali)
        if (devanagariCount > 0 && devanagariCount >= totalChars / 4) {
            val nepaliKeywords = listOf("कस्तो", "तपाईं", "छौ", "लाई", "गर्छु", "नमस्कार", "धन्यवाद")
            if (nepaliKeywords.any { trimmed.contains(it) }) {
                return Language.NEPALI
            }
            return Language.HINDI
        }

        // 3. Bengali
        if (bengaliCount > 0 && bengaliCount >= totalChars / 4) {
            return Language.BENGALI
        }

        // 4. Japanese / Chinese / Korean
        if (hiraganaKatakanaCount > 0) return Language.JAPANESE
        if (hangulCount > 0) return Language.KOREAN
        if (hanziCount > 0 && hanziCount >= totalChars / 4) return Language.CHINESE

        // 5. Cyrillic (Russian)
        if (cyrillicCount > 0 && cyrillicCount >= totalChars / 4) return Language.RUSSIAN

        // 6. Latin-based detection
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

        // Default Latin to English
        return Language.ENGLISH
    }
}
