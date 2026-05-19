package com.example.perkapp.features.kegiatan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.perkapp.features.kegiatan.domain.HomeUiState
import com.example.perkapp.features.kegiatan.domain.InventoryStats
import com.example.perkapp.features.kegiatan.domain.Kegiatan
import com.example.perkapp.features.kegiatan.domain.StatusKegiatan

// ============================================================
// FILE: HomeScreen.kt
// LOKASI: features/kegiatan/ui/HomeScreen.kt
// FUNGSI: Lapisan UI (View) dalam MVVM.
//         Menampilkan halaman Beranda (Home) aplikasi SIEPERKAP.
//
// Alur data:
// KegiatanRepository → HomeViewModel (uiState) → HomeScreen → Composable
//
// DEPENDENCY yang perlu ditambah di build.gradle (app):
//   implementation("io.coil-kt:coil-compose:2.6.0")
//   implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
//   implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
// ============================================================


// ---------------------------------------------------------------
// ENTRY POINT: HomeScreen
// Composable utama yang dipanggil dari NavGraph milik Adam.
// Contoh di NavGraph Adam:
//   composable(Routes.HOME) {
//       HomeScreen(
//           onNavigateToBorrow = { navController.navigate(Routes.BORROW) },
//           ...
//       )
//   }
// ---------------------------------------------------------------
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),   // Nanti ganti pakai Factory saat repo sudah siap
    onNavigateToActivities: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBorrow: () -> Unit = {},
    onNavigateToLogActivity: () -> Unit = {}
) {
    // Observe state dari ViewModel
    // collectAsStateWithLifecycle = berhenti observe saat layar tidak aktif (hemat baterai)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Scaffold = layout dasar Material 3
    // Menyediakan slot TopBar, BottomBar, dan konten utama
    Scaffold(
        topBar = {
            HomeTopBar(
                fotoUrl = uiState.userInfo.fotoUrl,
                onSyncDitekan = { viewModel.onSyncDitekan() }
            )
        },
        bottomBar = {
            HomeBottomNav(
                onNavigateToActivities = onNavigateToActivities,
                onNavigateToInventory  = onNavigateToInventory,
                onNavigateToHistory    = onNavigateToHistory,
                onNavigateToProfile    = onNavigateToProfile
            )
        }
    ) { innerPadding ->
        // innerPadding = jarak otomatis dari Scaffold supaya konten
        // tidak tertutup TopBar dan BottomBar

        when {
            // Kondisi 1: Sedang loading
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HijauUtama)
                }
            }

            // Kondisi 2: Ada error
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage!!, color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.muatDataHome() }) {
                            Text("Coba Lagi")
                        }
                    }
                }
            }

            // Kondisi 3: Data berhasil dimuat, tampilkan konten
            else -> {
                KontenHome(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onBorrowDitekan = {
                        viewModel.onPinjamAlatDitekan()
                        onNavigateToBorrow()
                    },
                    onLogActivityDitekan = {
                        viewModel.onCatatKegiatanDitekan()
                        onNavigateToLogActivity()
                    },
                    onSeeAllDitekan = onNavigateToActivities
                )
            }
        }
    }
}


// ---------------------------------------------------------------
// COMPOSABLE: KontenHome
// Konten utama halaman Home (bisa di-scroll vertikal)
// Dipisah dari HomeScreen supaya kode lebih rapi dan mudah dibaca
// ---------------------------------------------------------------
@Composable
fun KontenHome(
    uiState: HomeUiState,
    innerPadding: PaddingValues,
    onBorrowDitekan: () -> Unit,
    onLogActivityDitekan: () -> Unit,
    onSeeAllDitekan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())  // konten bisa di-scroll
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section 1: Sapaan & Status Sync
        SectionSapaan(
            sapaan    = uiState.userInfo.sapaan,
            nama      = uiState.userInfo.nama,
            isSynced  = uiState.isSynced
        )

        // Section 2: Tombol Aksi Cepat
        SectionQuickActions(
            onBorrowDitekan      = onBorrowDitekan,
            onLogActivityDitekan = onLogActivityDitekan
        )

        // Section 3: Statistik Inventori
        SectionInventori(stats = uiState.inventoryStats)

        // Section 4: Daftar Kegiatan Aktif
        SectionKegiatanAktif(
            kegiatanList   = uiState.kegiatanAktif,
            onSeeAllDitekan = onSeeAllDitekan
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}


