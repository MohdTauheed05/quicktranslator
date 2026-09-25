package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import com.example.service.FloatingBubbleService
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

    val historyList: StateFlow<List<TranslationHistoryEntity>> = translationRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesList: StateFlow<List<FavoriteEntity>> = translationRepository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            val detected = _currentResult.value?.detectedSourceLanguage ?: Language.ENGLISH
            userPreferences.setSourceLanguage(currentTarget)
            userPreferences.setTargetLanguage(detected)
        } else {
            userPreferences.setSourceLanguage(currentTarget)
            userPreferences.setTargetLanguage(currentSource)
        }

        val result = _currentResult.value
        if (result != null && result.translatedText.isNotBlank()) {
            _inputText.value = result.translatedText
            translate(result.translatedText)
        }
    }

    fun translate(textToTranslate: String? = null) {
        val text = (textToTranslate ?: _inputText.value).trim()
        if (text.isBlank()) {
            _errorMessage.value = "Please enter or paste text to translate."
            return
        }

        _inputText.value = text
        _isLoading.value = true
        _errorMessage.value = null
        _isSavedCurrent.value = false

        viewModelScope.launch {
            try {
                val result = translationRepository.translate(
                    text = text,
                    sourceLang = sourceLanguage.value,
                    targetLang = targetLanguage.value
                )
                _currentResult.value = result
                _isCurrentFavorite.value = translationRepository.isFavorite(
                    result.originalText,
                    result.targetLanguage.code
                )
            } catch (e: Exception) {
                _errorMessage.value = "Translation failed. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processSharedText(sharedText: String) {
        if (sharedText.isNotBlank()) {
            _inputText.value = sharedText
            translate(sharedText)
        }
    }

    fun saveCurrentTranslation() {
        val result = _currentResult.value ?: return
        viewModelScope.launch {
            translationRepository.saveToHistory(result)
            _isSavedCurrent.value = true
            _toastEvent.emit("Saved to History")
        }
    }

    fun toggleCurrentFavorite() {
        val result = _currentResult.value ?: return
        viewModelScope.launch {
            val nowFav = translationRepository.toggleFavorite(
                sourceLang = result.detectedSourceLanguage,
                originalText = result.originalText,
                targetLang = result.targetLanguage,
                translatedText = result.translatedText,
                category = if (result.isBusinessModeApplied) "Business" else "General"
            )
            _isCurrentFavorite.value = nowFav
            _toastEvent.emit(if (nowFav) "Added to Favorites" else "Removed from Favorites")
        }
    }

    fun speak(text: String, language: Language) {
        ttsManager.speak(text, language)
    }

    fun stopSpeaking() {
        ttsManager.stop()
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

    fun toggleFloatingBubble(context: Context) {
        val isCurrentlyEnabled = floatingBubbleEnabled.value
        if (isCurrentlyEnabled) {
            // Turn off
            FloatingBubbleService.stop(context)
            userPreferences.setFloatingBubbleEnabled(false)
            viewModelScope.launch { _toastEvent.emit("Floating Bubble disabled") }
        } else {
            // Check permission to draw over other apps
            if (Settings.canDrawOverlays(context)) {
                FloatingBubbleService.start(context)
                userPreferences.setFloatingBubbleEnabled(true)
                viewModelScope.launch { _toastEvent.emit("Floating Assistant activated! Look for the bubble over WhatsApp") }
            } else {
                _showOverlayPermissionDialog.value = true
            }
        }
    }

    fun dismissOverlayPermissionDialog() {
        _showOverlayPermissionDialog.value = false
    }

    fun requestOverlayPermission(context: Context) {
        _showOverlayPermissionDialog.value = false
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun onResumeCheckBubble(context: Context) {
        if (floatingBubbleEnabled.value && Settings.canDrawOverlays(context)) {
            FloatingBubbleService.start(context)
        }
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
            _inputText.value = ""
            _currentResult.value = null
            _toastEvent.emit("All data permanently deleted")
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
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
