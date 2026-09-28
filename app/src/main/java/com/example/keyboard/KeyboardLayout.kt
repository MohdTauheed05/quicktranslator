package com.example.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.domain.ai.AiTone
import com.example.domain.model.Language
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class KeyboardKeyMode {
    LETTERS,
    SYMBOLS,
    EXTRA_SYMBOLS
}

val GboardBg = Color(0xFF1E1F22)
val GboardToolbarBg = Color(0xFF26282B)
val GboardKeyBg = Color(0xFF2F3136)
val GboardFnKeyBg = Color(0xFF26272B)
val GboardEnterBlue = Color(0xFF1A73E8)
val GboardLiveGreen = Color(0xFF1E7E34)
val GboardTextWhite = Color(0xFFF2F2F2)
val GboardSubText = Color(0xFF9AA0A6)

@Composable
fun KeyboardLayout(
    sourceLang: Language,
    targetLang: Language,
    copiedOriginalText: String?,
    copiedTranslatedText: String?,
    isTranslatingCopied: Boolean,
    liveTypedText: String,
    liveTranslatedText: String?,
    isLiveTranslating: Boolean,
    isLiveModeEnabled: Boolean,
    isListeningVoice: Boolean,
    voiceStatusText: String?,
    keyMode: KeyboardKeyMode,
    isShifted: Boolean,
    isCapsLock: Boolean,
    showLanguagePickerFor: String?,
    suggestions: List<String> = emptyList(),
    showAiMenu: Boolean = false,
    isAiGenerating: Boolean = false,
    aiSmartReplies: List<String> = emptyList(),
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftClick: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit,
    onSwapLanguages: () -> Unit,
    onOpenLanguagePicker: (String) -> Unit,
    onSelectLanguage: (Language, String) -> Unit,
    onCloseLanguagePicker: () -> Unit,
    onInsertCopiedTranslation: () -> Unit,
    onDismissCopiedChip: () -> Unit,
    onInsertLiveTranslation: () -> Unit,
    onToggleLiveMode: () -> Unit,
    onVoiceClick: () -> Unit,
    onPasteClipboard: () -> Unit,
    onSwitchIme: () -> Unit,
    onSelectSuggestion: (String) -> Unit = {},
    onToggleAiMenu: () -> Unit = {},
    onApplyAiTone: (AiTone) -> Unit = {},
    onApplySmartReply: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GboardBg)
            .padding(bottom = 6.dp)
    ) {
        // Toolbar Section
        KeyboardToolbarSection(
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
            showLanguagePickerFor = showLanguagePickerFor,
            showAiMenu = showAiMenu,
            isAiGenerating = isAiGenerating,
            aiSmartReplies = aiSmartReplies,
            onSwapLanguages = onSwapLanguages,
            onOpenLanguagePicker = onOpenLanguagePicker,
            onSelectLanguage = onSelectLanguage,
            onCloseLanguagePicker = onCloseLanguagePicker,
            onInsertCopiedTranslation = onInsertCopiedTranslation,
            onDismissCopiedChip = onDismissCopiedChip,
            onInsertLiveTranslation = onInsertLiveTranslation,
            onToggleLiveMode = onToggleLiveMode,
            onVoiceClick = onVoiceClick,
            onPasteClipboard = onPasteClipboard,
            onToggleAiMenu = onToggleAiMenu,
            onApplyAiTone = onApplyAiTone,
            onApplySmartReply = onApplySmartReply
        )

        // Smart Auto-Correction & Suggestions Strip
        if (suggestions.isNotEmpty()) {
            SuggestionsBar(
                suggestions = suggestions,
                onSelectSuggestion = onSelectSuggestion
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Keys Section with iPhone-style Key Magnifier Popups
        KeyboardKeysSection(
            keyMode = keyMode,
            isUpper = isShifted || isCapsLock,
            isShifted = isShifted,
            isCapsLock = isCapsLock,
            onKeyClick = onKeyClick,
            onBackspace = onBackspace,
            onEnter = onEnter,
            onSpace = onSpace,
            onShiftClick = onShiftClick,
            onModeChange = onModeChange,
            onSwitchIme = onSwitchIme
        )
    }
}

@Composable
private fun SuggestionsBar(
    suggestions: List<String>,
    onSelectSuggestion: (String) -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFF222428))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            val isPrimary = index == 1 || (suggestions.size == 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
                        onSelectSuggestion(suggestion)
                    }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = suggestion,
                    color = if (isPrimary) Color(0xFF8AB4F8) else GboardTextWhite,
                    fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (isPrimary) 15.sp else 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (index < suggestions.size - 1) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color(0xFF383B40))
                )
            }
        }
    }
}

