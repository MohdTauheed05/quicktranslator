package com.example.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Language

enum class KeyboardKeyMode {
    LETTERS,
    SYMBOLS,
    EXTRA_SYMBOLS
}

private val GboardBg = Color(0xFF1E1F22)
private val GboardToolbarBg = Color(0xFF282A2E)
private val GboardKeyBg = Color(0xFF2D2F33)
private val GboardKeyPressedBg = Color(0xFF404347)
private val GboardFnKeyBg = Color(0xFF232528)
private val GboardEnterBlue = Color(0xFF1A73E8)
private val GboardTextWhite = Color(0xFFF2F2F2)
private val GboardSubText = Color(0xFF9AA0A6)

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
    val view = LocalView.current

    fun performHaptic() {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (e: Exception) {}
    }

    val topLanguages = listOf(
        Language.AUTO, Language.ARABIC, Language.ENGLISH, Language.HINDI,
        Language.URDU, Language.NEPALI, Language.FRENCH, Language.SPANISH,
        Language.GERMAN, Language.CHINESE, Language.RUSSIAN, Language.TURKISH
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GboardBg)
            .padding(bottom = 6.dp)
    ) {
        // 1. GBOARD TOP TOOLBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(GboardToolbarBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                onClick = {
                    performHaptic()
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
                    performHaptic()
                    onSwapLanguages()
                },
                modifier = Modifier.size(32.dp)
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
                    performHaptic()
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
                    performHaptic()
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
                        text = if (isLiveModeEnabled) "Live" else "Live",
                        color = if (isLiveModeEnabled) Color(0xFFD1E7DD) else GboardSubText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    performHaptic()
                    onVoiceClick()
                },
                modifier = Modifier.size(32.dp)
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
                    performHaptic()
                    onPasteClipboard()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    tint = GboardSubText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. LANGUAGE PICKER CHIPS STRIP
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
                            performHaptic()
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

        // 3. VOICE LISTENING ACTIVE BANNER
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

        // 4. GBOARD CLIPBOARD AUTO-TRANSLATE BANNER
        if (isTranslatingCopied) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2430))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFF8AB4F8),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Translating copied message...",
                    color = Color(0xFF8AB4F8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
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
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = Color(0xFF8AB4F8),
                    modifier = Modifier.size(18.dp)
                )
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
                        performHaptic()
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
                        performHaptic()
                        onDismissCopiedChip()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = GboardSubText, modifier = Modifier.size(15.dp))
                }
            }
        }

        // 5. LIVE TYPING PREVIEW STRIP
        if (isLiveModeEnabled && liveTypedText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF19251E))
                    .clickable {
                        performHaptic()
                        onInsertLiveTranslation()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = Color(0xFF34A853),
                    modifier = Modifier.size(16.dp)
                )
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E7E34)
                ) {
                    Text(
                        text = "Replace & Send",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 6. GBOARD MAIN QWERTY / SYMBOL KEYBOARD
        when (keyMode) {
            KeyboardKeyMode.LETTERS -> GboardLettersKeyboard(
                isShifted = isShifted,
                isCapsLock = isCapsLock,
                targetLangName = targetLang.name,
                onKeyClick = {
                    performHaptic()
                    onKeyClick(it)
                },
                onBackspace = {
                    performHaptic()
                    onBackspace()
                },
                onEnter = {
                    performHaptic()
                    onEnter()
                },
                onSpace = {
                    performHaptic()
                    onSpace()
                },
                onShiftClick = {
                    performHaptic()
                    onShiftClick()
                },
                onModeChange = {
                    performHaptic()
                    onModeChange(it)
                },
                onSwitchIme = onSwitchIme
            )
            KeyboardKeyMode.SYMBOLS -> GboardSymbolsKeyboard(
                onKeyClick = {
                    performHaptic()
                    onKeyClick(it)
                },
                onBackspace = {
                    performHaptic()
                    onBackspace()
                },
                onEnter = {
                    performHaptic()
                    onEnter()
                },
                onSpace = {
                    performHaptic()
                    onSpace()
                },
                onModeChange = {
                    performHaptic()
                    onModeChange(it)
                }
            )
            KeyboardKeyMode.EXTRA_SYMBOLS -> GboardExtraSymbolsKeyboard(
                onKeyClick = {
                    performHaptic()
                    onKeyClick(it)
                },
                onBackspace = {
                    performHaptic()
                    onBackspace()
                },
                onEnter = {
                    performHaptic()
                    onEnter()
                },
                onSpace = {
                    performHaptic()
                    onSpace()
                },
                onModeChange = {
                    performHaptic()
                    onModeChange(it)
                }
            )
        }
    }
}

