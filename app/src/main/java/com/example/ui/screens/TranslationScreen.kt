package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Language
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TranslationScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentResult by viewModel.currentResult.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSavedCurrent.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()

    var copiedConfirmation by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Translated Text", text)
        clipboard.setPrimaryClip(clip)
        copiedConfirmation = true
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Translation"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Translation Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("translation_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleCurrentFavorite() },
                        modifier = Modifier.testTag("favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val result = currentResult

            if (result == null || result.originalText.isBlank()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No translation available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go to Home")
                        }
                    }
                }
            } else {
                // Provider & Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (result.isDemo) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.testTag("demo_mode_badge")
                        ) {
                            Text(
                                text = "Demo Translation",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = result.providerName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (result.isBusinessModeApplied) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.testTag("business_mode_applied_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BusinessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Commercial Protection Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SOURCE CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("source_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SOURCE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Detected: ${result.detectedSourceLanguage.flagEmoji} ${result.detectedSourceLanguage.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // RTL Support for Source Language (e.g. Arabic, Urdu, Persian)
                        val sourceDirection = if (result.detectedSourceLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                        CompositionLocalProvider(LocalLayoutDirection provides sourceDirection) {
                            Text(
                                text = result.originalText,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("source_original_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = {
                                    if (isSpeaking) viewModel.stopSpeaking()
                                    else viewModel.speak(result.originalText, result.detectedSourceLanguage)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak Source",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { copyToClipboard(result.originalText) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Source",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Swap / Re-translate control bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.swapLanguages() },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("swap_in_translation_button")
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Swap Languages", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.translate(result.originalText) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("translate_again_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Translate Again", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TRANSLATION CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("translation_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRANSLATION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${result.targetLanguage.flagEmoji} ${result.targetLanguage.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // RTL Support for Target Language
                        val targetDirection = if (result.targetLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                        CompositionLocalProvider(LocalLayoutDirection provides targetDirection) {
                            Text(
                                text = result.translatedText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 19.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("translation_result_text")
                            )
                        }

                        // Preserved Commercial Tokens preview in Business Mode
                        if (result.preservedTokens.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Commercial Values Preserved Exactly:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.preservedTokens.forEach { token ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.testTag("preserved_token_chip")
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

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Buttons: [ Copy ] [ Speak ] [ Share ] [ Save ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Copy Button
                            Button(
                                onClick = { copyToClipboard(result.translatedText) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("copy_translation_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (copiedConfirmation) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (copiedConfirmation) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (copiedConfirmation) "Copied" else "Copy",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Speak Button
                            Button(
                                onClick = {
                                    if (isSpeaking) viewModel.stopSpeaking()
                                    else viewModel.speak(result.translatedText, result.targetLanguage)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("speak_translation_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSpeaking) "Stop" else "Speak",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Share Button
                            OutlinedButton(
                                onClick = { shareText(result.translatedText) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("share_translation_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontWeight = FontWeight.SemiBold)
                            }

                            // Save Button (Explicit user action!)
                            OutlinedButton(
                                onClick = { viewModel.saveCurrentTranslation() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("save_translation_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Save,
                                    contentDescription = null,
                                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSaved) "Saved" else "Save",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close / Back CTA
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_translation_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
