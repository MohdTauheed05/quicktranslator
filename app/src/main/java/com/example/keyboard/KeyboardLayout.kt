package com.example.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
val GboardToolbarBg = Color(0xFF282A2E)
val GboardKeyBg = Color(0xFF2D2F33)
val GboardKeyPressedBg = Color(0xFF404347)
val GboardFnKeyBg = Color(0xFF232528)
val GboardEnterBlue = Color(0xFF1A73E8)
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
    onSwitchIme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GboardBg)
            .padding(bottom = 6.dp)
    ) {
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
            onSwapLanguages = onSwapLanguages,
            onOpenLanguagePicker = onOpenLanguagePicker,
            onSelectLanguage = onSelectLanguage,
            onCloseLanguagePicker = onCloseLanguagePicker,
            onInsertCopiedTranslation = onInsertCopiedTranslation,
            onDismissCopiedChip = onDismissCopiedChip,
            onInsertLiveTranslation = onInsertLiveTranslation,
            onToggleLiveMode = onToggleLiveMode,
            onVoiceClick = onVoiceClick,
            onPasteClipboard = onPasteClipboard
        )

        Spacer(modifier = Modifier.height(4.dp))

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
    onSwapLanguages: () -> Unit,
    onOpenLanguagePicker: (String) -> Unit,
    onSelectLanguage: (Language, String) -> Unit,
    onCloseLanguagePicker: () -> Unit,
    onInsertCopiedTranslation: () -> Unit,
    onDismissCopiedChip: () -> Unit,
    onInsertLiveTranslation: () -> Unit,
    onToggleLiveMode: () -> Unit,
    onVoiceClick: () -> Unit,
    onPasteClipboard: () -> Unit
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(GboardToolbarBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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

            IconButton(
                onClick = {
                    haptic()
                    onSwapLanguages()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Languages",
                    tint = Color(0xFF8AB4F8),
                    modifier = Modifier.size(18.dp)
                )
            }

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
                        text = targetLang.name,
                        color = Color(0xFF8AB4F8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
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

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                onClick = {
                    haptic()
                    onToggleLiveMode()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isLiveModeEnabled) Color(0xFF0F5132) else Color(0xFF32363C)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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

            Spacer(modifier = Modifier.width(4.dp))

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
                    .background(Color(0xFF132032))
                    .border(1.dp, Color(0xFF1A73E8).copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = Color(0xFF8AB4F8), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📋 Copied: \"${copiedOriginalText?.take(22) ?: ""}\"",
                        color = GboardSubText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = copiedTranslatedText,
                        color = GboardTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    onClick = {
                        haptic()
                        onInsertCopiedTranslation()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = GboardEnterBlue
                ) {
                    Text(
                        text = "Paste ↵",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        haptic()
                        onDismissCopiedChip()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = GboardSubText, modifier = Modifier.size(15.dp))
                }
            }
        }

        if (isLiveModeEnabled && liveTypedText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF19251E))
                    .clickable {
                        haptic()
                        onInsertLiveTranslation()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLiveTranslating) "Translating..." else (liveTranslatedText ?: liveTypedText),
                        color = Color(0xFFA8DAB5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E7E34)) {
                    Text(text = "Replace & Send", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
            onModeChange = onModeChange
        )
        KeyboardKeyMode.EXTRA_SYMBOLS -> FastExtraSymbolsLayout(
            onKeyClick = onKeyClick,
            onBackspace = onBackspace,
            onEnter = onEnter,
            onSpace = onSpace,
            onModeChange = onModeChange
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            FastBackspaceKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onBackspace = onBackspace
            )
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
                    .clickable {
                        haptic()
                        onModeChange(KeyboardKeyMode.SYMBOLS)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "?123", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            FastKey(
                primaryText = ",",
                secondaryText = null,
                modifier = Modifier.weight(0.9f),
                onClick = {
                    haptic()
                    onKeyClick(",")
                }
            )

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
                Icon(imageVector = Icons.Default.Language, contentDescription = "Switch Keyboard", tint = GboardSubText, modifier = Modifier.size(18.dp))
            }

            Box(
                modifier = Modifier
                    .weight(4.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardKeyBg, RoundedCornerShape(6.dp))
                    .clickable {
                        haptic()
                        onSpace()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "English · QuickTranslate", color = GboardSubText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            FastKey(
                primaryText = ".",
                secondaryText = null,
                modifier = Modifier.weight(0.9f),
                onClick = {
                    haptic()
                    onKeyClick(".")
                }
            )

            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(2.dp, RoundedCornerShape(6.dp))
                    .background(GboardEnterBlue, RoundedCornerShape(6.dp))
                    .clickable {
                        haptic()
                        onEnter()
                    },
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
    onModeChange: (KeyboardKeyMode) -> Unit
) {
    val view = LocalView.current
    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
    }

    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    val row3 = listOf("*", "\"", "'", ":", ";", "!", "?")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { k -> FastKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(k) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { k -> FastKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(k) }) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onModeChange(KeyboardKeyMode.EXTRA_SYMBOLS) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "=\\<", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            row3.forEach { k -> FastKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(k) }) }

            FastBackspaceKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onBackspace = onBackspace
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onModeChange(KeyboardKeyMode.LETTERS) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "ABC", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            FastKey(primaryText = ",", modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(",") })
            Box(
                modifier = Modifier
                    .weight(4.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "space", color = GboardSubText, fontSize = 12.sp)
            }
            FastKey(primaryText = ".", modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(".") })
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(2.dp, RoundedCornerShape(6.dp))
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
    onModeChange: (KeyboardKeyMode) -> Unit
) {
    val view = LocalView.current
    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (e: Exception) {}
    }

    val row1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
    val row2 = listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
    val row3 = listOf("%", "©", "®", "™", "✓", "[", "]")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { k -> FastKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(k) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { k -> FastKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(k) }) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onModeChange(KeyboardKeyMode.SYMBOLS) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "?123", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            row3.forEach { k -> FastKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(k) }) }
            FastBackspaceKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onBackspace = onBackspace
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardFnKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onModeChange(KeyboardKeyMode.LETTERS) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "ABC", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            FastKey(primaryText = "<", modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick("<") })
            Box(
                modifier = Modifier
                    .weight(4.4f)
                    .height(46.dp)
                    .shadow(1.dp, RoundedCornerShape(6.dp))
                    .background(GboardKeyBg, RoundedCornerShape(6.dp))
                    .clickable { haptic(); onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "space", color = GboardSubText, fontSize = 12.sp)
            }
            FastKey(primaryText = ">", modifier = Modifier.weight(1f), onClick = { haptic(); onKeyClick(">") })
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .shadow(2.dp, RoundedCornerShape(6.dp))
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
private fun FastKey(
    primaryText: String,
    secondaryText: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .background(GboardKeyBg, RoundedCornerShape(6.dp))
            .then(
                if (onLongClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongClick() }
                        )
                    }
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                }
            ),
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
}

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
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    try {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } catch (e: Exception) {}
                    onBackspace()

                    repeatJob = coroutineScope.launch {
                        delay(350)
                        while (isActive) {
                            try {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            } catch (e: Exception) {}
                            onBackspace()
                            delay(45)
                        }
                    }

                    waitForUpOrCancellation()
                    repeatJob?.cancel()
                    repeatJob = null
                }
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
