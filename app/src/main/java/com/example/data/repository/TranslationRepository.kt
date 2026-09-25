package com.example.data.repository

import com.example.data.local.TranslationDao
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.TranslationHistoryEntity
import com.example.domain.model.Language
import com.example.domain.model.TranslationResult
import com.example.domain.translation.GeminiTranslationProvider
import com.example.domain.translation.GoogleNeuralTranslationProvider
import com.example.domain.translation.LanguageDetector
import com.example.domain.translation.MockTranslationProvider
import com.example.domain.translation.TranslationProvider
import kotlinx.coroutines.flow.Flow

class TranslationRepository(
    private val translationDao: TranslationDao,
    private val userPreferences: UserPreferencesRepository,
    private val billingRepository: BillingRepository
) {
    private val googleProvider = GoogleNeuralTranslationProvider()
    private val geminiProvider = GeminiTranslationProvider()

    val allHistory: Flow<List<TranslationHistoryEntity>> = translationDao.getAllHistory()
    val allFavorites: Flow<List<FavoriteEntity>> = translationDao.getAllFavorites()

    suspend fun translate(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        forceBusinessMode: Boolean? = null
    ): TranslationResult {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return TranslationResult(
                originalText = "",
                translatedText = "",
                detectedSourceLanguage = sourceLang,
                targetLanguage = targetLang,
                error = "No text provided to translate"
            )
        }

        // Check Business Mode
        val isBusinessMode = forceBusinessMode ?: userPreferences.businessModeEnabled.value

        // Select translation provider: Gemini AI if configured, otherwise Google Neural Translation Engine
        val provider: TranslationProvider = if (geminiProvider.isAvailable) {
            geminiProvider
        } else {
            googleProvider
        }

        val result = provider.translate(
            text = cleanText,
            sourceLang = sourceLang,
            targetLang = targetLang,
            isBusinessMode = isBusinessMode
        )

        return result
    }

    suspend fun saveToHistory(result: TranslationResult): Long {
        if (result.originalText.isBlank() || result.translatedText.isBlank()) return -1
        val entity = TranslationHistoryEntity(
            sourceLangCode = result.detectedSourceLanguage.code,
            sourceLangName = result.detectedSourceLanguage.name,
            originalText = result.originalText,
            targetLangCode = result.targetLanguage.code,
            targetLangName = result.targetLanguage.name,
            translatedText = result.translatedText,
            isBusinessMode = result.isBusinessModeApplied
        )
        return translationDao.insertHistory(entity)
    }

    suspend fun deleteHistory(id: Long) {
        translationDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        translationDao.clearAllHistory()
    }

    suspend fun toggleFavorite(
        sourceLang: Language,
        originalText: String,
        targetLang: Language,
        translatedText: String,
        category: String = "General"
    ): Boolean {
        val isFav = translationDao.isFavorite(originalText, targetLang.code)
        if (isFav) {
            translationDao.removeFavoriteByText(originalText, targetLang.code)
            return false
        } else {
            val fav = FavoriteEntity(
                sourceLangCode = sourceLang.code,
                sourceLangName = sourceLang.name,
                originalText = originalText,
                targetLangCode = targetLang.code,
                targetLangName = targetLang.name,
                translatedText = translatedText,
                category = category
            )
            translationDao.insertFavorite(fav)
            return true
        }
    }

    suspend fun isFavorite(originalText: String, targetLangCode: String): Boolean {
        return translationDao.isFavorite(originalText, targetLangCode)
    }

    suspend fun deleteFavorite(id: Long) {
        translationDao.deleteFavoriteById(id)
    }

    suspend fun clearFavorites() {
        translationDao.clearAllFavorites()
    }

    suspend fun deleteAllUserData() {
        translationDao.clearAllHistory()
        translationDao.clearAllFavorites()
        userPreferences.resetAllPreferences()
    }
}
