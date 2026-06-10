/**
 * File: MainActivity.kt
 *
 * FUNGSI UTAMA:
 * File ini berisi Activity tunggal (satu-satunya) yang menjadi titik masuk (entry point)
 * dari seluruh antarmuka pengguna aplikasi Perkapp.
 *
 * PENJELASAN MENDALAM:
 * Dalam arsitektur Jetpack Compose, seluruh UI dibangun menggunakan fungsi @Composable.
 * Oleh karena itu, proyek hanya membutuhkan SATU Activity (MainActivity) yang bertugas:
 *   1. Menginisialisasi tema visual (PerkappTheme).
 *   2. Memanggil fungsi root Composable (PerkappApp) yang mengatur seluruh navigasi halaman.
 *   3. Menjalankan sinkronisasi data pending saat aplikasi pertama dibuka (jika user sudah login).
 *   4. Menjadwalkan pekerja background (WorkManager) agar data otomatis tersinkronisasi
 *      ketika koneksi internet kembali tersedia.
 *
 * Anotasi @AndroidEntryPoint dari Hilt menandakan bahwa Activity ini mendukung
 * injeksi dependensi secara otomatis (Dependency Injection).
 *
 * PERAN DALAM ARSITEKTUR:
 * MainActivity → PerkappApp (Scaffold + BottomBar) → NavGraph → Screen-screen Compose
 */
package com.example.perkapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.perkapp.network.RetrofitClient
import com.example.perkapp.sync.SyncManager
import com.example.perkapp.ui.theme.PerkappTheme
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

import com.example.perkapp.database.UserPreferences
import com.example.perkapp.database.dataStore
import kotlinx.coroutines.flow.first

/**
 * MainActivity — Activity tunggal yang berfungsi sebagai wadah (host) seluruh UI Compose.
 *
 * Tidak ada Activity lain di proyek ini. Semua perpindahan halaman dilakukan
 * melalui NavController di dalam Composable, BUKAN melalui Intent antar-Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * FUNGSI: onCreate
     * 
     * TUJUAN:
     * Metode pendahulu yang dipanggil pertama kali oleh siklus hidup (lifecycle) Android 
     * ketika aplikasi dibuka. Berperan untuk menginisialisasi status sinkronisasi,
     * menyesuaikan tampilan tepi layar, dan menyuntikkan (inject) UI Jetpack Compose.
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. `super.onCreate` memanggil bawaan sistem untuk inisialisasi dasar.
     * 2. `enableEdgeToEdge()` membuat UI mengambil seluruh ruang layar, termasuk area di bawah status bar dan navigation bar.
     * 3. `lifecycleScope.launch` memulai *coroutine* (proses berjalan paralel) yang aman tanpa memory leak.
     * 4. Mengambil data token *user* dari `DataStore` (UserPreferences) secara sinkron untuk mengecek apakah user login.
     * 5. Jika token ditemukan (user login) dan perangkat terkoneksi internet (`NetworkUtils.isOnline`), 
     *    segera paksa sinkronisasi dengan fungsi `SyncManager.syncNow()`.
     * 6. Memanggil `SyncManager.scheduleSyncWhenOnline` agar sistem Android otomatis 
     *    mensinkronisasi data di masa depan kapanpun internet menyala (Worker).
     * 7. `setContent` dipanggil untuk menghubungkan UI berbasis kode (Compose) ke Android sistem.
     *    Semua komponen dimasukkan ke dalam `PerkappTheme` agar warnanya selaras.
     * 
     * @param savedInstanceState Bundle dari sistem operasi Android yang berisi status layar (jika sebelumnya diminimize/diputar).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Mengaktifkan tampilan layar penuh (immersive edge-to-edge)
        enableEdgeToEdge()

        // 2. Blok Sinkronisasi Awal (Eager Sync)
        // Coroutine ini akan otomatis dihentikan jika Activity ini ditutup (onDestroy)
        lifecycleScope.launch {
            // Mengambil preferensi pengguna secara lokal
            val userPrefs = UserPreferences(applicationContext.dataStore)
            val token = userPrefs.getAuthToken.first() // first() menunggu hingga nilai pertama terbaca
            
            // Cek kondisi login dan jaringan
            if (!token.isNullOrBlank() && com.example.perkapp.util.NetworkUtils.isOnline(applicationContext)) {
                // Mendorong data lokal (Room) ke server (API) secara langsung di awal
                SyncManager.syncNow(applicationContext)
            }
        }

        // 3. Menjadwalkan sinkronisasi asinkron (Lazy Sync)
        // Meski tidak ada internet saat ini, sistem akan mengingat untuk sinkronisasi saat internet tersedia
        SyncManager.scheduleSyncWhenOnline(applicationContext)

        // 4. Memasang Akar UI (Root User Interface)
        setContent {
            // Membungkus aplikasi dengan tema utama proyek Perkapp
            PerkappTheme {
                // Memanggil Composable utama yang berisi navigasi layar
                PerkappApp()
            }
        }
    }
}
