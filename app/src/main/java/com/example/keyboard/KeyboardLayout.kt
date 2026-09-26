package com.example.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onPasteClipboard: () -> Unit
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
            .background(Color(0xFF181A20))
            .padding(bottom = 6.dp)
    ) {
        // 1. TOP SMART TRANSLATION TOOLBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222630))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Source Language Pill
            Surface(
                onClick = {
                    performHaptic()
                    if (showLanguagePickerFor == "source") onCloseLanguagePicker()
                    else onOpenLanguagePicker("source")
                },
                shape = RoundedCornerShape(8.dp),
                color = if (showLanguagePickerFor == "source") Color(0xFF3B82F6) else Color(0xFF2D323F)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sourceLang.flagEmoji} ${sourceLang.name}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Swap Languages Button
            IconButton(
                onClick = {
                    performHaptic()
                    onSwapLanguages()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Languages",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Target Language Pill
            Surface(
                onClick = {
                    performHaptic()
                    if (showLanguagePickerFor == "target") onCloseLanguagePicker()
                    else onOpenLanguagePicker("target")
                },
                shape = RoundedCornerShape(8.dp),
                color = if (showLanguagePickerFor == "target") Color(0xFF3B82F6) else Color(0xFF1D4ED8)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${targetLang.flagEmoji} ${targetLang.name}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Live Translate Toggle Button
            Surface(
                onClick = {
                    performHaptic()
                    onToggleLiveMode()
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isLiveModeEnabled) Color(0xFF10B981) else Color(0xFF2D323F),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Live Translation",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isLiveModeEnabled) "Live ON" else "Live",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Microphone Button
            IconButton(
                onClick = {
                    performHaptic()
                    onVoiceClick()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isListeningVoice) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isListeningVoice) Color(0xFFEF4444) else Color(0xFF60A5FA),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Paste Clipboard Button
            IconButton(
                onClick = {
                    performHaptic()
                    onPasteClipboard()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste Clipboard",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // 2. QUICK LANGUAGE SELECTOR CAROUSEL
        AnimatedVisibility(visible = showLanguagePickerFor != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E222B))
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
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF2C3240)
                    ) {
                        Text(
                            text = "${lang.flagEmoji} ${lang.name}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 3. VOICE LISTENING BANNER
        if (isListeningVoice) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3B1D25))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFFEF4444),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = voiceStatusText ?: "Listening... speak now",
                    color = Color(0xFFFECACA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onVoiceClick, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 4. INSTANT CLIPBOARD AUTO-TRANSLATION CHIP
        if (isTranslatingCopied) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFF60A5FA),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Translating copied message...",
                    color = Color(0xFF93C5FD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (!copiedTranslatedText.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F2942))
                    .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📋 Copied: \"${copiedOriginalText?.take(25) ?: ""}\"",
                        color = Color(0xFF93C5FD),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = copiedTranslatedText,
                        color = Color.White,
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
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2563EB)
                ) {
                    Text(
                        text = "Insert ↵",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
        }

        // 5. LIVE TYPING TRANSLATION PREVIEW STRIP
        if (isLiveModeEnabled && liveTypedText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2836))
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
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLiveTranslating) "Translating..." else (liveTranslatedText ?: liveTypedText),
                        color = Color(0xFFA7F3D0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF059669)
                ) {
                    Text(
                        text = "Replace & Send",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 6. MAIN QWERTY / SYMBOL KEYBOARD KEYS
        when (keyMode) {
            KeyboardKeyMode.LETTERS -> LettersKeyboard(
                isShifted = isShifted,
                isCapsLock = isCapsLock,
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
                }
            )
            KeyboardKeyMode.SYMBOLS -> SymbolsKeyboard(
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
            KeyboardKeyMode.EXTRA_SYMBOLS -> ExtraSymbolsKeyboard(
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
private fun LettersKeyboard(
    isShifted: Boolean,
    isCapsLock: Boolean,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftClick: () -> Unit,
    onModeChange: (KeyboardKeyMode) -> Unit
) {
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
    val upper = isShifted || isCapsLock

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { k ->
                val label = if (upper) k.uppercase() else k
                KeyButton(text = label, modifier = Modifier.weight(1f), onClick = { onKeyClick(label) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row2.forEach { k ->
                val label = if (upper) k.uppercase() else k
                KeyButton(text = label, modifier = Modifier.weight(1f), onClick = { onKeyClick(label) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FunctionKeyButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardCapslock,
                        contentDescription = "Shift",
                        tint = if (isCapsLock) Color(0xFF60A5FA) else if (isShifted) Color.White else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.weight(1.4f),
                backgroundColor = if (isShifted || isCapsLock) Color(0xFF374151) else Color(0xFF242936),
                onClick = onShiftClick
            )
            row3.forEach { k ->
                val label = if (upper) k.uppercase() else k
                KeyButton(text = label, modifier = Modifier.weight(1f), onClick = { onKeyClick(label) })
            }
            FunctionKeyButton(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                },
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF242936),
                onClick = onBackspace
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(
                text = "?123",
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF242936),
                fontSize = 13.sp,
                onClick = { onModeChange(KeyboardKeyMode.SYMBOLS) }
            )
            KeyButton(text = ",", modifier = Modifier.weight(1f), onClick = { onKeyClick(",") })
            Surface(
                onClick = onSpace,
                modifier = Modifier.weight(4.4f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2E3442)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "space", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
            KeyButton(text = ".", modifier = Modifier.weight(1f), onClick = { onKeyClick(".") })
            FunctionKeyButton(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF2563EB),
                onClick = onEnter
            )
        }
    }
}

@Composable
private fun SymbolsKeyboard(
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { k -> KeyButton(text = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { k -> KeyButton(text = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton(
                text = "=/<",
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF242936),
                fontSize = 13.sp,
                onClick = { onModeChange(KeyboardKeyMode.EXTRA_SYMBOLS) }
            )
            row3.forEach { k -> KeyButton(text = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
            FunctionKeyButton(
                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = Color.White, modifier = Modifier.size(19.dp)) },
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF242936),
                onClick = onBackspace
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton(
                text = "ABC",
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF242936),
                fontSize = 13.sp,
                onClick = { onModeChange(KeyboardKeyMode.LETTERS) }
            )
            KeyButton(text = ",", modifier = Modifier.weight(1f), onClick = { onKeyClick(",") })
            Surface(
                onClick = onSpace,
                modifier = Modifier.weight(4.4f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2E3442)
            ) {
                Box(contentAlignment = Alignment.Center) { Text(text = "space", color = Color(0xFF94A3B8), fontSize = 12.sp) }
            }
            KeyButton(text = ".", modifier = Modifier.weight(1f), onClick = { onKeyClick(".") })
            FunctionKeyButton(
                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.White, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF2563EB),
                onClick = onEnter
            )
        }
    }
}

@Composable
private fun ExtraSymbolsKeyboard(
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { k -> KeyButton(text = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { k -> KeyButton(text = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton(
                text = "?123",
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF242936),
                fontSize = 13.sp,
                onClick = { onModeChange(KeyboardKeyMode.SYMBOLS) }
            )
            row3.forEach { k -> KeyButton(text = k, modifier = Modifier.weight(1f), onClick = { onKeyClick(k) }) }
            FunctionKeyButton(
                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = Color.White, modifier = Modifier.size(19.dp)) },
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF242936),
                onClick = onBackspace
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyButton(
                text = "ABC",
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF242936),
                fontSize = 13.sp,
                onClick = { onModeChange(KeyboardKeyMode.LETTERS) }
            )
            KeyButton(text = "<", modifier = Modifier.weight(1f), onClick = { onKeyClick("<") })
            Surface(
                onClick = onSpace,
                modifier = Modifier.weight(4.4f).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2E3442)
            ) {
                Box(contentAlignment = Alignment.Center) { Text(text = "space", color = Color(0xFF94A3B8), fontSize = 12.sp) }
            }
            KeyButton(text = ">", modifier = Modifier.weight(1f), onClick = { onKeyClick(">") })
            FunctionKeyButton(
                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color.White, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1.4f),
                backgroundColor = Color(0xFF2563EB),
                onClick = onEnter
            )
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF2E3442),
    fontSize: androidx.compose.ui.unit.TextUnit = 19.sp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FunctionKeyButton(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF242936),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
