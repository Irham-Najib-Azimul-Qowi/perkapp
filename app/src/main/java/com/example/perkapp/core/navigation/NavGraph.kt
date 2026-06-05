package com.example.perkapp.core.navigation

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
import com.example.perkapp.core.features.kegiatan.ui.AktivitasScreen
import com.example.perkapp.core.features.kegiatan.ui.TambahKegiatanScreen
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

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues = PaddingValues()
) {
    // Inisialisasi AlatViewModel dengan scope Activity agar dapat di-share antar layar sub-fitur Alat
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
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

        // --- BAGIAN REJA (KEGIATAN) ---
        composable(route = Screen.Home.route) {
            val repository = remember { FakeKegiatanRepository() }
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
            TambahKegiatanScreen(navController = navController)
        }

        // --- BAGIAN NAJIB (ALAT / INVENTARIS) ---
        composable(route = Screen.Inventaris.route) {
            InventarisScreen(
                viewModel = alatViewModel,
                onAddClick = {
                    navController.navigate(Screen.TambahAlat.route)
                },
                onItemClick = { id ->
                    navController.navigate(Screen.DetailAlat.createRoute(id))
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
                    alatViewModel.deleteAlat(id)
                    navController.popBackStack()
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
