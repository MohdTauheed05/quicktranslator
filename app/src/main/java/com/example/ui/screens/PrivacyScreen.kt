package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showFullPolicyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy & Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("privacy_back_button")) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Privacy-First Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "QuickTranslate is built with strict privacy and zero covert background monitoring.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clarifications Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What does QuickTranslate process?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val points = listOf(
                        "Only text you explicitly choose to translate is processed.",
                        "Text may be sent via secure HTTPS to the active translation engine to generate the translation.",
                        "QuickTranslate does not continuously monitor or store clipboard history secretly.",
                        "QuickTranslate does not collect your WhatsApp conversations, contacts, or messages.",
                        "Saved translations remain stored locally on your device only when you explicitly tap [ Save ].",
                        "No background accessibility inspection or intrusive overlays are used."
                    )

                    points.forEach { point ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(
                        onClick = { showFullPolicyDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("view_privacy_policy_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Complete Privacy Policy", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delete_my_data_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All My Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete All Data Permanently?") },
            text = { Text("This will immediately delete all saved translation history, favorites, and reset all app preferences. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllUserData()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFullPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showFullPolicyDialog = false },
            title = { Text("Privacy Policy - QuickTranslate") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = """
QuickTranslate Privacy Policy
Effective Date: 2026-09-24

1. Purpose
QuickTranslate provides instant translation assistance for text shared or copied by the user.

2. Information We Process
- Input Text: Only the text you intentionally submit via Share or Paste is processed for the sole purpose of translating it.
- Device State: We do not collect device identifiers, advertising IDs, or contact lists.
- Clipboard: When you trigger a translation or open QuickTranslate with the clipboard setting enabled, the app inspects the current clipboard content strictly to fulfill your translation request. It does not record or transmit clipboard history.

3. Local Storage
- Any history or saved phrases are stored strictly locally in your device's private SQLite database.
- You can delete any saved item or tap "Delete All My Data" at any time.

4. Third-Party Services
- Translations may be processed using standard neural cloud translation engines via encrypted HTTPS. No personally identifying information is attached to the request.
- Google Play Billing is used for subscription handling according to Google Play Store policies.

5. Compliance
- This application complies with Google Play Developer Program policies, User Data policies, and does not access unauthorized APIs or SMS/Call logs.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullPolicyDialog = false }) {
                    Text("I Understand")
                }
            }
        )
    }
}
