package com.example.perkapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.perkapp.ui.components.BottomBar
import com.example.perkapp.navigation.SetupNavGraph

/**
 * PerkappApp — Kerangka (Layout) utama dari antarmuka aplikasi.
 *
 * Di sini kita merakit komponen UI utama, seperti:
 * 1. Scaffold (Kerangka layar yang bisa diisi header/footer)
 * 2. BottomBar (Menu navigasi di bagian bawah aplikasi)
 * 3. NavGraph (Peta jalan pindah-pindah halaman)
 */
/**
 * FUNGSI: PerkappApp
 * 
 * TUJUAN:
 * Membangun kerangka (layout) struktural UI tingkat paling atas aplikasi. 
 * Fungsi ini mengikat sistem navigasi dengan komponen antar-layar (seperti menu navigasi bawah).
 * 
 * ALUR LOGIKA PENGERJAAN:
 * 1. Menginisialisasi `navController` (pengendali navigasi bawaan Jetpack Compose).
 *    Objek ini berfungsi mengingat "sejarah" layar yang dikunjungi user agar bisa kembali (back) dengan aman.
 * 2. Memanggil fungsi `Scaffold`, yaitu kerangka standar Material Design.
 * 3. Di dalam Scaffold, kita memberikan argumen `bottomBar` untuk merender menu bawah (`BottomBar`),
 *    dan me-passing `navController` ke dalamnya agar tombol-tombol menu tahu harus ke layar mana.
 * 4. `Scaffold` akan menghitung ruang yang tersisa (mengurangi area menu bawah) dan 
 *    mengirimkannya sebagai `innerPadding`.
 * 5. Memanggil `SetupNavGraph`, yang bertindak sebagai "peta" rute. Komponen ini 
 *    ditempatkan di tengah layar dan akan berganti-ganti konten sesuai instruksi `navController`.
 */
@Composable
fun PerkappApp() {
    // 1. Membuat pengontrol navigasi untuk pindah-pindah layar
    val navController = rememberNavController()
    
    // 2. Scaffold sebagai kerangka visual
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 3. Memasang menu navigasi di bagian bawah layar
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        // 4 & 5. SetupNavGraph merender layar utama sesuai padding dari Scaffold
        SetupNavGraph(navController = navController, paddingValues = innerPadding)
    }
}
