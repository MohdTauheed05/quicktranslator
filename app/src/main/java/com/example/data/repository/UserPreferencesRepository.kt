package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quicktranslate_prefs", Context.MODE_PRIVATE)

    private val _targetLanguage = MutableStateFlow(
        Language.fromCode(prefs.getString(KEY_TARGET_LANG, "en") ?: "en")
    )
    val targetLanguage: StateFlow<Language> = _targetLanguage.asStateFlow()

    private val _sourceLanguage = MutableStateFlow(
        Language.fromCode(prefs.getString(KEY_SOURCE_LANG, "auto") ?: "auto")
    )
    val sourceLanguage: StateFlow<Language> = _sourceLanguage.asStateFlow()

    private val _autoClipboardEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_CLIPBOARD, true)
    )
    val autoClipboardEnabled: StateFlow<Boolean> = _autoClipboardEnabled.asStateFlow()

    private val _floatingBubbleEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_FLOATING_BUBBLE, false)
    )
    val floatingBubbleEnabled: StateFlow<Boolean> = _floatingBubbleEnabled.asStateFlow()

    private val _businessModeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BUSINESS_MODE, false)
    )
    val businessModeEnabled: StateFlow<Boolean> = _businessModeEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING, false)
    )
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    fun setTargetLanguage(language: Language) {
        prefs.edit().putString(KEY_TARGET_LANG, language.code).apply()
        _targetLanguage.value = language
    }

    fun setSourceLanguage(language: Language) {
        prefs.edit().putString(KEY_SOURCE_LANG, language.code).apply()
        _sourceLanguage.value = language
    }

    fun setAutoClipboardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CLIPBOARD, enabled).apply()
        _autoClipboardEnabled.value = enabled
    }

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLOATING_BUBBLE, enabled).apply()
        _floatingBubbleEnabled.value = enabled
    }

    fun setBusinessModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUSINESS_MODE, enabled).apply()
        _businessModeEnabled.value = enabled
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING, completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    fun resetAllPreferences() {
        prefs.edit().clear().apply()
        _targetLanguage.value = Language.ENGLISH
        _sourceLanguage.value = Language.AUTO
        _autoClipboardEnabled.value = true
        _floatingBubbleEnabled.value = false
        _businessModeEnabled.value = false
        _themeMode.value = ThemeMode.SYSTEM
        _hasCompletedOnboarding.value = false
    }

    companion object {
        private const val KEY_TARGET_LANG = "target_lang"
        private const val KEY_SOURCE_LANG = "source_lang"
        private const val KEY_AUTO_CLIPBOARD = "auto_clipboard"
        private const val KEY_FLOATING_BUBBLE = "floating_bubble"
        private const val KEY_BUSINESS_MODE = "business_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ONBOARDING = "onboarding_done"
    }
}
