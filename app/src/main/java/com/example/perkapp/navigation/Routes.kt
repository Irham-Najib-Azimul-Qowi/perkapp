package com.example.perkapp.navigation

/**
 * Screen — Kumpulan semua rute (alamat) halaman di dalam aplikasi.
 *
 * Seperti daftar URL di website, ini memastikan kita tidak salah ketik nama halaman
 * saat ingin berpindah layar.
 */
sealed class Screen(val route: String) {
    // Halaman Pembuka (Splash Screen)
    object Splash : Screen("splash") // Rute splash
    
    // Fitur Autentikasi (Adam)
    object Login : Screen("login") // Rute login
    object Register : Screen("register") // Rute register
    object Profile : Screen("profile") // Rute profil
    
    // Fitur Kegiatan & Dashboard (Reja)
    object Home : Screen("home") // Rute home dashboard
    object Kegiatan : Screen("kegiatan") // Rute list kegiatan
    object TambahKegiatan : Screen("tambah_kegiatan") // Rute form tambah kegiatan
    
    // Halaman dinamis: memerlukan 'id' agar tahu kegiatan mana yang mau dibuka
    object DetailKegiatan : Screen("detail_kegiatan/{id}") { // Rute detail kegiatan dengan parameter id
        fun createRoute(id: String) = "detail_kegiatan/$id" // Fungsi pembuat rute dinamis detail kegiatan
    }
    object EditKegiatan : Screen("edit_kegiatan/{id}") { // Rute ubah kegiatan dengan parameter id
        fun createRoute(id: String) = "edit_kegiatan/$id" // Fungsi pembuat rute dinamis ubah kegiatan
    }

    // Fitur Alat / Inventaris (Najib)
    object Inventaris : Screen("inventaris") // Rute list inventaris alat
    object TambahAlat : Screen("tambah_alat") // Rute form tambah alat baru
    object TambahAlatLuar : Screen("tambah_alat_luar") // Rute form tambah alat eksternal/luar
    
    object DetailAlat : Screen("detail_alat/{alatId}") { // Rute detail alat dengan parameter alatId
        fun createRoute(alatId: String) = "detail_alat/$alatId" // Fungsi pembuat rute dinamis detail alat
    }
    object EditAlat : Screen("edit_alat/{alatId}") { // Rute ubah alat dengan parameter alatId
        fun createRoute(alatId: String) = "edit_alat/$alatId" // Fungsi pembuat rute dinamis ubah alat
    }
}
