package com.example.ui.screens

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.QuickTranslateApp
import com.example.domain.model.Language
import com.example.ui.theme.QuickTranslateTheme
import com.example.ui.viewmodel.MainViewModel

class FloatingTranslateActivity : ComponentActivity() {

    private val app by lazy { application as QuickTranslateApp }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            translationRepository = app.translationRepository,
            userPreferences = app.userPreferences,
            billingRepository = app.billingRepository,
            ttsManager = app.ttsManager
        )
    }

    private val textToTranslateState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textFromIntent = extractTextFromIntent(intent)
        if (!textFromIntent.isNullOrBlank()) {
            textToTranslateState.value = textFromIntent
        } else {
            val clipText = readClipboardText()
            if (!clipText.isNullOrBlank()) {
                textToTranslateState.value = clipText
            }
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val textToTranslate by textToTranslateState

            QuickTranslateTheme(themeMode = themeMode) {
                // Dimmed translucent backdrop that closes dialog on outside tap
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { finish() }
                        )
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingPopupCard(
                        viewModel = viewModel,
                        initialText = textToTranslate,
                        onDismiss = { finish() },
                        onCopyAndClose = { text ->
                            copyToClipboard(text)
                            Toast.makeText(this@FloatingTranslateActivity, "Translated text copied!", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onPasteRequested = {
                            val clip = readClipboardText()
                            if (!clip.isNullOrBlank()) {
                                textToTranslateState.value = clip
                                viewModel.translate(clip)
                                true
                            } else {
                                Toast.makeText(this@FloatingTranslateActivity, "Clipboard is empty. Copy text in WhatsApp first!", Toast.LENGTH_SHORT).show()
                                false
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkAndAutoTranslateClipboard()
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post {
            checkAndAutoTranslateClipboard()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val text = extractTextFromIntent(intent)
        if (!text.isNullOrBlank()) {
            textToTranslateState.value = text
            viewModel.translate(text)
        } else {
            checkAndAutoTranslateClipboard(force = true)
        }
    }

    private fun checkAndAutoTranslateClipboard(force: Boolean = false) {
        val clip = readClipboardText()
        if (!clip.isNullOrBlank()) {
            val lastResult = viewModel.currentResult.value
            if (force || lastResult == null || lastResult.originalText != clip) {
                textToTranslateState.value = clip
                viewModel.translate(clip)
            }
        }
    }

    private fun extractTextFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else null
            }
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            ACTION_TRANSLATE_CLIPBOARD -> {
                intent.getStringExtra(EXTRA_TEXT)
            }
            else -> intent.getStringExtra(EXTRA_TEXT)
        }
    }

    private fun readClipboardText(): String? {
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
            if (!clipboard.hasPrimaryClip()) return null
            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            val item = clip.getItemAt(0) ?: return null
            val text = item.coerceToText(this)?.toString()?.trim()
            if (text.isNullOrBlank()) null else text
        } catch (e: Exception) {
            null
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QuickTranslate", text)
        clipboard.setPrimaryClip(clip)
    }

    companion object {
        const val ACTION_TRANSLATE_CLIPBOARD = "com.example.ACTION_TRANSLATE_CLIPBOARD"
        const val EXTRA_TEXT = "extra_text"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingPopupCard(
    viewModel: MainViewModel,
    initialText: String?,
    onDismiss: () -> Unit,
    onCopyAndClose: (String) -> Unit,
    onPasteRequested: () -> Boolean = { false }
) {
    val currentResult by viewModel.currentResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isBusinessMode by viewModel.businessModeEnabled.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSavedCurrent.collectAsStateWithLifecycle()

    var inputToTranslate by remember { mutableStateOf(initialText ?: "") }
    var manualInputText by remember { mutableStateOf("") }

    LaunchedEffect(initialText) {
        if (!initialText.isNullOrBlank()) {
            inputToTranslate = initialText
            viewModel.translate(initialText)
        } else if (currentResult == null) {
            kotlinx.coroutines.delay(150)
            onPasteRequested()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* consume click */ }
            )
            .testTag("floating_popup_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Instant Translation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("floating_popup_close_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val result = currentResult

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Translating message...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (result != null && result.translatedText.isNotBlank()) {
                // Language info chip row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${result.detectedSourceLanguage.flagEmoji} ${result.detectedSourceLanguage.name} → ${result.targetLanguage.flagEmoji} ${result.targetLanguage.name}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.swapLanguages() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Swap", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { viewModel.translate(result.originalText) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Re-translate", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Original Text Preview (compact)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val srcDir = if (result.detectedSourceLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides srcDir) {
                        Text(
                            text = result.originalText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Translated Text (large & clear)
                val targetDir = if (result.targetLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides targetDir) {
                    Text(
                        text = result.translatedText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("floating_translation_text")
                    )
                }

                // Preserved commercial tokens in Business Mode
                if (result.preservedTokens.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        result.preservedTokens.forEach { token ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = token,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Primary CTA [ Copy & Reply / Close ]
                Button(
                    onClick = { onCopyAndClose(result.translatedText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_and_close_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Copy Translation & Back to Chat", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick translate next copied message button
                OutlinedButton(
                    onClick = { onPasteRequested() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "📋 Translate Next Copied Message", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Speak
                    OutlinedButton(
                        onClick = {
                            if (isSpeaking) viewModel.stopSpeaking()
                            else viewModel.speak(result.translatedText, result.targetLanguage)
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSpeaking) "Stop" else "Speak", fontSize = 13.sp)
                    }

                    // Save
                    OutlinedButton(
                        onClick = { viewModel.saveCurrentTranslation() },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSaved) "Saved" else "Save", fontSize = 13.sp)
                    }
                }
            } else {
                // Empty clipboard / no text yet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Instant WhatsApp Translation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Copy any message in your chat, then tap 'Paste' below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Paste Button
                    Button(
                        onClick = {
                            val success = onPasteRequested()
                            if (!success && manualInputText.isNotBlank()) {
                                viewModel.translate(manualInputText)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("floating_popup_paste_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "📋 Paste Copied Message", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        placeholder = {
                            Text("Or type/paste text directly here...", style = MaterialTheme.typography.bodySmall)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (manualInputText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.translate(manualInputText) },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Translate This Message", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
