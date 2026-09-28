package com.example.keyboard

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.QuickTranslateApp
import com.example.domain.ai.AiAssistantEngine
import com.example.domain.ai.AiTone
import com.example.domain.model.Language
import com.example.domain.translation.SpeechCorrector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuickTranslateKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val store by lazy { ViewModelStore() }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var liveTranslateJob: Job? = null

    private val translationRepo by lazy { (application as QuickTranslateApp).translationRepository }
    private val userPrefs by lazy { (application as QuickTranslateApp).userPreferences }

    private var targetLang by mutableStateOf(Language.ENGLISH)
    private var sourceLang by mutableStateOf(Language.AUTO)

    private var copiedOriginalText by mutableStateOf<String?>(null)
    private var copiedTranslatedText by mutableStateOf<String?>(null)
    private var isTranslatingCopied by mutableStateOf(false)
    private var lastCheckedClipboard: String? = null

    private var liveTypedText by mutableStateOf("")
    private var liveTranslatedText by mutableStateOf<String?>(null)
    private var isLiveTranslating by mutableStateOf(false)
    private var isLiveModeEnabled by mutableStateOf(false)

    // Smart Auto-Correction & Suggestions
    private var autoCorrectionEnabled by mutableStateOf(true)
    private var currentWord by mutableStateOf("")
    private var suggestions by mutableStateOf<List<String>>(emptyList())
    private var lastAutoCorrectedFrom: String? = null
    private var lastAutoCorrectedTo: String? = null

    // Gemini AI Assistant & Tones
    private var showAiMenu by mutableStateOf(false)
    private var isAiGenerating by mutableStateOf(false)
    private var aiSmartReplies by mutableStateOf<List<String>>(emptyList())

    // Voice Dictation
    private var isListeningVoice by mutableStateOf(false)
    private var voiceStatusText by mutableStateOf<String?>(null)
    private var speechRecognizer: SpeechRecognizer? = null

    private var keyMode by mutableStateOf(KeyboardKeyMode.LETTERS)
    private var isShifted by mutableStateOf(false)
    private var isCapsLock by mutableStateOf(false)
    private var showLanguagePickerFor by mutableStateOf<String?>(null)

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        try {
            savedStateRegistryController.performRestore(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        serviceScope.launch {
            userPrefs.targetLanguage.collect { targetLang = it }
        }
        serviceScope.launch {
            userPrefs.sourceLanguage.collect { sourceLang = it }
        }
        serviceScope.launch {
            userPrefs.autoCorrectionEnabled.collect { autoCorrectionEnabled = it }
        }
    }

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        try {
            win.decorView.setViewTreeLifecycleOwner(this)
            win.decorView.setViewTreeViewModelStoreOwner(this)
            win.decorView.setViewTreeSavedStateRegistryOwner(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decor ->
            try {
                decor.setViewTreeLifecycleOwner(this)
                decor.setViewTreeViewModelStoreOwner(this)
                decor.setViewTreeSavedStateRegistryOwner(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (lifecycleRegistry.currentState < Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewTreeLifecycleOwner(this@QuickTranslateKeyboardService)
            setViewTreeViewModelStoreOwner(this@QuickTranslateKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@QuickTranslateKeyboardService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)

            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        background = Color(0xFF1E1F22),
                        surface = Color(0xFF2D2F33),
                        primary = Color(0xFF1A73E8)
                    )
                ) {
                    KeyboardLayout(
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        copiedOriginalText = copiedOriginalText,
                        copiedTranslatedText = copiedTranslatedText,
                        isTranslatingCopied = isTranslatingCopied,
                        liveTypedText = liveTypedText,
                        liveTranslatedText = liveTranslatedText,
                        isLiveTranslating = isLiveTranslating,
                        isLiveModeEnabled = isLiveModeEnabled,
                        isListeningVoice = isListeningVoice,
                        voiceStatusText = voiceStatusText,
                        keyMode = keyMode,
                        isShifted = isShifted,
                        isCapsLock = isCapsLock,
                        showLanguagePickerFor = showLanguagePickerFor,
                        suggestions = suggestions,
                        showAiMenu = showAiMenu,
                        isAiGenerating = isAiGenerating,
                        aiSmartReplies = aiSmartReplies,
                        onKeyClick = { handleKeyClick(it) },
                        onBackspace = { handleBackspace() },
                        onEnter = { handleEnter() },
                        onSpace = { handleSpace() },
                        onShiftClick = { handleShiftClick() },
                        onModeChange = { keyMode = it },
                        onSwapLanguages = { handleSwapLanguages() },
                        onOpenLanguagePicker = { showLanguagePickerFor = it },
                        onSelectLanguage = { lang, target -> handleSelectLanguage(lang, target) },
                        onCloseLanguagePicker = { showLanguagePickerFor = null },
                        onInsertCopiedTranslation = { handleInsertCopiedTranslation() },
                        onDismissCopiedChip = { copiedTranslatedText = null },
                        onInsertLiveTranslation = { handleInsertLiveTranslation() },
                        onToggleLiveMode = {
                            isLiveModeEnabled = !isLiveModeEnabled
                            if (!isLiveModeEnabled) {
                                liveTypedText = ""
                                liveTranslatedText = null
                            }
                        },
                        onVoiceClick = { toggleVoiceInput() },
                        onPasteClipboard = { handlePasteClipboard() },
                        onSwitchIme = { handleSwitchInputMethod() },
                        onSelectSuggestion = { handleSelectSuggestion(it) },
                        onToggleAiMenu = { showAiMenu = !showAiMenu },
                        onApplyAiTone = { handleApplyAiTone(it) },
                        onApplySmartReply = { handleApplySmartReply(it) }
                    )
                }
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        window?.window?.decorView?.let { decor ->
            try {
                decor.setViewTreeLifecycleOwner(this)
                decor.setViewTreeViewModelStoreOwner(this)
                decor.setViewTreeSavedStateRegistryOwner(this)
            } catch (e: Exception) {}
        }
        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        liveTypedText = ""
        liveTranslatedText = null
        currentWord = ""
        suggestions = emptyList()
        showLanguagePickerFor = null
        showAiMenu = false

        checkClipboardForTranslation()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        stopVoiceInput()
        liveTranslateJob?.cancel()
        currentWord = ""
        suggestions = emptyList()
        showAiMenu = false
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVoiceInput()
        serviceScope.cancel()
        store.clear()
        if (lifecycleRegistry.currentState >= Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private fun checkClipboardForTranslation() {
        if (!userPrefs.autoClipboardEnabled.value) return

        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            if (!clipboard.hasPrimaryClip()) return

            val clipItem = clipboard.primaryClip?.getItemAt(0)
            val clipText = clipItem?.coerceToText(this)?.toString()?.trim() ?: return

            if (clipText.isEmpty() || clipText == lastCheckedClipboard) return
            lastCheckedClipboard = clipText

            if (clipText.length > 500) return

            translateCopiedText(clipText)

            // Pre-generate AI Smart Replies for copied WhatsApp message
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val replies = AiAssistantEngine.generateSmartReplies(clipText)
                    serviceScope.launch(Dispatchers.Main) {
                        aiSmartReplies = replies
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun translateCopiedText(text: String) {
        copiedOriginalText = text
        copiedTranslatedText = null
        isTranslatingCopied = true

        serviceScope.launch(Dispatchers.IO) {
            try {
                val targetToUse = if (targetLang == Language.AUTO) Language.ENGLISH else targetLang
                val result = translationRepo.translate(
                    text = text,
                    sourceLang = sourceLang,
                    targetLang = targetToUse,
                    forceBusinessMode = userPrefs.businessModeEnabled.value
                )
                if (result.translatedText.isNotBlank()) {
                    copiedTranslatedText = result.translatedText
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isTranslatingCopied = false
            }
        }
    }

    private fun handleKeyClick(char: String) {
        currentInputConnection?.commitText(char, 1)

        if (isShifted && !isCapsLock) {
            isShifted = false
        }

        // Auto-correction & suggestions tracking
        if (char.length == 1 && (char[0].isLetter() || char[0].isDigit())) {
            currentWord += char
            lastAutoCorrectedFrom = null
            lastAutoCorrectedTo = null
            suggestions = if (autoCorrectionEnabled) {
                AutoCorrectionEngine.getSuggestions(currentWord)
            } else {
                emptyList()
            }
        } else {
            currentWord = ""
            suggestions = emptyList()
        }

        if (isLiveModeEnabled) {
            liveTypedText += char
            triggerLiveTranslation(liveTypedText)
        }
    }

    private fun handleSelectSuggestion(suggestion: String) {
        val wordLen = currentWord.length
        if (wordLen > 0) {
            currentInputConnection?.deleteSurroundingText(wordLen, 0)
        }
        currentInputConnection?.commitText(suggestion + " ", 1)
        currentWord = ""
        suggestions = emptyList()
    }

    private fun handleBackspace() {
        // If user presses backspace immediately after an auto-correction, undo it back to literal typed word!
        if (lastAutoCorrectedTo != null && lastAutoCorrectedFrom != null) {
            val toLen = lastAutoCorrectedTo!!.length + 1 // including the trailing space
            currentInputConnection?.deleteSurroundingText(toLen, 0)
            currentInputConnection?.commitText(lastAutoCorrectedFrom!!, 1)
            currentWord = lastAutoCorrectedFrom!!
            lastAutoCorrectedFrom = null
            lastAutoCorrectedTo = null
            suggestions = if (autoCorrectionEnabled) AutoCorrectionEngine.getSuggestions(currentWord) else emptyList()
            return
        }

        currentInputConnection?.deleteSurroundingText(1, 0)
        if (currentWord.isNotEmpty()) {
            currentWord = currentWord.dropLast(1)
            suggestions = if (autoCorrectionEnabled && currentWord.isNotEmpty()) {
                AutoCorrectionEngine.getSuggestions(currentWord)
            } else {
                emptyList()
            }
        }

        if (isLiveModeEnabled && liveTypedText.isNotEmpty()) {
            liveTypedText = liveTypedText.dropLast(1)
            if (liveTypedText.isNotEmpty()) {
                triggerLiveTranslation(liveTypedText)
            } else {
                liveTranslatedText = null
            }
        }
    }

    private fun handleSpace() {
        // Check for Auto-Correction on space
        if (autoCorrectionEnabled && currentWord.isNotBlank()) {
            val correction = AutoCorrectionEngine.getAutoCorrectionForSpace(currentWord)
            if (correction != null && correction != currentWord) {
                currentInputConnection?.deleteSurroundingText(currentWord.length, 0)
                currentInputConnection?.commitText(correction, 1)
                lastAutoCorrectedFrom = currentWord
                lastAutoCorrectedTo = correction
            }
        }

        currentInputConnection?.commitText(" ", 1)
        currentWord = ""
        suggestions = emptyList()

        if (isLiveModeEnabled) {
            liveTypedText += " "
            triggerLiveTranslation(liveTypedText)
        }
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        when (action) {
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_DONE -> {
                ic.performEditorAction(action)
            }
            else -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }
        liveTypedText = ""
        liveTranslatedText = null
        currentWord = ""
        suggestions = emptyList()
    }

    private fun handleShiftClick() {
        if (!isShifted && !isCapsLock) {
            isShifted = true
        } else if (isShifted && !isCapsLock) {
            isCapsLock = true
        } else {
            isShifted = false
            isCapsLock = false
        }
    }

    private fun handleSwapLanguages() {
        if (sourceLang == Language.AUTO) {
            val oldTarget = targetLang
            targetLang = Language.ENGLISH
            sourceLang = oldTarget
        } else {
            val temp = sourceLang
            sourceLang = targetLang
            targetLang = temp
        }
        copiedOriginalText?.let { translateCopiedText(it) }
        if (liveTypedText.isNotBlank()) {
            triggerLiveTranslation(liveTypedText)
        }
    }

    private fun handleSelectLanguage(language: Language, target: String) {
        if (target == "source") {
            sourceLang = language
            userPrefs.setSourceLanguage(language)
        } else {
            targetLang = language
            userPrefs.setTargetLanguage(language)
        }
        showLanguagePickerFor = null
        copiedOriginalText?.let { translateCopiedText(it) }
        if (liveTypedText.isNotBlank()) {
            triggerLiveTranslation(liveTypedText)
        }
    }

    private fun handleInsertCopiedTranslation() {
        copiedTranslatedText?.let { text ->
            currentInputConnection?.commitText(text, 1)
        }
    }

    private fun handleInsertLiveTranslation() {
        val translated = liveTranslatedText ?: return
        val typedLen = liveTypedText.length
        if (typedLen > 0) {
            currentInputConnection?.deleteSurroundingText(typedLen, 0)
        }
        currentInputConnection?.commitText(translated, 1)
        liveTypedText = ""
        liveTranslatedText = null
    }

    private fun handlePasteClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            if (clipboard.hasPrimaryClip()) {
                val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
                if (!clipText.isNullOrBlank()) {
                    currentInputConnection?.commitText(clipText, 1)
                    translateCopiedText(clipText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleApplyAiTone(tone: AiTone) {
        isAiGenerating = true
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Determine text to rewrite: either live typed text, copied text, or text before cursor
                val textToRewrite = if (liveTypedText.isNotBlank()) {
                    liveTypedText
                } else if (!copiedOriginalText.isNullOrBlank()) {
                    copiedOriginalText!!
                } else {
                    currentInputConnection?.getTextBeforeCursor(200, 0)?.toString()?.trim() ?: ""
                }

                if (textToRewrite.isNotBlank()) {
                    val rewritten = AiAssistantEngine.rewrite(textToRewrite, tone, targetLang)
                    serviceScope.launch(Dispatchers.Main) {
                        if (rewritten.isNotBlank()) {
                            if (liveTypedText.isNotBlank()) {
                                currentInputConnection?.deleteSurroundingText(liveTypedText.length, 0)
                                liveTypedText = ""
                            }
                            currentInputConnection?.commitText(rewritten, 1)
                            showAiMenu = false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                serviceScope.launch(Dispatchers.Main) {
                    isAiGenerating = false
                }
            }
        }
    }

    private fun handleApplySmartReply(reply: String) {
        currentInputConnection?.commitText(reply + " ", 1)
        showAiMenu = false
    }

    private fun triggerLiveTranslation(text: String) {
        liveTranslateJob?.cancel()
        if (text.isBlank()) {
            liveTranslatedText = null
            return
        }
        liveTranslateJob = serviceScope.launch(Dispatchers.IO) {
            delay(320)
            isLiveTranslating = true
            try {
                val targetToUse = if (targetLang == Language.AUTO) Language.ENGLISH else targetLang
                val result = translationRepo.translate(
                    text = text,
                    sourceLang = sourceLang,
                    targetLang = targetToUse,
                    forceBusinessMode = false
                )
                liveTranslatedText = result.translatedText
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLiveTranslating = false
            }
        }
    }

    private fun toggleVoiceInput() {
        if (isListeningVoice) {
            stopVoiceInput()
        } else {
            startVoiceInput()
        }
    }

    private fun startVoiceInput() {
        // 1. Check RECORD_AUDIO runtime permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            voiceStatusText = "Microphone permission required - tap to allow"
            mainHandler.postDelayed({ voiceStatusText = null }, 3500)
            try {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("REQUEST_MIC_PERMISSION", true)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        // 2. Check if SpeechRecognizer service is available on device
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceStatusText = "Opening voice recognition..."
            mainHandler.postDelayed({ voiceStatusText = null }, 2000)
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to translate with QuickTranslate")
                }
                startActivity(intent)
            } catch (e: Exception) {
                voiceStatusText = "Voice input unavailable on this device"
            }
            return
        }

        try {
            stopVoiceInput()
            isListeningVoice = true
            voiceStatusText = "Listening... speak now"

            mainHandler.post {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                isListeningVoice = true
                                voiceStatusText = "Listening... speak now"
                            }

                            override fun onBeginningOfSpeech() {
                                voiceStatusText = "Hearing voice..."
                            }

                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {
                                voiceStatusText = "Translating speech..."
                            }

                            override fun onError(error: Int) {
                                isListeningVoice = false
                                val msg = when (error) {
                                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer busy"
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission needed"
                                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network required for voice"
                                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                                    else -> "Voice ended"
                                }
                                voiceStatusText = msg
                                mainHandler.postDelayed({ voiceStatusText = null }, 2200)
                            }

                            override fun onResults(results: Bundle?) {
                                isListeningVoice = false
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                                val (bestSpoken, _) = SpeechCorrector.processSpeechResults(matches)

                                if (bestSpoken.isNotBlank()) {
                                    serviceScope.launch(Dispatchers.IO) {
                                        val targetToUse = if (targetLang == Language.AUTO) Language.ENGLISH else targetLang
                                        val trans = translationRepo.translate(
                                            text = bestSpoken,
                                            sourceLang = sourceLang,
                                            targetLang = targetToUse,
                                            forceBusinessMode = false
                                        )
                                        val textToCommit = if (trans.translatedText.isNotBlank()) trans.translatedText else bestSpoken
                                        serviceScope.launch(Dispatchers.Main) {
                                            currentInputConnection?.commitText(textToCommit + " ", 1)
                                        }
                                    }
                                }
                                voiceStatusText = null
                            }

                            override fun onPartialResults(partialResults: Bundle?) {}
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                        if (sourceLang != Language.AUTO) {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLang.code)
                        }
                    }
                    speechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    isListeningVoice = false
                    voiceStatusText = "Voice initialization error"
                    mainHandler.postDelayed({ voiceStatusText = null }, 2000)
                }
            }
        } catch (e: Exception) {
            isListeningVoice = false
            voiceStatusText = "Voice error"
            mainHandler.postDelayed({ voiceStatusText = null }, 2000)
        }
    }

    private fun stopVoiceInput() {
        isListeningVoice = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        speechRecognizer = null
    }

    private fun handleSwitchInputMethod() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
