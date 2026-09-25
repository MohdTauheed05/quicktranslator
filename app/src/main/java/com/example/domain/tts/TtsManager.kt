package com.example.domain.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.domain.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            _lastError.value = "Failed to initialize Text-to-Speech engine"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
        } else {
            isInitialized = false
            _lastError.value = "Text-to-Speech initialization error"
        }
    }

    fun speak(text: String, language: Language) {
        if (!isInitialized || tts == null) {
            _lastError.value = "TTS engine not ready"
            return
        }

        if (text.isBlank()) return

        val locale = language.locale
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to default or English
            tts?.setLanguage(Locale.ENGLISH)
        }

        val utteranceId = "utterance_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
