package com.example.perkapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.perkapp.core.navigation.BottomBar
import com.example.perkapp.core.navigation.SetupNavGraph

/**
 * PerkappApp — Kerangka (Layout) utama dari antarmuka aplikasi.
 *
 * Di sini kita merakit komponen UI utama, seperti:
 * 1. Scaffold (Kerangka layar yang bisa diisi header/footer)
 * 2. BottomBar (Menu navigasi di bagian bawah aplikasi)
 * 3. NavGraph (Peta jalan pindah-pindah halaman)
 */
@Composable
fun PerkappApp() {
    // Membuat pengontrol navigasi untuk pindah-pindah layar
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Memasang menu navigasi di bagian bawah layar
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        // innerPadding adalah ruang sisa layar setelah dikurangi tinggi BottomBar.
        // SetupNavGraph akan mengatur layar apa yang sedang aktif/tampil di tengah.
        SetupNavGraph(navController = navController, paddingValues = innerPadding)
    }
}
