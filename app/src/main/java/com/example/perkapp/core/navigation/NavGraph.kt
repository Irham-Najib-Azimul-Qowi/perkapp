package com.example.perkapp.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.perkapp.features.auth.ui.LoginScreen
import com.example.perkapp.features.auth.ui.SplashScreen
import com.example.perkapp.features.auth.ui.RegisterScreen
import com.example.perkapp.features.kegiatan.ui.HomeScreen
import com.example.perkapp.features.alat.ui.InventarisScreen

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.padding(paddingValues)
    ) {

        // --- BAGIAN ADAM (AUTH) ---
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
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
                    // Sukses daftar, bypass dan langsung masuk Home atau Login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // --- BAGIAN REJA & NAJIB ---
        composable(route = Screen.Home.route) {
            val repository = com.example.perkapp.features.kegiatan.data.FakeKegiatanRepository()
            val homeViewModel: com.example.perkapp.features.kegiatan.ui.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.perkapp.features.kegiatan.ui.HomeViewModel.HomeViewModelFactory(repository)
            )

            com.example.perkapp.features.kegiatan.ui.HomeScreen(
                navController = navController,
                viewModel = homeViewModel,
                onNavigateToActivities = {
                    navController.navigate(Screen.Kegiatan.route)
                },
                onNavigateToInventory = {
                    navController.navigate(Screen.Inventaris.route)
                }
            )
        }
        composable(route = Screen.Inventaris.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val application = context.applicationContext as android.app.Application
            val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
            val alatApi = com.example.perkapp.core.network.RetrofitClient.instance.create(com.example.perkapp.features.alat.api.AlatApiService::class.java)
            val alatRepository = com.example.perkapp.features.alat.data.repository.AlatRepository(alatApi, db.alatDao(), context)
            val alatViewModel: com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.perkapp.features.alat.ui.viewmodel.AlatViewModelFactory(alatRepository, application)
            )

            com.example.perkapp.features.alat.ui.screen.InventarisScreen(
                viewModel = alatViewModel,
                onAddClick = {
                    // TODO: Navigate to Add Inventaris Screen
                },
                onItemClick = { id ->
                    // TODO: Navigate to Detail Inventaris Screen
                }
            )
        }
        composable(route = Screen.Kegiatan.route) {
            com.example.perkapp.core.features.kegiatan.ui.AktivitasScreen(
                onTambahAktivitas = {
                    navController.navigate(Screen.TambahKegiatan.route)
                },
                onDetailAktivitas = { id ->
                    navController.navigate(Screen.DetailKegiatan.createRoute(id))
                }
            )
        }
        composable(route = Screen.TambahKegiatan.route) {
            com.example.perkapp.core.features.kegiatan.ui.TambahKegiatanScreen(navController = navController)
        }
        composable(route = Screen.Profile.route) {
            com.example.perkapp.features.auth.ui.ProfileScreen(
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) // Bersihkan semua backstack
                    }
                },
                onNavigateToInventaris = {
                    navController.navigate(Screen.Inventaris.route)
                }
            )
        }
    }
}