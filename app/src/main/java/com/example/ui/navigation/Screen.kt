package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Tasks : Screen("tasks")
    object Family : Screen("family")
    object Settings : Screen("settings")
}
