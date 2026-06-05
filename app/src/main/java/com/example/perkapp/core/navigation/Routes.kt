package com.example.perkapp.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Inventaris : Screen("inventaris")
    object Kegiatan : Screen("kegiatan")
    object Profile : Screen("profile")
    object TambahKegiatan : Screen("tambah_kegiatan")
    object DetailKegiatan : Screen("detail_kegiatan/{id}") {
        fun createRoute(id: String) = "detail_kegiatan/$id"
    }
    // Najib's routes
    object TambahAlat : Screen("tambah_alat")
    object DetailAlat : Screen("detail_alat/{alatId}") {
        fun createRoute(alatId: String) = "detail_alat/$alatId"
    }
    object EditAlat : Screen("edit_alat/{alatId}") {
        fun createRoute(alatId: String) = "edit_alat/$alatId"
    }
}