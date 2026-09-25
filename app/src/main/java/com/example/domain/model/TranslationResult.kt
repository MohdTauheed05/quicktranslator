package com.example.domain.model

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val detectedSourceLanguage: Language,
    val targetLanguage: Language,
    val isDemo: Boolean = false,
    val providerName: String = "QuickTranslate Engine",
    val isBusinessModeApplied: Boolean = false,
    val preservedTokens: List<String> = emptyList(),
    val error: String? = null
)
