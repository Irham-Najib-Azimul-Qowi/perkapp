package com.example.perkapp.core.navigation

/**
 * Screen — Kumpulan semua rute (alamat) halaman di dalam aplikasi.
 *
 * Seperti daftar URL di website, ini memastikan kita tidak salah ketik nama halaman
 * saat ingin berpindah layar.
 */
sealed class Screen(val route: String) {
    // Halaman Pembuka
    object Splash : Screen("splash")
    
    // Fitur Autentikasi (Adam)
    object Login : Screen("login")
    object Register : Screen("register")
    object Profile : Screen("profile")
    
    // Fitur Kegiatan & Dashboard (Reja)
    object Home : Screen("home")
    object Kegiatan : Screen("kegiatan")
    object TambahKegiatan : Screen("tambah_kegiatan")
    
    // Halaman dinamis: memerlukan 'id' agar tahu kegiatan mana yang mau dibuka
    object DetailKegiatan : Screen("detail_kegiatan/{id}") {
        fun createRoute(id: String) = "detail_kegiatan/$id"
    }
    object EditKegiatan : Screen("edit_kegiatan/{id}") {
        fun createRoute(id: String) = "edit_kegiatan/$id"
    }

    // Fitur Alat / Inventaris (Najib)
    object Inventaris : Screen("inventaris")
    object TambahAlat : Screen("tambah_alat")
    object TambahAlatLuar : Screen("tambah_alat_luar")
    
    object DetailAlat : Screen("detail_alat/{alatId}") {
        fun createRoute(alatId: String) = "detail_alat/$alatId"
    }
    object EditAlat : Screen("edit_alat/{alatId}") {
        fun createRoute(alatId: String) = "edit_alat/$alatId"
    }
}