// ---------------------------------------------------------------
// COMPOSABLE: HomeTopBar — Header atas aplikasi
// Menampilkan: foto profil, nama app, tombol sync
// ---------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(fotoUrl: String, onSyncDitekan: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Foto profil (dimuat dari URL menggunakan library Coil)
                AsyncImage(
                    model               = fotoUrl,
                    contentDescription  = "Foto profil",
                    modifier            = Modifier.size(40.dp).clip(CircleShape).background(HijauMuda),
                    contentScale        = ContentScale.Crop
                )
                Text("SIEPERKAP", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HijauUtama)
            }
        },
        actions = {
            // Tombol sync → panggil viewModel.onSyncDitekan()
            IconButton(onClick = onSyncDitekan) {
                Icon(Icons.Outlined.Sync, contentDescription = "Sync data", tint = HijauUtama)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FF))
    )
}


// ---------------------------------------------------------------
// COMPOSABLE: SectionSapaan — Sapaan user & chip status sync
// ---------------------------------------------------------------
@Composable
fun SectionSapaan(sapaan: String, nama: String, isSynced: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Teks sapaan: "Good Morning, Alex"
        Text(
            text       = "$sapaan, $nama",
            fontSize   = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF121C2A)
        )

        // Chip status sync (bentuk pil)
        Surface(shape = CircleShape, color = HijauMuda, shadowElevation = 2.dp) {
            Row(
                modifier            = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector        = if (isSynced) Icons.Filled.CheckCircle else Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint               = Color(0xFF007230),
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text       = if (isSynced) "All data synced" else "Not synced",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF007230)
                )
            }
        }
    }
}


// ---------------------------------------------------------------
// COMPOSABLE: SectionQuickActions — 2 tombol aksi cepat
// ---------------------------------------------------------------
@Composable
fun SectionQuickActions(onBorrowDitekan: () -> Unit, onLogActivityDitekan: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.fillMaxWidth()
    ) {
        // Tombol 1: Pinjam Alat (hijau = aksi utama)
        Button(
            onClick  = onBorrowDitekan,
            modifier = Modifier.weight(1f).height(90.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color(0xFF004B1E))
                Text("Borrow Equipment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF004B1E))
            }
        }

        // Tombol 2: Catat Kegiatan (abu = aksi sekunder)
        Button(
            onClick  = onLogActivityDitekan,
            modifier = Modifier.weight(1f).height(90.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9E3F6))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Color(0xFF3D4A3D))
                Text("Log Activity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3D4A3D))
            }
        }
    }
}


// ---------------------------------------------------------------
// COMPOSABLE: SectionInventori — 3 kartu statistik (bento grid)
// ---------------------------------------------------------------
@Composable
fun SectionInventori(stats: InventoryStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Inventory Overview", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF121C2A))

        // Baris 2 kartu kecil sejajar
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            KartuStatistik(
                modifier = Modifier.weight(1f),
                icon     = { Icon(Icons.Outlined.Output, contentDescription = null, tint = HijauUtama) },
                angka    = stats.borrowedCount,
                label    = "Borrowed items"
            )
            KartuStatistik(
                modifier = Modifier.weight(1f),
                icon     = { Icon(Icons.Outlined.CheckBox, contentDescription = null, tint = Color(0xFF006E2D)) },
                angka    = stats.availableCount,
                label    = "Available items"
            )
        }

        // Kartu penuh: Pending Sync
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEFF4FF), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier              = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier           = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFF8B7C)),
                        contentAlignment   = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Color(0xFF76231B))
                    }
                    Column {
                        Text(stats.pendingSyncCount.toString(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF121C2A))
                        Text("Pending sync items", fontSize = 11.sp, color = Color(0xFF3D4A3D))
                    }
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF6D7B6C))
            }
        }
    }
}


// ---------------------------------------------------------------
// COMPOSABLE HELPER: KartuStatistik — kartu reusable untuk angka
// ---------------------------------------------------------------
@Composable
fun KartuStatistik(modifier: Modifier = Modifier, icon: @Composable () -> Unit, angka: Int, label: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEFF4FF), shadowElevation = 1.dp, modifier = modifier.height(128.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            icon()
            Column {
                Text(angka.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF121C2A))
                Text(label, fontSize = 11.sp, color = Color(0xFF3D4A3D))
            }
        }
    }
}


