package com.example.perkapp.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Inventaris : Screen("inventaris")
    object Kegiatan : Screen("kegiatan")
    object DetailKegiatan : Screen("detail_kegiatan/{id}") {
        fun createRoute(id: String) = "detail_kegiatan/$id"
    }
}