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
    onBackClick: () -> Unit = {} // Aksi callback saat menekan tombol kembali
) { // Mulai dari fungsi Composable InventarisScreen
    // Membaca daftar alat dari LiveData di ViewModel secara reaktif dengan nilai awal kosong
    val alatList by viewModel.alatList.observeAsState(emptyList())
    // Membaca status loading data dari LiveData di ViewModel secara reaktif dengan nilai awal false
    val isLoading by viewModel.isLoading.observeAsState(false)
    // Mengambil Context aplikasi Compose saat ini
    val context = LocalContext.current
    // Menginisialisasi state status jaringan awal apakah tersambung internet
    var isOnline by remember { mutableStateOf(NetworkUtils.isOnline(context)) }

    // Memantau aliran status koneksi internet secara asinkron/real-time
    LaunchedEffect(Unit) {
        // Mengumpulkan status internet terbaru menggunakan observeNetworkStatus
        NetworkUtils.observeNetworkStatus(context).collectLatest { online ->
            isOnline = online // Perbarui nilai state jaringan lokal
            // Jika statusnya berubah menjadi online (tersambung internet)
            if (online) {
                // Pindahkan eksekusi coroutine ke thread pekerja (IO)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Jalankan fungsi login latar belakang untuk memperbarui token kedaluwarsa
                    com.example.perkapp.core.network.RetrofitClient.performSilentLogin(context)
                }
                viewModel.getAllAlat() // Muat ulang data daftar alat terbaru dari server
            }
        }
    }

    // Mengambil daftar seluruh alat dari lokal saat pertama kali layar dirender
    LaunchedEffect(Unit) {
        viewModel.getAllAlat() // Panggil fungsi getAllAlat di ViewModel
    }

    // Scaffold menyusun kerangka visual utama halaman (TopBar, BottomBar, FAB, dan Konten)
    Scaffold(
        topBar =  { // Pengaturan bilah atas halaman (Top Bar)
            TopAppBar( // Komponen TopAppBar
                title = { // Judul di bilah atas
                    Text( // Teks judul
                        text = "Inventaris Alat", // Judul bertuliskan Inventaris Alat
                        style = MaterialTheme.typography.titleLarge, // Gaya ukuran font judul besar
                        fontWeight = FontWeight.Bold // Menebalkan teks judul
                    )
                },
                navigationIcon = { // Ikon navigasi sebelah kiri judul
                    IconButton(onClick = onBackClick) { // Tombol klik kembali
                        Icon( // Komponen ikon
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Menggunakan ikon panah kiri
                            contentDescription = "Kembali ke Profil", // Deskripsi aksesibilitas tombol
                            tint = Color.White // Mewarnai ikon dengan putih
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( // Pengaturan skema warna TopAppBar
                    containerColor = MaterialTheme.colorScheme.primary, // Warna kontainer utama primer aplikasi
                    titleContentColor = Color.White // Warna konten/teks putih
                )
            )
        },
        // Tombol (+) melayang di pojok kanan bawah
        floatingActionButton = { // Menentukan tombol melayang (Floating Action Button)
            FloatingActionButton( // Komponen FAB
                onClick =  onAddClick, // Aksi callback diarahkan ke onAddClick saat diklik
                containerColor = MaterialTheme.colorScheme.primary, // Warna latar belakang tombol primer
                contentColor = Color.White, // Warna ikon putih
                shape = CircleShape, // Potong bentuk bulat melingkar sempurna
                modifier = Modifier // Pengubah ukuran dan efek tombol
                    .size(64.dp) // Ukuran tombol 64dp
                    .shadow(8.dp, CircleShape) // Bayangan tombol 8dp dengan bentuk bulat
            ) {
                Icon( // Komponen ikon di dalam tombol
                    Icons.Default.Add, // Ikon tambah "+" bawaan
                    contentDescription =  "Tambah Alat", // Deskripsi aksesibilitas tambah alat
                    modifier = Modifier.size(28.dp) // Ukuran ikon 28dp
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background // Menentukan latar belakang layar utama
    ) { innerPadding -> // Inner padding aman dari Scaffold
        Column( // Menyusun elemen secara vertikal ke bawah
            modifier = Modifier // Pengatur tata letak kolom
                .fillMaxSize() // Lebar dan tinggi memenuhi layar penuh
                .padding(innerPadding) // Terapkan padding aman dari Scaffold
        ) {
            // --- Peringatan Kuning Mode Offline ---
            // Muncul secara meluncur/animasi jika internet mati
            AnimatedVisibility( // Animasi visibilitas elemen
                visible = !isOnline, // Tampil hanya saat status isOnline bernilai false
                enter = slideInVertically() + fadeIn(), // Animasi muncul meluncur vertikal dan memudar masuk
                exit = slideOutVertically() + fadeOut() // Animasi hilang meluncur vertikal dan memudar keluar
            ) {
                Row( // Menyusun ikon dan teks secara horizontal ke samping
                    modifier = Modifier // Pengatur tata letak baris
                        .fillMaxWidth() // Lebar baris memenuhi layar horizontal
                        .background(Color(0xFFFFF3CD)) // Latar belakang warna kuning muda peringatan
                        .padding(horizontal = 16.dp, vertical = 10.dp), // Jarak padding dalam kotak kuning
                    verticalAlignment = Alignment.CenterVertically // Pusatkan elemen secara vertikal di tengah baris
                ) {
                    Icon( // Ikon awan coret (Cloud Off)
                        imageVector = Icons.Default.CloudOff, // Menggunakan ikon awan coret
                        contentDescription = null, // Ikon dekoratif saja tanpa deskripsi suara
                        modifier = Modifier.size(18.dp), // Ukuran ikon 18dp
                        tint = Color(0xFF856404) // Warna ikon cokelat tua peringatan
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Jarak spasi horizontal kecil antara ikon dan teks
                    Text( // Teks informasi mode offline
                        text = "Mode Offline — Data akan disinkronkan saat online", // Isi teks peringatan
                        style = MaterialTheme.typography.bodySmall, // Gaya ukuran font kecil
                        color = Color(0xFF856404) // Warna teks cokelat tua peringatan
                    )
                }
            }

            // --- Menangani Tampilan Saat Sedang Loading, Kosong, Atau Ada Data ---
            if (isLoading) { // Jika status loading bernilai true
                // Menampilkan efek putar loading
                Box( // Box untuk memposisikan loading tepat di tengah layar
                    modifier = Modifier.fillMaxSize(), // Memenuhi layar
                    contentAlignment = Alignment.Center // Posisi objek di tengah
                ) {
                    CircularProgressIndicator( // Indikator lingkaran loading berputar
                        color = MaterialTheme.colorScheme.primary, // Menggunakan warna primer aplikasi
                        strokeWidth =  3.dp // Ketebalan garis lingkaran 3dp
                    )
                }
            } else if (alatList.isEmpty()){ // Jika status loading false dan daftar alat kosong
                // Menampilkan pesan kosong jika belum ada data alat sama sekali
                Box( // Wadah pemusat elemen teks kosong
                    modifier = Modifier.fillMaxSize(), // Memenuhi layar penuh
                    contentAlignment = Alignment.Center // Pusatkan konten di tengah layar
                ) {
                    Column( // Menyusun teks informasi kosong secara vertikal
                        horizontalAlignment = Alignment.CenterHorizontally, // Tengahkan objek horizontal
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Jarak vertikal antar teks 8dp
                    ) {
                        Text( // Teks judul kosong
                            text =  "Belum ada alat", // Pesan belum ada alat
                            style = MaterialTheme.typography.titleMedium, // Gaya ukuran font judul sedang
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Warna abu-abu gelap
                        )
                        Text( // Teks arahan tambah alat
                            text = "Tekan tombol + untuk menambah alat baru", // Cara menambah alat
                            style = MaterialTheme.typography.bodyMedium, // Gaya ukuran teks deskripsi biasa
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Warna teks abu-abu gelap
                        )
                    }
                }
            } else { // Jika data alat ada dan loading telah selesai
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