@Composable
private fun KeyboardToolbarSection(
    sourceLang: Language,
    targetLang: Language,
    copiedOriginalText: String?,
    copiedTranslatedText: String?,
    isTranslatingCopied: Boolean,
    liveTypedText: String,
    liveTranslatedText: String?,
    isLiveTranslating: Boolean,
    isLiveModeEnabled: Boolean,
    isListeningVoice: Boolean,
    voiceStatusText: String?,
    showLanguagePickerFor: String?,
    showAiMenu: Boolean,
    isAiGenerating: Boolean,
    aiSmartReplies: List<String>,
    onSwapLanguages: () -> Unit,
    onOpenLanguagePicker: (String) -> Unit,
    onSelectLanguage: (Language, String) -> Unit,
    onCloseLanguagePicker: () -> Unit,
    onInsertCopiedTranslation: () -> Unit,
    onDismissCopiedChip: () -> Unit,
    onInsertLiveTranslation: () -> Unit,
    onToggleLiveMode: () -> Unit,
    onVoiceClick: () -> Unit,
    onPasteClipboard: () -> Unit,
    onToggleAiMenu: () -> Unit,
    onApplyAiTone: (AiTone) -> Unit,
    onApplySmartReply: (String) -> Unit
) {
    val view = LocalView.current
    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
    }

    val topLanguages = listOf(
        Language.AUTO, Language.ARABIC, Language.ENGLISH, Language.HINDI,
        Language.URDU, Language.NEPALI, Language.FRENCH, Language.SPANISH,
        Language.GERMAN, Language.CHINESE, Language.RUSSIAN, Language.TURKISH
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Gboard Main Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(GboardToolbarBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Source Language Pill
            Surface(
                onClick = {
                    haptic()
                    if (showLanguagePickerFor == "source") onCloseLanguagePicker()
                    else onOpenLanguagePicker("source")
                },
                shape = RoundedCornerShape(8.dp),
                color = if (showLanguagePickerFor == "source") Color(0xFF3C4043) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate",
                        tint = Color(0xFF8AB4F8),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (sourceLang == Language.AUTO) "Detect" else sourceLang.name,
                        color = GboardTextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = GboardSubText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Swap Languages
            IconButton(
                onClick = {
                    haptic()
                    onSwapLanguages()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap",
                    tint = GboardSubText,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Target Language Pill
            Surface(
                onClick = {
                    haptic()
                    if (showLanguagePickerFor == "target") onCloseLanguagePicker()
                    else onOpenLanguagePicker("target")
                },
                shape = RoundedCornerShape(8.dp),
                color = if (showLanguagePickerFor == "target") Color(0xFF3C4043) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${targetLang.flagEmoji} ${targetLang.name}",
                        color = GboardTextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = GboardSubText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // AI Magic Button (Gemini AI Assistant & Tones)
            Surface(
                onClick = {
                    haptic()
                    onToggleAiMenu()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (showAiMenu) Color(0xFF6A1B9A) else Color(0xFF38234A)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "AI Magic",
                        tint = if (showAiMenu) Color.White else Color(0xFFCE93D8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "AI ✨",
                        color = if (showAiMenu) Color.White else Color(0xFFE1BEE7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Live Translation Toggle
            Surface(
                onClick = {
                    haptic()
                    onToggleLiveMode()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isLiveModeEnabled) Color(0xFF0F5132) else Color(0xFF32363C)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = if (isLiveModeEnabled) Color(0xFF75B798) else GboardSubText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Live",
                        color = if (isLiveModeEnabled) Color(0xFFD1E7DD) else GboardSubText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Voice Dictation (Mic)
            IconButton(
                onClick = {
                    haptic()
                    onVoiceClick()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (isListeningVoice) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Dictation",
                    tint = if (isListeningVoice) Color(0xFFEA4335) else GboardTextWhite,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Paste Clipboard Icon
            IconButton(
                onClick = {
                    haptic()
                    onPasteClipboard()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    tint = GboardSubText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // AI Assistant Magic Bar (Tones & Smart Replies)
        AnimatedVisibility(visible = showAiMenu) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF241C2E))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAiGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFCE93D8), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI writing...", color = Color(0xFFCE93D8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        AiTone.entries.forEach { tone ->
                            Surface(
                                onClick = {
                                    haptic()
                                    onApplyAiTone(tone)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF3E2856)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = tone.iconLabel, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tone.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Smart Replies Carousel (if copied message exists)
                if (aiSmartReplies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "AI Smart Replies to copied message:",
                        color = Color(0xFFCE93D8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        aiSmartReplies.forEach { reply ->
                            Surface(
                                onClick = {
                                    haptic()
                                    onApplySmartReply(reply)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E2838)
                            ) {
                                Text(
                                    text = reply,
                                    color = Color(0xFF8AB4F8),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Language Picker Horizontal Carousel
        AnimatedVisibility(visible = showLanguagePickerFor != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF202124))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                topLanguages.forEach { lang ->
                    if (showLanguagePickerFor == "target" && lang == Language.AUTO) return@forEach
                    val isSelected = if (showLanguagePickerFor == "source") lang == sourceLang else lang == targetLang
                    Surface(
                        onClick = {
                            haptic()
                            showLanguagePickerFor?.let { onSelectLanguage(lang, it) }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) GboardEnterBlue else Color(0xFF2C2F33)
                    ) {
                        Text(
                            text = "${lang.flagEmoji} ${lang.name}",
                            color = GboardTextWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Voice Listening Active Banner
        if (isListeningVoice) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C1618))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFFEA4335),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = voiceStatusText ?: "Listening... speak now",
                    color = Color(0xFFF28B82),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onVoiceClick, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Clipboard Instant Translation Banner (WhatsApp Auto-Translate)
        if (isTranslatingCopied) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2430))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF8AB4F8), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Translating copied message...", color = Color(0xFF8AB4F8), fontSize = 12.sp)
            }
        } else if (!copiedTranslatedText.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2430))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Translated into ${targetLang.name}:",
                            color = Color(0xFF8AB4F8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "• WhatsApp Copied", color = GboardSubText, fontSize = 10.sp)
                    }
                    Text(
                        text = copiedTranslatedText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = {
                            haptic()
                            onInsertCopiedTranslation()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = GboardEnterBlue
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Paste ↵", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            haptic()
                            onDismissCopiedChip()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = GboardSubText, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Live Auto-Translate Floating Preview Banner
        if (isLiveModeEnabled && liveTypedText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF12281D))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(text = "Live translation -> ${targetLang.name}", color = Color(0xFF75B798), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (isLiveTranslating) {
                        Text(text = "Translating...", color = Color(0xFFA3CFBB), fontSize = 12.sp)
                    } else {
                        Text(
                            text = liveTranslatedText ?: liveTypedText,
                            color = Color(0xFFD1E7DD),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = {
                        haptic()
                        onInsertLiveTranslation()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = GboardLiveGreen
                ) {
                    Text(
                        text = "Replace ↵",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardKeysSection(
    keyMode: KeyboardKeyMode,
    isUpper: Boolean,
    isShifted: Boolean,
    isCapsLock: Boolean,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftClick: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit,
    onSwitchIme: () -> Unit
) {
    when (keyMode) {
        KeyboardKeyMode.LETTERS -> FastLettersLayout(
            isUpper = isUpper,
            isShifted = isShifted,
            isCapsLock = isCapsLock,
            onKeyClick = onKeyClick,
            onBackspace = onBackspace,
            onEnter = onEnter,
            onSpace = onSpace,
            onShiftClick = onShiftClick,
            onModeChange = onModeChange,
            onSwitchIme = onSwitchIme
        )
        KeyboardKeyMode.SYMBOLS -> FastSymbolsLayout(
            onKeyClick = onKeyClick,
            onBackspace = onBackspace,
            onEnter = onEnter,
            onSpace = onSpace,
            onModeChange = onModeChange,
            onSwitchIme = onSwitchIme
        )
        KeyboardKeyMode.EXTRA_SYMBOLS -> FastExtraSymbolsLayout(
            onKeyClick = onKeyClick,
            onBackspace = onBackspace,
            onEnter = onEnter,
            onSpace = onSpace,
            onModeChange = onModeChange,
            onSwitchIme = onSwitchIme
        )
    }
}

@Composable
private fun FastLettersLayout(
    isUpper: Boolean,
    isShifted: Boolean,
    isCapsLock: Boolean,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftClick: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit,
    onSwitchIme: () -> Unit
) {
    val view = LocalView.current
    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
    }

    val row1 = listOf(
        Pair("q", "1"), Pair("w", "2"), Pair("e", "3"), Pair("r", "4"), Pair("t", "5"),
        Pair("y", "6"), Pair("u", "7"), Pair("i", "8"), Pair("o", "9"), Pair("p", "0")
    )
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ROW 1: Q-P
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { (letter, num) ->
                val label = if (isUpper) letter.uppercase() else letter
                FastKey(
                    primaryText = label,
                    secondaryText = num,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic()
                        onKeyClick(label)
                    },
                    onLongClick = {
                        haptic()
                        onKeyClick(num)
                    }
                )
            }
        }

        // ROW 2: A-L
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row2.forEach { letter ->
                val label = if (isUpper) letter.uppercase() else letter
                FastKey(
                    primaryText = label,
                    secondaryText = null,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic()
                        onKeyClick(label)
                    }
                )
            }
        }

        // ROW 3: Shift, Z-M, Auto-Repeat Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift Key
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(if (isShifted || isCapsLock) Color(0xFF3B4048) else GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable {
                        haptic()
                        onShiftClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardCapslock,
                    contentDescription = "Shift",
                    tint = if (isCapsLock) Color(0xFF8AB4F8) else if (isShifted) Color.White else GboardSubText,
                    modifier = Modifier.size(20.dp)
                )
            }

            row3.forEach { letter ->
                val label = if (isUpper) letter.uppercase() else letter
                FastKey(
                    primaryText = label,
                    secondaryText = null,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic()
                        onKeyClick(label)
                    }
                )
            }

            // Auto-Repeat Backspace Key
            FastBackspaceKey(
                modifier = Modifier.weight(1.4f),
                onBackspace = onBackspace
            )
        }

        // ROW 4: ?123, Emoji/Lang, Space, ., Enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable {
                        haptic()
                        onModeChange(KeyboardKeyMode.SYMBOLS)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "?123", color = GboardTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable {
                        haptic()
                        onSwitchIme()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Language, contentDescription = "Switch Keyboard", tint = GboardSubText, modifier = Modifier.size(19.dp))
            }

            FastKey(
                primaryText = ",",
                secondaryText = null,
                modifier = Modifier.weight(0.9f),
                onClick = { haptic(); onKeyClick(",") }
            )

            // Spacebar
            Box(
                modifier = Modifier
                    .weight(3.8f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QuickTranslate",
                    color = GboardSubText.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            FastKey(
                primaryText = ".",
                secondaryText = null,
                modifier = Modifier.weight(0.9f),
                onClick = { haptic(); onKeyClick(".") }
            )

            // Enter Button
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardEnterBlue, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onEnter() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FastSymbolsLayout(
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit,
    onSwitchIme: () -> Unit
) {
    val view = LocalView.current
    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
    }

    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
    val row3 = listOf("*", "\"", "'", ":", ";", "!", "?")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { sym ->
                FastKey(primaryText = sym, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(sym) })
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { sym ->
                FastKey(primaryText = sym, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(sym) })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onModeChange(KeyboardKeyMode.EXTRA_SYMBOLS) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "=\\<", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            row3.forEach { sym ->
                FastKey(primaryText = sym, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(sym) })
            }

            FastBackspaceKey(modifier = Modifier.weight(1.3f), onBackspace = onBackspace)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onModeChange(KeyboardKeyMode.LETTERS) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "ABC", color = GboardTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onSwitchIme() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Language, contentDescription = "Switch", tint = GboardSubText, modifier = Modifier.size(19.dp))
            }

            FastKey(primaryText = ",", modifier = Modifier.weight(0.9f), onClick = { haptic(); onKeyClick(",") })

            Box(
                modifier = Modifier
                    .weight(3.8f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "QuickTranslate", color = GboardSubText.copy(alpha = 0.6f), fontSize = 12.sp)
            }

            FastKey(primaryText = ".", modifier = Modifier.weight(0.9f), onClick = { haptic(); onKeyClick(".") })

            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardEnterBlue, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onEnter() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FastExtraSymbolsLayout(
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit,
    onSwitchIme: () -> Unit
) {
    val view = LocalView.current
    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
    }

    val row1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
    val row2 = listOf("£", "€", "¥", "¢", "^", "°", "=", "{", "}", "\\")
    val row3 = listOf("%", "©", "®", "[", "]", "<", ">")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { sym ->
                FastKey(primaryText = sym, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(sym) })
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { sym ->
                FastKey(primaryText = sym, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(sym) })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onModeChange(KeyboardKeyMode.SYMBOLS) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "?123", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            row3.forEach { sym ->
                FastKey(primaryText = sym, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(sym) })
            }

            FastBackspaceKey(modifier = Modifier.weight(1.3f), onBackspace = onBackspace)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onModeChange(KeyboardKeyMode.LETTERS) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "ABC", color = GboardTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onSwitchIme() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Language, contentDescription = "Switch", tint = GboardSubText, modifier = Modifier.size(19.dp))
            }

            FastKey(primaryText = ",", modifier = Modifier.weight(0.9f), onClick = { haptic(); onKeyClick(",") })

            Box(
                modifier = Modifier
                    .weight(3.8f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "QuickTranslate", color = GboardSubText.copy(alpha = 0.6f), fontSize = 12.sp)
            }

            FastKey(primaryText = ".", modifier = Modifier.weight(0.9f), onClick = { haptic(); onKeyClick(".") })

            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardEnterBlue, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onEnter() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Key button with iPhone keyboard-style key enlargement preview popup!
 * When clicked or held, a crisp enlarged bubble pops up directly above the key.
 */
@Composable
private fun FastKey(
    primaryText: String,
    secondaryText: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current

    Box(
        modifier = modifier
            .height(46.dp)
            .zIndex(if (isPressed) 100f else 1f)
            .pointerInput(primaryText) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick()
                        }
                    },
                    onLongPress = {
                        isPressed = false
                        if (onLongClick != null) {
                            try { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (e: Exception) {}
                            onLongClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Base Key Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(1.dp, RoundedCornerShape(6.dp))
                .background(if (isPressed) Color(0xFF3B4048) else GboardKeyBg, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    color = GboardSubText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp)
                )
            }
            Text(
                text = primaryText,
                color = GboardTextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }

        // iPhone-Style Key Enlargement Preview Bubble
        if (isPressed) {
            Box(
                modifier = Modifier
                    .offset(y = (-54).dp)
                    .width(54.dp)
                    .height(58.dp)
                    .shadow(10.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C2F34), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        color = Color(0xFF8AB4F8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 3.dp, end = 6.dp)
                    )
                }
                Text(
                    text = primaryText,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Continuous auto-repeating backspace key (Hold to erase continuously)
 */
@Composable
private fun FastBackspaceKey(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GboardFnKeyBg,
    onBackspace: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }
    val view = LocalView.current

    Box(
        modifier = modifier
            .height(46.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
                        onBackspace()

                        repeatJob = coroutineScope.launch {
                            delay(400)
                            while (isActive) {
                                try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
                                onBackspace()
                                delay(65)
                            }
                        }

                        tryAwaitRelease()
                        repeatJob?.cancel()
                        repeatJob = null
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = GboardTextWhite,
            modifier = Modifier.size(20.dp)
        )
    }
}
