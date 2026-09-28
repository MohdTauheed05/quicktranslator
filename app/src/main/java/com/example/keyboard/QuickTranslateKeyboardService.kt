package com.example.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.example.QuickTranslateApp
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
    private var clipboardTranslateJob: Job? = null

    private val app by lazy { applicationContext as QuickTranslateApp }
    private val translationRepo by lazy { app.translationRepository }
    private val userPrefs by lazy { app.userPreferences }

    // Keyboard State Holders
    private var sourceLang by mutableStateOf(Language.AUTO)
    private var targetLang by mutableStateOf(Language.ENGLISH)
    private var copiedOriginalText by mutableStateOf<String?>(null)
    private var copiedTranslatedText by mutableStateOf<String?>(null)
    private var isTranslatingCopied by mutableStateOf(false)
    private var lastCheckedClipboard: String? = null

    private var liveTypedText by mutableStateOf("")
    private var liveTranslatedText by mutableStateOf<String?>(null)
    private var isLiveTranslating by mutableStateOf(false)
    private var isLiveModeEnabled by mutableStateOf(false)

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
                        onSwitchIme = { handleSwitchInputMethod() }
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
        showLanguagePickerFor = null

        checkClipboardForTranslation()
    }

    override fun onWindowShown() {
        super.onWindowShown()
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
        checkClipboardForTranslation()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        stopVoiceInput()
    }

    private fun handleSwitchInputMethod() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkClipboardForTranslation() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            if (!clipboard.hasPrimaryClip()) return
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return
            val raw = clip.getItemAt(0)?.coerceToText(this)?.toString()?.trim()

            if (!raw.isNullOrBlank() && raw != lastCheckedClipboard) {
                lastCheckedClipboard = raw
                translateCopiedText(raw)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun translateCopiedText(text: String) {
        clipboardTranslateJob?.cancel()
        clipboardTranslateJob = serviceScope.launch(Dispatchers.IO) {
            isTranslatingCopied = true
            copiedOriginalText = text
            try {
                val targetToUse = if (targetLang == Language.AUTO) Language.ENGLISH else targetLang
                val result = translationRepo.translate(
                    text = text,
                    sourceLang = sourceLang,
                    targetLang = targetToUse,
                    forceBusinessMode = false
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

        if (isLiveModeEnabled) {
            liveTypedText += char
            triggerLiveTranslation(liveTypedText)
        }
    }

    private fun handleBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
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
        currentInputConnection?.commitText(" ", 1)
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
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceStatusText = "Speech recognition unavailable"
            mainHandler.postDelayed({ voiceStatusText = null }, 2000)
            return
        }

        try {
            stopVoiceInput()
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
                        voiceStatusText = "Speech ended"
                        mainHandler.postDelayed({ voiceStatusText = null }, 1500)
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
                                    currentInputConnection?.commitText(textToCommit, 1)
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
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US", "hi-IN", "ur-PK", "ar-SA"))
                if (sourceLang != Language.AUTO) {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLang.code)
                }
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListeningVoice = false
            e.printStackTrace()
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

    override fun onDestroy() {
        super.onDestroy()
        stopVoiceInput()
        serviceScope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
