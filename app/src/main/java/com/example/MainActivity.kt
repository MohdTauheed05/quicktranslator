package com.example

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingDialog
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TranslationScreen
import com.example.ui.theme.QuickTranslateTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val app by lazy { application as QuickTranslateApp }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            translationRepository = app.translationRepository,
            userPreferences = app.userPreferences,
            billingRepository = app.billingRepository,
            ttsManager = app.ttsManager
        )
    }

    private var pendingSharedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val targetLang by viewModel.targetLanguage.collectAsStateWithLifecycle()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()

            QuickTranslateTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Handle pending shared text once navController is active
                    LaunchedEffect(pendingSharedText) {
                        val text = pendingSharedText
                        if (!text.isNullOrBlank()) {
                            viewModel.processSharedText(text)
                            navController.navigate(Screen.Translation.route) {
                                launchSingleTop = true
                            }
                            pendingSharedText = null
                        }
                    }

                    // Toast message collector
                    LaunchedEffect(Unit) {
                        viewModel.toastEvent.collect { message ->
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Onboarding dialog on first run
                    if (!hasCompletedOnboarding) {
                        OnboardingDialog(
                            initialTargetLang = targetLang,
                            onFinish = { chosenLang ->
                                viewModel.completeOnboarding(chosenLang)
                            }
                        )
                    }

                    QuickTranslateNavHost(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeCheckBubble(this)
        checkClipboardIfEnabled()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!shared.isNullOrBlank()) {
                        pendingSharedText = shared
                    }
                }
            }
            Intent.ACTION_PROCESS_TEXT -> {
                val selected = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (!selected.isNullOrBlank()) {
                    pendingSharedText = selected
                }
            }
        }
    }

    private fun checkClipboardIfEnabled() {
        if (!viewModel.autoClipboardEnabled.value) return

        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clipDesc = clipboard.primaryClipDescription
                if (clipDesc != null && clipDesc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    val item = clipboard.primaryClip?.getItemAt(0)
                    val text = item?.text?.toString()?.trim()
                    // Only auto-populate if input is currently empty and clipboard has meaningful text
                    if (!text.isNullOrBlank() && viewModel.inputText.value.isBlank()) {
                        viewModel.updateInputText(text)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore security exception if restricted by OS
        }
    }
}

@Composable
fun QuickTranslateNavHost(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToTranslation = { navController.navigate(Screen.Translation.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Translation.route) {
            TranslationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
