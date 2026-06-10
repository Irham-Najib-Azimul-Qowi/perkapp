package com.example.perkapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.perkapp.model.HomeUiState
import com.example.perkapp.model.Kegiatan
import com.example.perkapp.model.StatusKegiatan
import com.example.perkapp.viewmodel.HomeViewModel

/**
 * FUNGSI: HomeScreen
 * TUJUAN: Halaman beranda utama (Dashboard) dari aplikasi.
 * Menampilkan ringkasan kegiatan yang sedang aktif/berlangsung.
 * Berfungsi sebagai titik awal pengguna setelah berhasil login.
 *
 * @param navController Controller untuk navigasi antar halaman
 * @param viewModel ViewModel penyedia data untuk beranda
 * @param onNavigateToActivities Aksi saat ingin melihat semua aktivitas
 * @param onNavigateToInventory Aksi saat ingin melihat inventaris alat
 * @param onNavigateToHistory Aksi saat ingin melihat riwayat
 * @param onNavigateToProfile Aksi saat ingin membuka profil
 * @param onNavigateToBorrow Aksi meminjam
 * @param onNavigateToLogActivity Aksi log aktivitas
 * @param onDetailClick Aksi saat salah satu kegiatan diklik (membuka detail)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen( 
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    onNavigateToActivities: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBorrow: () -> Unit = {},
    onNavigateToLogActivity: () -> Unit = {},
    onDetailClick: (String) -> Unit = {}
) {
    // Mengamati state dari ViewModel (loading, daftar kegiatan, error)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "perkapp",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.errorMessage!!, color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.muatDataHome() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Kegiatan Berlangsung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val kegiatanBerlangsung = uiState.kegiatanAktif.filter { 
                                it.statusType == StatusKegiatan.AKTIF 
                            }

                            if (kegiatanBerlangsung.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tidak ada aktivitas berlangsung saat ini.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                kegiatanBerlangsung.forEach { kegiatan ->
                                    KartuKegiatan(
                                        kegiatan = kegiatan,
                                        onClick = { onDetailClick(kegiatan.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * FUNGSI: KartuKegiatan
 * TUJUAN: Komponen UI (Card) untuk menampilkan ringkasan satu kegiatan.
 *
 * @param kegiatan Objek data kegiatan yang akan ditampilkan
 * @param onClick Aksi yang dipanggil saat kartu ini ditekan/diklik
 */
@Composable
fun KartuKegiatan(
    kegiatan: Kegiatan,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = kegiatan.judul,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = if (kegiatan.statusType == StatusKegiatan.AKTIF) MaterialTheme.colorScheme.primaryContainer else Color(0xFFD9E3F6),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = kegiatan.kategori,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (kegiatan.statusType == StatusKegiatan.AKTIF) MaterialTheme.colorScheme.primary else Color(0xFF3D4A3D)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = kegiatan.lokasi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val isAktif = kegiatan.statusType == StatusKegiatan.AKTIF
                val bannerColor = if (kegiatan.isPending) Color(0xFFFFF3CD) else if (isAktif) Color(0xFFE8F5E9) else Color(0xFFFFF3CD)
                val bannerTextColor = if (kegiatan.isPending) Color(0xFF856404) else if (isAktif) Color(0xFF2E7D32) else Color(0xFF856404)
                val bannerText = if (kegiatan.isPending) "Pending (Menunggu Sinkronisasi)" else if (isAktif) "Kegiatan Berlangsung" else "Maintenance / Audit Kegiatan"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = bannerColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = bannerTextColor
                    )
                }
            }
        }
    }
}