@Composable
private fun GboardLettersKeyboard(
    isShifted: Boolean,
    isCapsLock: Boolean,
    targetLangName: String,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftClick: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit,
    onSwitchIme: () -> Unit
) {
    val row1 = listOf(
        Pair("q", "1"), Pair("w", "2"), Pair("e", "3"), Pair("r", "4"), Pair("t", "5"),
        Pair("y", "6"), Pair("u", "7"), Pair("i", "8"), Pair("o", "9"), Pair("p", "0")
    )
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
    val upper = isShifted || isCapsLock

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { (letter, num) ->
                val label = if (upper) letter.uppercase() else letter
                GboardKey(
                    primaryText = label,
                    secondaryText = num,
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick(label) },
                    onLongClick = { onKeyClick(num) }
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
                val label = if (upper) letter.uppercase() else letter
                GboardKey(
                    primaryText = label,
                    secondaryText = null,
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick(label) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GboardFunctionKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = if (isShifted || isCapsLock) Color(0xFF3B4048) else GboardFnKeyBg,
                onClick = onShiftClick
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardCapslock,
                    contentDescription = "Shift",
                    tint = if (isCapsLock) Color(0xFF8AB4F8) else if (isShifted) Color.White else GboardSubText,
                    modifier = Modifier.size(20.dp)
                )
            }

            row3.forEach { letter ->
                val label = if (upper) letter.uppercase() else letter
                GboardKey(
                    primaryText = label,
                    secondaryText = null,
                    modifier = Modifier.weight(1f),
                    onClick = { onKeyClick(label) }
                )
            }

            GboardFunctionKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onClick = onBackspace
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = GboardTextWhite,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GboardFunctionKey(
                modifier = Modifier.weight(1.3f),
                backgroundColor = GboardFnKeyBg,
                onClick = { onModeChange(KeyboardKeyMode.SYMBOLS) }
            ) {
                Text(text = "?123", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            GboardKey(
                primaryText = ",",
                secondaryText = null,
                modifier = Modifier.weight(0.9f),
                onClick = { onKeyClick(",") }
            )

            GboardFunctionKey(
                modifier = Modifier.weight(0.9f),
                backgroundColor = GboardFnKeyBg,
                onClick = onSwitchIme
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Switch Keyboard",
                    tint = GboardSubText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Surface(
                onClick = onSpace,
                modifier = Modifier
                    .weight(4.4f)
                    .height(46.dp),
                shape = RoundedCornerShape(6.dp),
                color = GboardKeyBg,
                shadowElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "English · QuickTranslate",
                        color = GboardSubText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            GboardKey(
                primaryText = ".",
                secondaryText = null,
                modifier = Modifier.weight(0.9f),
                onClick = { onKeyClick(".") }
            )

            Surface(
                onClick = onEnter,
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp),
                shape = RoundedCornerShape(6.dp),
                color = GboardEnterBlue,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GboardSymbolsKeyboard(
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit
) {
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
            row1.forEach { k -> GboardKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { k -> GboardKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GboardFunctionKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onClick = { onModeChange(KeyboardKeyMode.EXTRA_SYMBOLS) }
            ) {
                Text(text = "=\\<", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            row3.forEach { k -> GboardKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }

            GboardFunctionKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onClick = onBackspace
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = GboardTextWhite, modifier = Modifier.size(19.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GboardFunctionKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onClick = { onModeChange(KeyboardKeyMode.LETTERS) }
            ) {
                Text(text = "ABC", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            GboardKey(primaryText = ",", modifier = Modifier.weight(1f), onClick = { onKeyClick(",") })
            Surface(
                onClick = onSpace,
                modifier = Modifier.weight(4.4f).height(46.dp),
                shape = RoundedCornerShape(6.dp),
                color = GboardKeyBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "space", color = GboardSubText, fontSize = 12.sp)
                }
            }
            GboardKey(primaryText = ".", modifier = Modifier.weight(1f), onClick = { onKeyClick(".") })
            Surface(
                onClick = onEnter,
                modifier = Modifier.weight(1.4f).height(46.dp),
                shape = RoundedCornerShape(6.dp),
                color = GboardEnterBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun GboardExtraSymbolsKeyboard(
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit
) {
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
            row1.forEach { k -> GboardKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { k -> GboardKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GboardFunctionKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onClick = { onModeChange(KeyboardKeyMode.SYMBOLS) }
            ) {
                Text(text = "?123", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            row3.forEach { k -> GboardKey(primaryText = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
            GboardFunctionKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onClick = onBackspace
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = GboardTextWhite, modifier = Modifier.size(19.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GboardFunctionKey(
                modifier = Modifier.weight(1.4f),
                backgroundColor = GboardFnKeyBg,
                onClick = { onModeChange(KeyboardKeyMode.LETTERS) }
            ) {
                Text(text = "ABC", color = GboardTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            GboardKey(primaryText = "<", modifier = Modifier.weight(1f), onClick = { onKeyClick("<") })
            Surface(
                onClick = onSpace,
                modifier = Modifier.weight(4.4f).height(46.dp),
                shape = RoundedCornerShape(6.dp),
                color = GboardKeyBg
            ) {
                Box(contentAlignment = Alignment.Center) { Text(text = "space", color = GboardSubText, fontSize = 12.sp) }
            }
            GboardKey(primaryText = ">", modifier = Modifier.weight(1f), onClick = { onKeyClick(">") })
            Surface(
                onClick = onEnter,
                modifier = Modifier.weight(1.4f).height(46.dp),
                shape = RoundedCornerShape(6.dp),
                color = GboardEnterBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun GboardKey(
    primaryText: String,
    secondaryText: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(46.dp)
            .then(
                if (onLongClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongClick() }
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(6.dp),
        color = if (isPressed) GboardKeyPressedBg else GboardKeyBg,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
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
}

@Composable
private fun GboardFunctionKey(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GboardFnKeyBg,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(6.dp),
        color = if (isPressed) GboardKeyPressedBg else backgroundColor,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
