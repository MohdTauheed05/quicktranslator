package com.example.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Language
import com.example.ui.components.BusinessModeBadge
import com.example.ui.components.LanguageSelectorModal
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToTranslation: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val sourceLang by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val targetLang by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isBusinessMode by viewModel.businessModeEnabled.collectAsStateWithLifecycle()
    val floatingBubbleEnabled by viewModel.floatingBubbleEnabled.collectAsStateWithLifecycle()
    val showOverlayDialog by viewModel.showOverlayPermissionDialog.collectAsStateWithLifecycle()
    val currentResult by viewModel.currentResult.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    fun readClipboardAndTranslate() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clipDesc = clipboard.primaryClipDescription
            if (clipDesc != null && clipDesc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                val item = clipboard.primaryClip?.getItemAt(0)
                val text = item?.text?.toString()?.trim()
                if (!text.isNullOrBlank()) {
                    viewModel.translate(text)
                    onNavigateToTranslation()
                    return
                }
            }
        }
        viewModel.translate(inputText)
        if (inputText.isNotBlank()) {
            onNavigateToTranslation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "QUICKTRANSLATE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "100% Free · Universal Assistant",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Free Unlimited Banner with Business Mode Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BusinessModeBadge(
                    isEnabled = isBusinessMode,
                    onClick = { viewModel.toggleBusinessMode() }
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Free & Unlimited",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // NEW: Instant WhatsApp Floating Bubble Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floating_bubble_banner_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (floatingBubbleEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (floatingBubbleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BubbleChart,
                                contentDescription = null,
                                tint = if (floatingBubbleEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Instant Floating Bubble",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (floatingBubbleEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (floatingBubbleEnabled) "Active over WhatsApp! Tap the bubble to translate copied messages." else "Show floating bubble over WhatsApp for instant 1-tap translation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (floatingBubbleEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = floatingBubbleEnabled,
                        onCheckedChange = { viewModel.toggleFloatingBubble(context) },
                        modifier = Modifier.testTag("floating_bubble_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Translation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_translation_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Language Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Source Language Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSourcePicker = true }
                                .testTag("source_language_button"),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = "Source Language",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(text = sourceLang.flagEmoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sourceLang.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Swap Button
                        IconButton(
                            onClick = { viewModel.swapLanguages() },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("swap_languages_button")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Swap Languages",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Target Language Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showTargetPicker = true }
                                .testTag("target_language_button"),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = "Target Language",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(text = targetLang.flagEmoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = targetLang.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_text_input"),
                        placeholder = {
                            Text(
                                "Type or paste message here...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateInputText("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Action: [ Paste & Translate ]
                    Button(
                        onClick = { readClipboardAndTranslate() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("paste_and_translate_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (inputText.isNotBlank()) "Translate Message" else "Paste & Translate",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Actions Section
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.updateInputText("مرحبا، كيف حالك؟")
                            viewModel.translate("مرحبا، كيف حالك؟")
                            onNavigateToTranslation()
                        }
                        .testTag("quick_action_demo_share"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Text",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try Arabic",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val businessSample = "MOQ 500 PCS\nPrice USD 0.075/PC\nFOB Ningbo\nPayment 30% advance"
                            viewModel.updateInputText(businessSample)
                            viewModel.translate(businessSample)
                            onNavigateToTranslation()
                        }
                        .testTag("quick_action_type_text"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = "Business Sample",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Quotation Demo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onNavigateToHistory)
                        .testTag("quick_action_history"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onNavigateToFavorites)
                        .testTag("quick_action_favorites"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Favorites",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Recent Translation Quick Card (if exists)
            if (currentResult != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTranslation() }
                        .testTag("recent_translation_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LATEST RESULT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentResult?.translatedText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // How Instant WhatsApp Translation Works
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "2 Ways to Translate Instantly Inside WhatsApp",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Floating Bubble: Enable the switch above. When you copy a message in WhatsApp, tap the floating bubble to see the instant translation popup directly over your chat.\n\n2. Highlight & QuickTranslate: Select message text in WhatsApp -> tap [ QuickTranslate ] in the popup menu to translate without leaving the app.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOverlayPermissionDialog() },
            title = { Text("Enable Display Over Other Apps") },
            text = {
                Text("To show the instant translation popup and floating bubble directly over WhatsApp, Android requires the 'Display over other apps' permission.")
            },
            confirmButton = {
                Button(onClick = { viewModel.requestOverlayPermission(context) }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOverlayPermissionDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSourcePicker) {
        LanguageSelectorModal(
            title = "Select Source Language",
            currentLanguage = sourceLang,
            allowAutoDetect = true,
            onLanguageSelected = { viewModel.setSourceLanguage(it) },
            onDismiss = { showSourcePicker = false }
        )
    }

    if (showTargetPicker) {
        LanguageSelectorModal(
            title = "Select Target Language",
            currentLanguage = targetLang,
            allowAutoDetect = false,
            onLanguageSelected = { viewModel.setTargetLanguage(it) },
            onDismiss = { showTargetPicker = false }
        )
    }
}