// ---------------------------------------------------------------
// COMPOSABLE: SectionKegiatanAktif — kartu kegiatan scroll horizontal
// ---------------------------------------------------------------
@Composable
fun SectionKegiatanAktif(kegiatanList: List<Kegiatan>, onSeeAllDitekan: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Active Activities", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF121C2A))
            TextButton(onClick = onSeeAllDitekan) {
                Text("See All", color = HijauUtama, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Scroll horizontal: geser ke kanan untuk lihat kartu lainnya
        Row(
            modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            kegiatanList.forEach { kegiatan ->
                KartuKegiatan(kegiatan = kegiatan)
            }
        }
    }
}


// ---------------------------------------------------------------
// COMPOSABLE: KartuKegiatan — satu kartu kegiatan
// Warna border bawah berdasarkan StatusKegiatan dari domain model
// ---------------------------------------------------------------
@Composable
fun KartuKegiatan(kegiatan: Kegiatan) {
    // Warna ditentukan dari enum StatusKegiatan (bukan hardcode string)
    val warnaBorder = when (kegiatan.statusType) {
        StatusKegiatan.AKTIF        -> HijauUtama            // hijau
        StatusKegiatan.MAINTENANCE  -> Color(0xFFFF8B7C)     // oranye
        StatusKegiatan.AUDIT        -> Color(0xFF6D7B6C)     // abu
    }

    Surface(
        shape         = RoundedCornerShape(16.dp),
        color         = Color(0xFFF8F9FF),
        shadowElevation = 2.dp,
        modifier      = Modifier.width(280.dp)  // lebar tetap supaya bisa scroll
    ) {
        Box {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Baris atas: badge kategori + label waktu
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (kegiatan.statusType == StatusKegiatan.AKTIF) HijauMuda else Color(0xFFD9E3F6)
                    ) {
                        Text(
                            text     = kegiatan.kategori,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color    = if (kegiatan.statusType == StatusKegiatan.AKTIF) Color(0xFF007230) else Color(0xFF3D4A3D)
                        )
                    }
                    Text(kegiatan.labelWaktu, fontSize = 11.sp, color = Color(0xFF3D4A3D))
                }

                // Judul kegiatan
                Text(kegiatan.judul, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF121C2A))

                // Lokasi kegiatan
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFF3D4A3D), modifier = Modifier.size(18.dp))
                    Text(kegiatan.lokasi, fontSize = 14.sp, color = Color(0xFF3D4A3D))
                }

                // Progress bar (nilai dari domain model: 0.0f - 1.0f)
                LinearProgressIndicator(
                    progress   = { kegiatan.progress },
                    modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color      = warnaBorder,
                    trackColor = Color(0xFFD9E3F6)
                )
            }

            // Garis bawah berwarna (menggantikan border-b-4 dari HTML)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(warnaBorder)
            )
        }
    }
}


// ---------------------------------------------------------------
// COMPOSABLE: HomeBottomNav — navigasi bawah 5 menu
// "Home" = selected karena ini halaman Home
// ---------------------------------------------------------------
@Composable
fun HomeBottomNav(
    onNavigateToActivities : () -> Unit,
    onNavigateToInventory  : () -> Unit,
    onNavigateToHistory    : () -> Unit,
    onNavigateToProfile    : () -> Unit
) {
    NavigationBar(containerColor = Color(0xFFE6EEFF), tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = true,
            onClick  = {},
            icon     = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label    = { Text("Home") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF007230),
                selectedTextColor = Color(0xFF007230),
                indicatorColor    = HijauMuda
            )
        )
        NavigationBarItem(selected = false, onClick = onNavigateToActivities,
            icon = { Icon(Icons.Outlined.EventNote, contentDescription = null) }, label = { Text("Activities") })
        NavigationBarItem(selected = false, onClick = onNavigateToInventory,
            icon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) }, label = { Text("Inventory") })
        NavigationBarItem(selected = false, onClick = onNavigateToHistory,
            icon = { Icon(Icons.Outlined.History, contentDescription = null) }, label = { Text("History") })
        NavigationBarItem(selected = false, onClick = onNavigateToProfile,
            icon = { Icon(Icons.Filled.Person, contentDescription = null) }, label = { Text("Profile") })
    }
}


// ---------------------------------------------------------------
// WARNA KONSTANTA
// Sebaiknya dipindah ke core/ui/theme/Color.kt agar bisa dipakai
// di seluruh fitur tanpa duplikasi
// ---------------------------------------------------------------
val HijauUtama = Color(0xFF006E2F)  // primary - hijau tua
val HijauMuda  = Color(0xFF7CF994)  // secondary-container - hijau muda