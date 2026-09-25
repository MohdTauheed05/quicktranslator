package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.ThemeMode
import com.example.ui.components.LanguageSelectorModal
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val sourceLang by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val targetLang by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val isBusinessMode by viewModel.businessModeEnabled.collectAsStateWithLifecycle()
    val autoClipboard by viewModel.autoClipboardEnabled.collectAsStateWithLifecycle()
    val floatingBubbleEnabled by viewModel.floatingBubbleEnabled.collectAsStateWithLifecycle()
    val showOverlayDialog by viewModel.showOverlayPermissionDialog.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Free Tier / Unlimited Badge Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_free_unlimited_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "100% Free & Unlimited",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "No subscriptions, no limits, no paywalls. All features completely unlocked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: WhatsApp & Floating Assistant
            Text(
                text = "INSTANT ASSISTANT (WHATSAPP & APPS)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Floating Bubble Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Floating Assistant Bubble", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.BubbleChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "Shows a floating quick icon over WhatsApp. Copy any message and tap the bubble to see the instant translation popup directly in WhatsApp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = floatingBubbleEnabled,
                            onCheckedChange = { viewModel.toggleFloatingBubble(context) },
                            modifier = Modifier.testTag("settings_floating_bubble_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Auto Clipboard Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(text = "Auto Clipboard Detection", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "When opening QuickTranslate, checks clipboard for copied text. Never monitors or collects history secretly in the background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoClipboard,
                            onCheckedChange = { viewModel.toggleAutoClipboard() },
                            modifier = Modifier.testTag("auto_clipboard_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Languages
            Text(
                text = "TRANSLATION PREFERENCES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Target Language Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTargetPicker = true }
                            .padding(16.dp)
                            .testTag("settings_target_language_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Target Language", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(text = "Primary language for translations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${targetLang.flagEmoji} ${targetLang.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Source Language Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSourcePicker = true }
                            .padding(16.dp)
                            .testTag("settings_source_language_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Source Language", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(text = "Default input language", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${sourceLang.flagEmoji} ${sourceLang.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Business Mode Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Business Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.Default.BusinessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "Protects commercial values (prices, MOQ, FOB terms, SKU, %) from alteration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBusinessMode,
                            onCheckedChange = { viewModel.toggleBusinessMode() },
                            modifier = Modifier.testTag("business_mode_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Appearance & Theme
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Theme Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(text = "Light, Dark, or System Default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
                            val isSelected = themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .testTag("theme_button_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Privacy & Information
            Text(
                text = "PRIVACY & ABOUT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToPrivacy)
                            .padding(16.dp)
                            .testTag("settings_privacy_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Privacy & Data Controls", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToAbout)
                            .padding(16.dp)
                            .testTag("settings_about_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "About QuickTranslate", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
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
            title = "Select Default Source Language",
            currentLanguage = sourceLang,
            allowAutoDetect = true,
            onLanguageSelected = { viewModel.setSourceLanguage(it) },
            onDismiss = { showSourcePicker = false }
        )
    }

    if (showTargetPicker) {
        LanguageSelectorModal(
            title = "Select Default Target Language",
            currentLanguage = targetLang,
            allowAutoDetect = false,
            onLanguageSelected = { viewModel.setTargetLanguage(it) },
            onDismiss = { showTargetPicker = false }
        )
    }
}
