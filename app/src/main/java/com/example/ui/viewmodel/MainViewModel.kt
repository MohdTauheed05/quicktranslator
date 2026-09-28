package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.TranslationHistoryEntity
import com.example.data.repository.BillingRepository
import com.example.data.repository.ThemeMode
import com.example.data.repository.TranslationRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.model.Language
import com.example.domain.model.TranslationResult
import com.example.domain.tts.TtsManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val translationRepository: TranslationRepository,
    private val userPreferences: UserPreferencesRepository,
    private val billingRepository: BillingRepository,
    private val ttsManager: TtsManager
) : ViewModel() {

    val targetLanguage = userPreferences.targetLanguage
    val sourceLanguage = userPreferences.sourceLanguage
    val businessModeEnabled = userPreferences.businessModeEnabled
    val autoClipboardEnabled = userPreferences.autoClipboardEnabled
    val floatingBubbleEnabled = userPreferences.floatingBubbleEnabled
    val themeMode = userPreferences.themeMode
    val hasCompletedOnboarding = userPreferences.hasCompletedOnboarding

    val history: StateFlow<List<TranslationHistoryEntity>> =
        translationRepository.allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favorites: StateFlow<List<FavoriteEntity>> =
        translationRepository.allFavorites.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Aliases for screen compatibility (FavoritesScreen and HistoryScreen)
    val favoritesList: StateFlow<List<FavoriteEntity>> = favorites
    val historyList: StateFlow<List<TranslationHistoryEntity>> = history

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _currentResult = MutableStateFlow<TranslationResult?>(null)
    val currentResult: StateFlow<TranslationResult?> = _currentResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSavedCurrent = MutableStateFlow(false)
    val isSavedCurrent: StateFlow<Boolean> = _isSavedCurrent.asStateFlow()

    private val _isCurrentFavorite = MutableStateFlow(false)
    val isCurrentFavorite: StateFlow<Boolean> = _isCurrentFavorite.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _showOverlayPermissionDialog = MutableStateFlow(false)
    val showOverlayPermissionDialog: StateFlow<Boolean> = _showOverlayPermissionDialog.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
        if (_errorMessage.value != null) _errorMessage.value = null
    }

    fun processSharedText(text: String) {
        _inputText.value = text
        translate(text)
    }

    fun onResumeCheckBubble(context: Context) {
        // Pure keyboard companion mode
    }

    fun toggleFloatingBubble(context: Context) {
        // Pure keyboard mode
    }

    fun dismissOverlayPermissionDialog() {
        _showOverlayPermissionDialog.value = false
    }

    fun requestOverlayPermission(context: Context) {
        _showOverlayPermissionDialog.value = false
    }

    fun setSourceLanguage(language: Language) {
        userPreferences.setSourceLanguage(language)
        if (_inputText.value.isNotBlank()) {
            translate(_inputText.value)
        }
    }

    fun setTargetLanguage(language: Language) {
        userPreferences.setTargetLanguage(language)
        if (_inputText.value.isNotBlank()) {
            translate(_inputText.value)
        }
    }

    fun swapLanguages() {
        val currentSource = sourceLanguage.value
        val currentTarget = targetLanguage.value
        if (currentSource == Language.AUTO) {
            userPreferences.setSourceLanguage(currentTarget)
            userPreferences.setTargetLanguage(Language.ENGLISH)
        } else {
            userPreferences.setSourceLanguage(currentTarget)
            userPreferences.setTargetLanguage(currentSource)
        }
        val currentResult = _currentResult.value
        if (currentResult != null && currentResult.translatedText.isNotBlank()) {
            _inputText.value = currentResult.translatedText
            translate(currentResult.translatedText)
        }
    }

    fun translate(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isSavedCurrent.value = false
            _isCurrentFavorite.value = false

            try {
                val result = translationRepository.translate(
                    text = clean,
                    sourceLang = sourceLanguage.value,
                    targetLang = targetLanguage.value
                )
                _currentResult.value = result
                _isSavedCurrent.value = true
                _isCurrentFavorite.value = translationRepository.isFavorite(result.originalText, result.targetLanguage.code)
            } catch (e: Exception) {
                _errorMessage.value = "Translation error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearInput() {
        _inputText.value = ""
        _currentResult.value = null
        _errorMessage.value = null
    }

    fun speak(text: String, language: Language) {
        ttsManager.speak(text, language)
    }

    fun speakText(text: String, language: Language) {
        ttsManager.speak(text, language)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun toggleCurrentFavorite() {
        val result = _currentResult.value ?: return
        toggleFavorite(result)
    }

    fun toggleFavorite(result: TranslationResult) {
        viewModelScope.launch {
            val nowFav = translationRepository.toggleFavorite(
                sourceLang = result.detectedSourceLanguage,
                originalText = result.originalText,
                targetLang = result.targetLanguage,
                translatedText = result.translatedText
            )
            _isCurrentFavorite.value = nowFav
            _toastEvent.emit(if (nowFav) "Added to Favorites" else "Removed from Favorites")
        }
    }

    fun saveCurrentTranslation() {
        val result = _currentResult.value ?: return
        viewModelScope.launch {
            try {
                translationRepository.saveToHistory(result)
                _isSavedCurrent.value = true
                _toastEvent.emit("Translation saved to history")
            } catch (e: Exception) {
                _toastEvent.emit("Failed to save translation")
            }
        }
    }

    fun toggleBusinessMode() {
        val current = businessModeEnabled.value
        userPreferences.setBusinessModeEnabled(!current)
        if (_inputText.value.isNotBlank()) {
            translate(_inputText.value)
        }
    }

    fun toggleAutoClipboard() {
        val current = autoClipboardEnabled.value
        userPreferences.setAutoClipboardEnabled(!current)
    }

    fun setThemeMode(mode: ThemeMode) {
        userPreferences.setThemeMode(mode)
    }

    fun completeOnboarding(chosenTargetLang: Language) {
        userPreferences.setTargetLanguage(chosenTargetLang)
        userPreferences.setOnboardingCompleted(true)
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            translationRepository.deleteHistory(id)
            _toastEvent.emit("Item deleted")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            translationRepository.clearHistory()
            _toastEvent.emit("History cleared")
        }
    }

    fun deleteFavoriteItem(id: Long) {
        viewModelScope.launch {
            translationRepository.deleteFavorite(id)
            _toastEvent.emit("Favorite removed")
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            translationRepository.clearFavorites()
            _toastEvent.emit("All favorites cleared")
        }
    }

    fun deleteAllUserData() {
        viewModelScope.launch {
            translationRepository.deleteAllUserData()
            _toastEvent.emit("All user data cleared")
        }
    }

    class Factory(
        private val translationRepository: TranslationRepository,
        private val userPreferences: UserPreferencesRepository,
        private val billingRepository: BillingRepository,
        private val ttsManager: TtsManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(
                    translationRepository,
                    userPreferences,
                    billingRepository,
                    ttsManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
