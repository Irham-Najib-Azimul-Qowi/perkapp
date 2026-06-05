package com.example.perkapp.core.navigation

// Mengimpor library dasar Jetpack Compose untuk fungsi UI Composable
import androidx.compose.runtime.Composable
// Mengimpor NavHostController untuk mengontrol alur navigasi
import androidx.navigation.NavHostController
// Mengimpor komponen NavHost dan composable untuk membuat grafik navigasi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
// Mengimpor fungsi helper viewModel untuk pembuatan instance ViewModel di Compose
import androidx.lifecycle.viewmodel.compose.viewModel
// Mengimpor HomeScreen buatan Reja (yang berada di package fitur kegiatan)
import com.example.perkapp.features.kegiatan.ui.HomeScreen
// Mengimpor HomeViewModel buatan Reja
import com.example.perkapp.features.kegiatan.ui.HomeViewModel
// Mengimpor FakeKegiatanRepository yang menyuplai data uji coba tanpa API nyata
import com.example.perkapp.features.kegiatan.data.FakeKegiatanRepository
// Mengimpor AktivitasScreen buatan Reja (yang berada di package core kegiatan)
import com.example.perkapp.core.features.kegiatan.ui.AktivitasScreen
// Mengimpor TambahKegiatanScreen buatan Reja
import com.example.perkapp.core.features.kegiatan.ui.TambahKegiatanScreen

/**
 * SetupNavGraph berfungsi mengatur konfigurasi rute navigasi aplikasi.
 *
 * @param navController NavHostController yang mengendalikan transisi antar layar.
 */
@Composable
fun SetupNavGraph(navController: NavHostController) {
    // Menggunakan NavHost sebagai kontainer halaman-halaman yang bisa dikunjungi.
    // Rute awal (startDestination) diarahkan langsung ke "kegiatan" untuk testing Aktivitas/Tambah Kegiatan.
    NavHost(
        navController = navController,
        startDestination = Screen.Kegiatan.route 
    ) {

        // --- BAGIAN ADAM (AUTH) ---
        // Rute untuk layar Splash Screen (menunggu implementasi dari Adam)
        composable(route = Screen.Splash.route) {
            // Nanti diisi SplashScreen() oleh Adam
        }
        // Rute untuk layar Login (menunggu implementasi dari Adam)
        composable(route = Screen.Login.route) {
            // Nanti diisi LoginScreen() oleh Adam
        }
        // Rute untuk layar Register (menunggu implementasi dari Adam)
        composable(route = Screen.Register.route) {
            // Nanti diisi RegisterScreen() oleh Adam
        }

        // --- BAGIAN REJA & NAJIB ---
        
        // Rute untuk layar utama (HomeScreen) milik Reja
        composable(route = "HomeScreen") {
            // Inisialisasi repository tiruan untuk menyuplai data uji coba
            val repository = FakeKegiatanRepository()
            // Inisialisasi HomeViewModel melalui Factory agar parameter repository terpenuhi dan mencegah crash runtime
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.HomeViewModelFactory(repository)
            )

            HomeScreen(
                navController = navController,
                viewModel = homeViewModel,
                // Saat user menekan tombol menu Activities di bottom nav bar, arahkan ke rute kegiatan
                onNavigateToActivities = {
                    navController.navigate(Screen.Kegiatan.route)
                },
                // Saat user menekan tombol menu Inventory di bottom nav bar, arahkan ke rute inventaris
                onNavigateToInventory = {
                    navController.navigate(Screen.Inventaris.route)
                }
            )
        }

        // Rute untuk layar daftar kegiatan/aktivitas (AktivitasScreen) milik Reja
        composable(route = Screen.Kegiatan.route) {
            AktivitasScreen(
                // Mengatur aksi ketika menekan tombol tambah kegiatan (+) untuk membuka TambahKegiatanScreen
                onTambahAktivitas = {
                    navController.navigate(Screen.TambahKegiatan.route)
                },
                // Mengatur aksi ketika menekan salah satu card kegiatan untuk melihat detailnya
                onDetailAktivitas = { id ->
                    navController.navigate(Screen.DetailKegiatan.createRoute(id))
                }
            )
        }

        // Rute untuk layar tambah kegiatan (TambahKegiatanScreen) milik Reja
        composable(route = Screen.TambahKegiatan.route) {
            TambahKegiatanScreen(navController = navController)
        }

        // Rute untuk layar inventaris barang (InventarisScreen) milik Najib
        composable(route = Screen.Inventaris.route) {
            // Nanti diisi InventarisScreen() punya Najib
        }
    }
}
