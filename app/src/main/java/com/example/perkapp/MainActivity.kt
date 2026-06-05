package com.example.perkapp

// Mengimpor library OS Android dasar untuk mengelola daur hidup/state
import android.os.Bundle
// Mengimpor ComponentActivity sebagai base class activity yang support Jetpack Compose
import androidx.activity.ComponentActivity
// Mengimpor setContent untuk menghubungkan UI Compose ke activity ini
import androidx.activity.compose.setContent
// Mengimpor enableEdgeToEdge untuk membuat tampilan full screen sampai ke bar status & navigasi sistem
import androidx.activity.enableEdgeToEdge
// Mengimpor rememberNavController untuk menginisialisasi controller navigasi
import androidx.navigation.compose.rememberNavController
// Mengimpor SetupNavGraph yang berisi kumpulan rute navigasi aplikasi
import com.example.perkapp.core.navigation.SetupNavGraph
// Mengimpor tema kustom aplikasi Perkapp
import com.example.perkapp.ui.theme.PerkappTheme
// Mengimpor anotasi AndroidEntryPoint agar kelas Activity ini bisa di-inject dependensi oleh Hilt
import dagger.hilt.android.AndroidEntryPoint

// Menandai MainActivity sebagai titik masuk (entry point) untuk komponen Hilt Android
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Fungsi onCreate dipanggil saat activity pertama kali dibuat
    override fun onCreate(savedInstanceState: Bundle?) {
        // Memanggil fungsi onCreate dari superclass (ComponentActivity)
        super.onCreate(savedInstanceState)
        // Mengaktifkan mode Edge-to-Edge agar layout aplikasi bisa menggunakan seluruh area layar
        enableEdgeToEdge()
        // Mengatur tampilan konten utama menggunakan Jetpack Compose
        setContent {
            // Membungkus seluruh tampilan dengan tema PerkappTheme agar style/warna konsisten
            PerkappTheme {
                // Membuat dan menyimpan instance NavController agar bisa memantau dan mengontrol perpindahan halaman
                val navController = rememberNavController()
                // Memanggil rute-rute navigasi (NavGraph) yang telah dikonfigurasi
                SetupNavGraph(navController = navController)
            }
        }
    }
}