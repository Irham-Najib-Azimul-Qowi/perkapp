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
 * FUNGSI: BottomBar
 * TUJUAN: Merender (menampilkan) menu navigasi utama di bagian bawah layar HP.
 * 
 * ALUR LOGIKA PENGERJAAN:
 * 1. Mendefinisikan 3 tombol utama (`items`): Home, Kegiatan, Profil.
 * 2. Memantau (`observe`) secara *real-time* pengguna sedang ada di layar mana
 *    menggunakan `currentBackStackEntryAsState()`.
 * 3. Memutuskan apakah menu ini harus Tampil atau Sembunyi (`showBottomBar`).
 *    Menu ini cerdas: ia otomatis menghilang saat pengguna masuk ke halaman 
 *    sekunder (seperti form Tambah Data atau halaman Detail) agar area layar lebih luas.
 * 4. Jika harus tampil, ia merender komponen `NavigationBar` bawaan Jetpack Compose
 *    dan mengisi tombol-tombolnya.
 * 5. Saat sebuah tombol diklik, navigasi diatur menggunakan metode `popUpTo` agar
 *    tidak terjadi penumpukan *history* layar (yang bikin HP lemot atau harus nge-back 100x).
 * 
 * @param navController Alat pengendali navigasi untuk memberikan perintah pindah layar.
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
