package com.example.perkapp.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.perkapp.core.Injection
import com.example.perkapp.core.utils.NetworkUtils

/**
 * FUNGSI: ProfileScreen
 * TUJUAN: Menjadi dasbor mini bagi pengguna untuk melihat identitas dirinya, 
 * peran (Role), status koneksi saat ini, serta pintu keluar (Logout).
 * 
 * ALUR LOGIKA PENGERJAAN:
 * 1. Mengambil State `currentUser` (Profil dari Room DB), `isOnlineState` (dari NetworkUtils),
 *    serta `token` (dari DataStore) secara reaktif.
 * 2. Menyusun tampilan profil dengan membedakan warna label (Primary/Secondary) 
 *    bergantung pada Role (Admin vs Member).
 * 3. Jika pengguna adalah Admin, layar akan menyuntikkan tombol ekstra 
 *    "Inventaris Alat" yang tidak bisa dilihat oleh Member biasa.
 * 4. Menyediakan tombol "Logout" yang jika ditekan akan membersihkan sesi.
 *    Bila `token` terdeteksi kosong, layar memicu *Navigation Event* (`onLogoutSuccess`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutSuccess: () -> Unit,
    onNavigateToInventaris: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(Injection.provideAuthRepository(LocalContext.current))
    )
) {
    // Membaca data pengguna yang saat ini sedang login dari Room Database
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    
    val context = LocalContext.current
    // Mengamati apakah HP sedang konek ke internet atau tidak secara realtime
    val isOnlineState by remember(context) {
        NetworkUtils.observeNetworkStatus(context)
    }.collectAsState(initial = NetworkUtils.isOnline(context))

    // Mengambil token untuk memastikan user masih login
    val token by viewModel.authToken.collectAsState(initial = "LOADING")
    
    // Jika token mendadak kosong (misal akibat di-logout dari sistem), lempar user kembali ke halaman Login
    LaunchedEffect(token) {
        if (token != "LOADING" && token.isNullOrEmpty()) {
            onLogoutSuccess()
        }
    }

    // Scaffold menyediakan kerangka dasar layar (TopBar + Konten Utama)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profil Saya",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Jika data user sudah berhasil dimuat dari database
            if (currentUser != null) {
                // --- Nama dan Email ---
                Text(
                    text = currentUser!!.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = currentUser!!.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // --- Baris Label Role & Status Online ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Label Peran (Admin atau User Biasa)
                    Box(
                        modifier = Modifier
                            .background(
                                // Warnanya dibedakan: Admin (warna primary), User biasa (warna secondary)
                                color = if (currentUser!!.role == "admin") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentUser!!.role.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentUser!!.role == "admin") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Label Status Koneksi Internet
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = if (isOnlineState) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), // Hijau jika online, Merah jika offline
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        // Titik warna indikator online/offline
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isOnlineState) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnlineState) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnlineState) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // --- Menu Khusus Admin ---
                // Hanya muncul jika tipe user adalah admin
                if (currentUser!!.role == "admin") {
                    Button(
                        onClick = onNavigateToInventaris,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            "Inventaris Alat",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // Menampilkan efek putar jika data profil belum selesai dimuat
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(48.dp))
            }

            // --- Tombol Logout ---
            OutlinedButton(
                onClick = {
                    // Minta ViewModel untuk menghapus data token & sesi
                    viewModel.logout()
                    onLogoutSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(
                    "Logout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
