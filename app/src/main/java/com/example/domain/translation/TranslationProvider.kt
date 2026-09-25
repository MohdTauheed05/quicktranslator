package com.example.domain.translation

import com.example.domain.model.Language
import com.example.domain.model.TranslationResult

interface TranslationProvider {
    val providerId: String
    val providerName: String
    val isAvailable: Boolean
    
    suspend fun translate(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        isBusinessMode: Boolean
    ): TranslationResult
}
