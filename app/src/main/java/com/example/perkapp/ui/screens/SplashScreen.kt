package com.example.perkapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.perkapp.database.UserPreferences
import com.example.perkapp.database.dataStore
import kotlinx.coroutines.delay

/**
 * FUNGSI: SplashScreen
 * TUJUAN: Menjadi layar pertama yang dilihat pengguna saat aplikasi baru dibuka.
 * 
 * ALUR LOGIKA PENGERJAAN:
 * 1. Layar ini bukan sekadar pajangan, tetapi ia memiliki tugas berat di balik layar:
 *    Membaca DataStore (memori HP) untuk mencari `auth_token`.
 * 2. Selama proses pencarian (`token == "LOADING"`), ia akan menahan pengguna di layar ini.
 * 3. Jika token ditemukan (dan valid), ia langsung mengalihkan rute ke `Home`.
 * 4. Jika token kosong/tidak ada, ia mengarahkan rute ke halaman `Login`.
 * (Ada sedikit jeda `delay` agar logo aplikasi sempat terlihat secara visual).
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context.dataStore) }
    // Gunakan initial state "LOADING" sebagai indikator kita masih mengecek datastore (sedang membaca memori HP)
    val token by userPreferences.getAuthToken.collectAsState(initial = "LOADING")

    // LaunchedEffect akan berjalan satu kali saat layar ini dimunculkan
    LaunchedEffect(token) {
        // Jika token masih "LOADING", artinya kita belum selesai membaca DataStore. Kita tunggu.
        if (token != "LOADING") {
            // Beri sedikit jeda (1.5 detik) agar animasi Splash atau logonya sempat terlihat oleh user
            delay(1500)
            
            if (token.isNullOrEmpty()) {
                // Token kosong -> user belum login, arahkan ke halaman Login
                onNavigateToLogin()
            } else {
                // Token ada -> user sudah pernah login dan sesinya belum habis, langsung masuk Home
                onNavigateToHome()
            }
        }
    }

    // Tampilan UI SplashScreen (Hanya menaruh teks 'perkapp' di tengah layar)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "perkapp",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
