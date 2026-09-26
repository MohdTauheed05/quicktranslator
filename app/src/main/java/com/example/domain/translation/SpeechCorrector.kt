package com.example.domain.translation

object SpeechCorrector {

    // Common phonetic confusions when speaking English/Trade words in Hindi/Urdu voice recognition
    private val PHONETIC_CORRECTIONS = listOf(
        // Brand & Trade terms
        Regex("(?i)\\bvinod\\b") to "Venol",
        Regex("(?i)\\bvenod\\b") to "Venol",
        Regex("(?i)\\bvinol\\b") to "Venol",
        Regex("(?i)\\bvenal\\b") to "Venol",

        // UAE / Gulf locations
        Regex("(?i)\\b(?:aj\\s+man|aaj\\s+man|aazman)\\b") to "Ajman",
        Regex("(?i)\\b(?:shar\\s+jah|sarjah)\\b") to "Sharjah",
        Regex("(?i)\\b(?:abu\\s+dabi|abudabi|abu\\s+dhabhi)\\b") to "Abu Dhabi",
        Regex("(?i)\\b(?:al\\s+en|al\\s+ayan|alain)\\b") to "Al Ain",
        Regex("(?i)\\b(?:deera|dera)\\b") to "Deira",
        Regex("(?i)\\b(?:bur\\s*dubai|bardubai)\\b") to "Bur Dubai",
        Regex("(?i)\\b(?:ras\\s*al\\s*khema|rasal\\s*kaimah|rak)\\b") to "Ras Al Khaimah",
        Regex("(?i)\\b(?:fujera|fujeirah)\\b") to "Fujairah",
        Regex("(?i)\\b(?:uaq|um\\s*al\\s*quwain)\\b") to "Umm Al Quwain",

        // Commercial terms
        Regex("(?i)\\bem\\s*o\\s*kyu\\b") to "MOQ",
        Regex("(?i)\\bsee\\s*i\\s*ef\\b") to "CIF",
        Regex("(?i)\\bef\\s*o\\s*bee\\b") to "FOB"
    )

    /**
     * Inspects all speech recognition candidates, applies phonetic trade corrections,
     * and returns the best matched string along with alternative candidate suggestions.
     */
    fun processSpeechResults(candidates: List<String>): Pair<String, List<String>> {
        if (candidates.isEmpty()) return Pair("", emptyList())

        // 1. Check if any alternate candidate directly contains a recognized brand (e.g. "Venol")
        var chosenText = candidates.first()
        for (candidate in candidates) {
            if (candidate.contains("venol", ignoreCase = true) ||
                candidate.contains("ajman", ignoreCase = true) ||
                candidate.contains("sharjah", ignoreCase = true) ||
                candidate.contains("abu dhabi", ignoreCase = true)) {
                chosenText = candidate
                break
            }
        }

        // 2. Apply phonetic corrections to the chosen text
        var corrected = chosenText
        for ((regex, replacement) in PHONETIC_CORRECTIONS) {
            corrected = corrected.replace(regex, replacement)
        }

        // 3. Generate distinct suggestions from remaining candidates
        val alternativeSuggestions = mutableListOf<String>()
        if (corrected != chosenText) {
            alternativeSuggestions.add(chosenText)
        }
        for (candidate in candidates) {
            var candCorrected = candidate
            for ((regex, replacement) in PHONETIC_CORRECTIONS) {
                candCorrected = candCorrected.replace(regex, replacement)
            }
            if (candCorrected !in alternativeSuggestions && candCorrected.lowercase() != corrected.lowercase()) {
                alternativeSuggestions.add(candCorrected)
            }
            if (alternativeSuggestions.size >= 3) break
        }

        return Pair(corrected, alternativeSuggestions)
    }
}
