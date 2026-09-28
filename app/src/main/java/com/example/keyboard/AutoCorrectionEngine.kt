package com.example.keyboard

object AutoCorrectionEngine {

    // Common typo mapping -> correct word
    private val typoMap = mapOf(
        "teh" to "the",
        "recieve" to "receive",
        "recieved" to "received",
        "recieving" to "receiving",
        "seperate" to "separate",
        "definately" to "definitely",
        "definitly" to "definitely",
        "occured" to "occurred",
        "untill" to "until",
        "wierd" to "weird",
        "alot" to "a lot",
        "tommorow" to "tomorrow",
        "tomorow" to "tomorrow",
        "goverment" to "government",
        "beleive" to "believe",
        "calender" to "calendar",
        "neccessary" to "necessary",
        "necesary" to "necessary",
        "arguement" to "argument",
        "truely" to "truly",
        "publically" to "publicly",
        "accommodate" to "accommodate",
        "accomodate" to "accommodate",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "didnt" to "didn't",
        "doesnt" to "doesn't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "hadnt" to "hadn't",
        "wouldnt" to "wouldn't",
        "couldnt" to "couldn't",
        "shouldnt" to "shouldn't",
        "im" to "I'm",
        "ive" to "I've",
        "ill" to "I'll",
        "youre" to "you're",
        "theyre" to "they're",
        "weve" to "we've",
        "thats" to "that's",
        "whats" to "what's",
        "wheres" to "where's",
        "hows" to "how's",
        "lets" to "let's",
        "ur" to "your",
        "u" to "you",
        "r" to "are",
        "pls" to "please",
        "plz" to "please",
        "thx" to "thanks",
        "ty" to "thank you",
        "np" to "no problem",
        "omw" to "on my way",
        "brb" to "be right back",
        "idk" to "I don't know",
        "tbh" to "to be honest",
        "fyi" to "FYI",
        "asap" to "ASAP",
        "goodmorning" to "good morning",
        "goodnight" to "good night",
        "hav" to "have",
        "helllo" to "hello",
        "helo" to "hello",
        "gud" to "good",
        "tht" to "that",
        "wat" to "what",
        "wher" to "where",
        "wen" to "when",
        "lyk" to "like",
        "bcoz" to "because",
        "bcuz" to "because",
        "becoz" to "because",
        "wud" to "would",
        "cud" to "could",
        "shud" to "should"
    )

    // Frequent common dictionary words for prefix matching and completions
    private val commonWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "great", "hello", "please", "thanks", "thank", "message", "price", "order", "delivery",
        "meeting", "confirm", "available", "invoice", "payment", "tomorrow", "today", "yesterday",
        "urgent", "contact", "address", "company", "shipment", "warehouse", "product", "quality"
    )

    /**
     * Given the currently typed prefix/word, returns a list of up to 3 suggestions:
     * - Middle (Index 1 or 0): The primary auto-correction or completion candidate
     * - Left: Literal typed text
     * - Right: Alternative prediction / completion
     */
    fun getSuggestions(rawWord: String): List<String> {
        val word = rawWord.trim()
        if (word.isEmpty()) return emptyList()

        val lower = word.lowercase()
        val isCapitalized = word.first().isUpperCase()

        // 1. Direct typo check
        val typoReplacement = typoMap[lower]
        if (typoReplacement != null) {
            val formattedCorrection = if (isCapitalized) typoReplacement.replaceFirstChar { it.uppercase() } else typoReplacement
            return listOf(word, formattedCorrection, formattedCorrection + "!")
        }

        // 2. Prefix completion from common vocabulary
        val matches = commonWords.filter { it.startsWith(lower) && it != lower }
            .take(2)
            .map { if (isCapitalized) it.replaceFirstChar { char -> char.uppercase() } else it }

        if (matches.isNotEmpty()) {
            return if (matches.size == 1) {
                listOf(word, matches[0], word.uppercase())
            } else {
                listOf(word, matches[0], matches[1])
            }
        }

        // 3. Fallback: literal word and simple casings
        return listOf(word, word.replaceFirstChar { it.uppercase() }, word.uppercase())
    }

    /**
     * Checks if pressing Space should trigger an auto-correction.
     * Returns the corrected word if one exists, or null if no correction is needed.
     */
    fun getAutoCorrectionForSpace(rawWord: String): String? {
        val word = rawWord.trim()
        if (word.length < 2) return null
        val lower = word.lowercase()
        val isCapitalized = word.first().isUpperCase()

        val typoReplacement = typoMap[lower]
        if (typoReplacement != null) {
            return if (isCapitalized) typoReplacement.replaceFirstChar { it.uppercase() } else typoReplacement
        }
        return null
    }
}
