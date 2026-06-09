package com.example.perkapp.core.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.core.features.kegiatan.ui.*
import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.repository.AlatRepository
import com.example.perkapp.features.alat.ui.screen.DetailAlatScreen
import com.example.perkapp.features.alat.ui.screen.EditAlatScreen
import com.example.perkapp.features.alat.ui.screen.InventarisScreen
import com.example.perkapp.features.alat.ui.screen.TambahAlatScreen
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModelFactory
import com.example.perkapp.features.auth.ui.LoginScreen
import com.example.perkapp.features.auth.ui.ProfileScreen
import com.example.perkapp.features.auth.ui.RegisterScreen
import com.example.perkapp.features.auth.ui.SplashScreen
import com.example.perkapp.features.kegiatan.data.FakeKegiatanRepository
import com.example.perkapp.features.kegiatan.ui.HomeScreen
import com.example.perkapp.features.kegiatan.ui.HomeViewModel

/**
 * SetupNavGraph — Peta Jalan Aplikasi.
 *
 * Mengatur layar apa yang harus ditampilkan berdasarkan rute saat ini.
 * Juga bertugas menyuntikkan (inject) ViewModel ke masing-masing layar.
 */
/**
 * FUNGSI: SetupNavGraph
 * 
 * TUJUAN:
 * Memetakan seluruh rute layar (Screen) dalam aplikasi layaknya "Peta Jalan".
 * Fungsi ini bertugas menerjemahkan string rute (contoh: "home", "login") 
 * menjadi wujud antarmuka visual (`@Composable`) yang akan digambar di layar HP.
 * 
 * ALUR LOGIKA PENGERJAAN:
 * 1. Mendaftarkan `AlatViewModel` di tingkat *Activity* (Root Scope) agar state alat-alat
 *    bisa dipakai bersama antara fitur Tambah Alat, Daftar Alat, dan Form Peminjaman Alat.
 * 2. Membuat `NavHost` yang mendengarkan perubahan URL/rute dari `navController`.
 * 3. Mengatur layar pembuka perdana (`startDestination`) jatuh pada `SplashScreen`.
 * 4. Mendefinisikan setiap rute dengan blok `composable()`. Jika pengguna pindah ke "login",
 *    Render `LoginScreen` dan injeksikan fungsi-fungsi navigasi tambahannya.
 * 5. Menangani argumen dinamis (contoh: ID alat/kegiatan) menggunakan `navArgument` 
 *    pada rute seperti detail atau edit.
 * 
 * @param navController Pengatur kemudi utama navigasi bawaan Compose.
 * @param paddingValues Jarak (margin) dari elemen induk (biasanya `Scaffold`) agar UI tidak tertutup BottomBar.
 */
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues = PaddingValues()
) {
    // Inisialisasi AlatViewModel dengan scope Activity agar dapat di-share antar layar sub-fitur Alat
    // Hal ini memastikan data alat yang baru ditambahkan langsung muncul di layar daftar tanpa harus muat ulang
    val context = LocalContext.current.applicationContext as android.app.Application
    val database = AppDatabase.getDatabase(context)
    val alatDao = database.alatDao()
    val alatApi = RetrofitClient.instance.create(AlatApiService::class.java)
    val alatRepository = AlatRepository(alatApi, alatDao, context)
    val alatViewModelFactory = AlatViewModelFactory(alatRepository, context)
    val alatViewModel: AlatViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner,
        factory = alatViewModelFactory
    )

    // NavHost adalah kontainer utama yang menampung semua layar
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route, // Layar pertama kali dibuka adalah SplashScreen
        modifier = Modifier.padding(paddingValues)
    ) {

        // --- BAGIAN ADAM (FITUR AUTENTIKASI) ---
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true } // Hapus splash dari history agar tak bisa di-back
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack() // Kembali ke layar sebelumnya (Login)
                }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) // Bersihkan semua backstack (Riwayat halaman) setelah Logout
                    }
                },
                onNavigateToInventaris = {
                    navController.navigate(Screen.Inventaris.route)
                }
            )
        }

        // --- BAGIAN REJA (FITUR KEGIATAN & HOME) ---
        composable(route = Screen.Home.route) {
            val repository = remember { FakeKegiatanRepository(context) }
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.HomeViewModelFactory(repository)
            )

            HomeScreen(
                navController = navController,
                viewModel = homeViewModel,
                onNavigateToActivities = {
                    navController.navigate(Screen.Kegiatan.route)
                },
                onNavigateToInventory = {
                    navController.navigate(Screen.Inventaris.route)
                },
                onDetailClick = { id ->
                    navController.navigate(Screen.DetailKegiatan.createRoute(id))
                }
            )
        }

        composable(route = Screen.Kegiatan.route) {
            AktivitasScreen(
                onTambahAktivitas = {
                    navController.navigate(Screen.TambahKegiatan.route)
                },
                onDetailAktivitas = { id ->
                    navController.navigate(Screen.DetailKegiatan.createRoute(id))
                }
            )
        }

        composable(route = Screen.TambahKegiatan.route) {
            TambahKegiatanScreen(
                navController = navController,
                viewModel = alatViewModel
            )
        }

        // Rute ini menerima argumen dinamis berupa 'id' kegiatan
        composable(
            route = Screen.DetailKegiatan.route,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            DetailKegiatanScreen(
                kegiatanId = id,
                onBack = { navController.popBackStack() },
                onEditClick = { kegiatanId ->
                    navController.navigate(Screen.EditKegiatan.createRoute(kegiatanId))
                },
                onDeleteSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditKegiatan.route,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            EditKegiatanScreen(
                kegiatanId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.TambahAlatLuar.route) {
            TambahAlatLuarScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        // --- BAGIAN NAJIB (FITUR ALAT / INVENTARIS) ---
        composable(route = Screen.Inventaris.route) {
            InventarisScreen(
                viewModel = alatViewModel,
                onAddClick = {
                    navController.navigate(Screen.TambahAlat.route)
                },
                onItemClick = { id ->
                    navController.navigate(Screen.DetailAlat.createRoute(id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.TambahAlat.route) {
            TambahAlatScreen(
                viewModel = alatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DetailAlat.route,
            arguments = listOf(
                navArgument("alatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val alatId = backStackEntry.arguments?.getString("alatId") ?: ""
            DetailAlatScreen(
                alatId = alatId,
                viewModel = alatViewModel,
                onBack = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.EditAlat.createRoute(id))
                },
                onDeleteClick = { id ->
                    alatViewModel.deleteAlat(
                        id = id,
                        onSuccess = {
                            Toast.makeText(context, "Alat berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        composable(
            route = Screen.EditAlat.route,
            arguments = listOf(
                navArgument("alatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val alatId = backStackEntry.arguments?.getString("alatId") ?: ""
            EditAlatScreen(
                alatId = alatId,
                viewModel = alatViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
