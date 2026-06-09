package com.example.perkapp.core.navigation // Paket navigasi utama core

import androidx.compose.material.icons.Icons // Mengimpor ikon bawaan compose
import androidx.compose.material.icons.filled.Home // Mengimpor ikon Home
import androidx.compose.material.icons.filled.Settings // Mengimpor ikon Settings
import androidx.compose.material.icons.filled.List // Mengimpor ikon List
import androidx.compose.material.icons.filled.Person // Mengimpor ikon Person
import androidx.compose.material3.Icon // Mengimpor komponen Icon
import androidx.compose.material3.NavigationBar // Mengimpor komponen NavigationBar
import androidx.compose.material3.NavigationBarItem // Mengimpor komponen NavigationBarItem
import androidx.compose.material3.Text // Mengimpor komponen Text
import androidx.compose.runtime.Composable // Mengimpor composable annotation
import androidx.compose.runtime.getValue // Mengimpor delegasi property getValue
import androidx.compose.ui.graphics.vector.ImageVector // Mengimpor kelas ImageVector
import androidx.navigation.NavHostController // Mengimpor kelas NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState // Mengimpor method state backstack

/**
 * BottomNavItem — Kerangka data untuk tombol-tombol di menu bawah.
 */
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home) // Tombol menu Home
    object Kegiatan : BottomNavItem(Screen.Kegiatan.route, "Kegiatan", Icons.Default.List) // Tombol menu Kegiatan
    object Profile : BottomNavItem(Screen.Profile.route, "Profil", Icons.Default.Person) // Tombol menu Profil
}

/**
 * FUNGSI: BottomBar
 * TUJUAN: Merender (menampilkan) menu navigasi utama di bagian bawah layar HP.
 */
@Composable
fun BottomBar(navController: NavHostController) { // Deklarasi composable BottomBar dengan parameter navController
    val items = listOf( // Mendefinisikan list menu BottomBar
        BottomNavItem.Home, // Item Home
        BottomNavItem.Kegiatan, // Item Kegiatan
        BottomNavItem.Profile // Item Profil
    )

    // Pantau layar apa yang sedang aktif sekarang secara reaktif
    val navBackStackEntry by navController.currentBackStackEntryAsState() // State backstack entri
    val currentRoute = navBackStackEntry?.destination?.route // String rute saat ini

    // Tampilkan BottomBar hanya di halaman utama yang ada di daftar 'items'
    val showBottomBar = items.any { it.route == currentRoute } // Boolean kecocokan rute aktif dengan item menu

    if (showBottomBar) { // Jika showBottomBar bernilai true
        NavigationBar { // Tampilkan komponen kontainer NavigationBar
            items.forEach { item -> // Lakukan perulangan untuk setiap item menu
                NavigationBarItem( // Komponen tombol menu individual
                    icon = { Icon(item.icon, contentDescription = item.title) }, // Render ikon menu
                    label = { Text(item.title) }, // Render label teks menu
                    // Tandai tombol sebagai 'Aktif' jika rutenya cocok
                    selected = currentRoute == item.route, // Cek status selected
                    onClick = { // Aksi saat menu diklik
                        navController.navigate(item.route) { // Berpindah ke rute yang diklik
                            // popUpTo Mencegah penumpukan halaman saat pengguna 
                            // bolak-balik mencet tombol menu yang sama
                            popUpTo(navController.graph.startDestinationId) { // PopUp ke halaman pertama
                                saveState = true // Simpan status halaman yang ditinggalkan
                            }
                            launchSingleTop = true // Hindari duplikasi halaman jika berada di atas stack
                            restoreState = true // Kembalikan status halaman saat kembali dibuka
                        }
                    }
                )
            }
        }
    }
}
