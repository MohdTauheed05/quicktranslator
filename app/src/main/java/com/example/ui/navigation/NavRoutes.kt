package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Translation : Screen("translation")
    object History : Screen("history")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object Privacy : Screen("privacy")
    object About : Screen("about")
}
