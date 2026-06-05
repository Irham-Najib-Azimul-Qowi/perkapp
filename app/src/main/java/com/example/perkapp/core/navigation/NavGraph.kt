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
            HomeScreen()
        }
        composable(route = Screen.Inventaris.route) {
            InventarisScreen()
        }
        composable(route = Screen.Kegiatan.route) {
            // Jika Kegiatan ada halamannya sendiri, taruh di sini
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