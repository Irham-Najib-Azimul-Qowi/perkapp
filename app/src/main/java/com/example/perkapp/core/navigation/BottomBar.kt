package com.example.perkapp.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * BottomNavItem — Kerangka data untuk tombol-tombol di menu bawah.
 */
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Kegiatan : BottomNavItem(Screen.Kegiatan.route, "Kegiatan", Icons.Default.List)
    object Profile : BottomNavItem(Screen.Profile.route, "Profil", Icons.Default.Person)
}

/**
 * BottomBar — Komponen menu navigasi di bagian bawah aplikasi.
 *
 * Menu ini cerdas: ia hanya akan muncul jika pengguna sedang berada di 
 * halaman Home, Kegiatan, atau Profil. Jika masuk ke halaman Detail, 
 * menu ini akan otomatis sembunyi.
 */
@Composable
fun BottomBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Kegiatan,
        BottomNavItem.Profile
    )

    // Pantau layar apa yang sedang aktif sekarang
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tampilkan BottomBar hanya di halaman utama yang ada di daftar 'items'
    val showBottomBar = items.any { it.route == currentRoute }

    if (showBottomBar) {
        NavigationBar {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    // Tandai tombol sebagai 'Aktif' jika rutenya cocok
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            // popUpTo Mencegah penumpukan halaman saat pengguna 
                            // bolak-balik mencet tombol menu yang sama
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
