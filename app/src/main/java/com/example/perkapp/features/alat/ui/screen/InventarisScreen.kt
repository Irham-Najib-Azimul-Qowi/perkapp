package com.example.perkapp.features.alat.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.core.utils.NetworkUtils
import com.example.perkapp.features.alat.ui.component.AlatCard
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * FUNGSI: InventarisScreen
 * TUJUAN: Menjadi Halaman Utama (Beranda) bagi Admin untuk melihat isi gudang.
 *
 * ALUR LOGIKA PENGERJAAN:
 * 1. Saat pertama kali dibuka (`LaunchedEffect`), layar menyuruh `AlatViewModel`
 *    untuk mengambil daftar alat (`getAllAlat`).
 * 2. Memantau `NetworkUtils` secara realtime. Bila internet putus, 
 *    muncul *Banner Kuning* peringatan *Offline* menggunakan animasi turun (`slideInVertically`).
 * 3. Jika internet hidup kembali secara tiba-tiba, ia mencoba *Silent Login* 
 *    (memulihkan token) dan memuat ulang barang.
 * 4. Menyediakan dua kotak *Dashboard* di atas (Total Alat & Status Penyimpanan).
 * 5. Menggunakan `LazyColumn` untuk merender daftar `AlatCard` satu per satu
 *    dengan irit memori (hanya memuat elemen yang terlihat di layar).
 * 6. Menyediakan Tombol Tambah `+` (FAB) di pojok untuk menambah barang baru.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarisScreen(
    viewModel: AlatViewModel,
    onAddClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    // Membaca daftar alat dan status loading dari ViewModel secara reaktif
    val alatList by viewModel.alatList.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val context = LocalContext.current
    // Status offline/online awal
    var isOnline by remember { mutableStateOf(NetworkUtils.isOnline(context)) }

    // Memantau perubahan status jaringan (internet) secara real-time
    LaunchedEffect(Unit) {
        NetworkUtils.observeNetworkStatus(context).collectLatest { online ->
            isOnline = online
            // Jika tiba-tiba online, kita coba segarkan koneksi (silent login) dan ambil data terbaru
            if (online) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.perkapp.core.network.RetrofitClient.performSilentLogin(context)
                }
                viewModel.getAllAlat()
            }
        }
    }

    // Mengambil data saat layar ini pertama kali dimunculkan
    LaunchedEffect(Unit) {
        viewModel.getAllAlat()
    }

    Scaffold(
        topBar =  {
            TopAppBar(
                title = {
                    Text(
                        text = "Inventaris Alat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Profil",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        // Tombol (+) melayang di pojok kanan bawah
        floatingActionButton = {
            FloatingActionButton(
                onClick =  onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription =  "Tambah Alat",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Peringatan Kuning Mode Offline ---
            // Muncul secara meluncur/animasi jika internet mati
            AnimatedVisibility(
                visible = !isOnline,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3CD))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF856404)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode Offline — Data akan disinkronkan saat online",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF856404)
                    )
                }
            }

            // --- Menangani Tampilan Saat Sedang Loading, Kosong, Atau Ada Data ---
            if (isLoading) {
                // Menampilkan efek putar loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth =  3.dp
                    )
                }
            } else if (alatList.isEmpty()){
                // Menampilkan pesan kosong jika belum ada data alat sama sekali
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text =  "Belum ada alat",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tekan tombol + untuk menambah alat baru",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // --- Menampilkan Dashboard Ringkasan & Daftar Alat ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Kotak Kiri: Total Alat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Total Alat",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${alatList.size} Alat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Kotak Kanan: Status Pending (Belum dikirim ke server)
                    val pendingCount = alatList.count { it.sync_status == "pending" }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (pendingCount > 0) Color(0xFFFFF3CD) else Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Penyimpanan",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (pendingCount > 0) Color(0xFF856404) else Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (pendingCount > 0) "$pendingCount Pending di Room" else "Semua Tersinkron",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingCount > 0) Color(0xFF856404) else Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // LazyColumn adalah daftar yang bisa di-scroll secara efisien (hanya merender item yang terlihat)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(alatList) { alat ->
                        AlatCard(
                            alat = alat,
                            onClick = { onItemClick(alat.id)}
                        )
                    }
                }
            }
        }
    }
}