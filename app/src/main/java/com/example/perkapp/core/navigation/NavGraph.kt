package com.example.perkapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route // Sementara kita set mulai dari Login
    ) {

        // --- BAGIAN ADAM (AUTH) ---
        composable(route = Screen.Splash.route) {
            // Nanti diisi SplashScreen()
        }
        composable(route = Screen.Login.route) {
            // Nanti diisi LoginScreen()
        }
        composable(route = Screen.Register.route) {
            // Nanti diisi RegisterScreen()
        }

        // --- BAGIAN REJA & NAJIB ---
        composable(route = Screen.Home.route) {
            // Nanti diisi HomeScreen() punya Reja
        }
        composable(route = Screen.Inventaris.route) {
            // Nanti diisi InventarisScreen() punya Najib
        }
    }
}