package com.example.perkapp.core.features.kegiatan.ui

// Mengimpor modul-modul animasi Jetpack Compose untuk mendukung transisi UI yang halus
import androidx.compose.animation.*
// Mengimpor modul dasar layout, gestur, klik, scroll, dan penggambaran bentuk
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
// Mengimpor koleksi ikon dasar Material Design
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
// Mengimpor elemen-elemen UI Material 3
import androidx.compose.material3.*
// Mengimpor library Compose Runtime untuk mengelola status (state)
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
// Mengimpor modul integrasi ViewModel Hilt ke Compose
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Enum status aktivitas yang membedakan tipe kondisi kegiatan.
 */
enum class StatusAktivitas { BERLANGSUNG, SELESAI, DRAFT }

/**
 * Representasi data class kegiatan untuk halaman Aktivitas.
 */
data class Aktivitas(
    val id: String, // ID unik kegiatan
    val judul: String, // Judul nama kegiatan
    val deskripsi: String, // Uraian pendek mengenai aktivitas
    val status: StatusAktivitas, // Status pengerjaan saat ini
    val progress: Float, // Progres pengerjaan (0.0f - 1.0f)
    val tanggal: String, // Informasi tanggal atau status langsung
    val isPending: Boolean = false, // Status sync
    val peminjam: String = "",
    val realDeskripsi: String = "",
    val createdBy: String? = null,
    val alatApproved: Boolean = false // Status persetujuan alat oleh admin
)

/**
 * Composable utama (Root Screen) untuk menampilkan daftar Aktivitas/Kegiatan secara keseluruhan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AktivitasScreen(
    viewModel: AktivitasViewModel = hiltViewModel(), // Injeksi otomatis menggunakan Hilt
    onTambahAktivitas: () -> Unit = {},
    onDetailAktivitas: (String) -> Unit = {},
) {
    // Mengamati UI state dari ViewModel secara reaktif
    val uiState by viewModel.uiState.collectAsState()

    // Scaffold menyediakan struktur dasar halaman Material 3
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Aktivitas Kegiatan",
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
        floatingActionButton = {
            // Tombol melayang di pojok kanan bawah (+) berwarna hijau cerah dengan ikon plus hitam
            FloatingActionButton(
                onClick = onTambahAktivitas, // Callback tombol diklik
                shape = CircleShape, // Bentuk bulat sempurna sesuai gambar
                containerColor = MaterialTheme.colorScheme.primary, // Hijau cerah sesuai gambar
                contentColor = Color.White, // Warna ikon plus hitam sesuai gambar
                modifier = Modifier
                    .size(64.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                // Ikon tambah "+" bawaan Material Icons
                Icon(Icons.Default.Add, contentDescription = "Add Activity", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding -> // Menampung padding Scaffold secara aman agar konten tidak tertutup
        Column(
            modifier = Modifier
                .fillMaxSize() // Memenuhi layar
                .padding(innerPadding) // Padding otomatis
        ) {
            // Menampilkan banner offline dengan animasi geser turun/naik jika status isOffline aktif
            AnimatedVisibility(
                visible = uiState.isOffline,
                enter = expandVertically(), // Animasi muncul secara vertikal
                exit = shrinkVertically(), // Animasi menyusut vertikal saat offline berakhir
            ) {
                // Merender komponen banner penanda offline
                OfflineBanner()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 100.dp, // Jarak padding bawah agar kartu terakhir tidak tertutupi
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp), // Mengatur jarak antar item list
            ) {

                // Komponen kedua berupa kolom pencarian dan filter chip status
                item {
                    SearchFilterSection(
                        query = uiState.searchQuery, // String pencarian saat ini
                        onQueryChange = viewModel::onSearchQueryChange, // Memicu pencarian ulang saat mengetik
                        activeFilter = uiState.activeFilter, // Status filter yang aktif
                        onFilterChange = viewModel::onFilterChange, // Mengganti status filter
                    )
                }

                // Mengecek apabila list aktivitas yang difilter kosong
                if (uiState.aktivitasList.isEmpty()) {
                    // Merender visualisasi kosong (Empty State)
                    item { EmptyAktivitasState() }
                } else {
                    // Merender masing-masing card aktivitas menggunakan items() secara efisien
                    items(
                        items = uiState.aktivitasList,
                        key = { it.id }, // Memberikan key unik agar perombakan posisi list lebih cepat di-render
                    ) { aktivitas ->
                        AktivitasCard(
                            aktivitas = aktivitas,
                            // Memicu aksi navigasi ke detail saat kartu di-tap dengan membawa ID
                            onClick = { onDetailAktivitas(aktivitas.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Banner penanda koneksi internet offline yang berwarna merah kontras.
 */
@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth() // Lebar penuh
            .background(MaterialTheme.colorScheme.errorContainer) // Latar kontainer error merah
            .padding(horizontal = 16.dp, vertical = 10.dp), // Padding dalam
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Ikon awan terputus/off
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.width(8.dp)) // Jarak spasi horizontal
        // Teks deskripsi info offline
        Text(
            text = "Offline – changes will be synced automatically",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/**
 * Baris pencarian teks (English) dan filter chip status pengerjaan (In Progress / Completed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterSection(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilter: StatusAktivitas?,
    onFilterChange: (StatusAktivitas?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Kolom input pencarian (TextField) dengan ikon kaca pembesar berbahasa Inggris
        OutlinedTextField(
            value = query, // Mengikat teks pencarian
            onValueChange = onQueryChange, // Pemicu perubahan teks saat diketik
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = {
                // Teks petunjuk input bahasa Inggris sesuai gambar
                Text(
                    "Search activities...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            },
            leadingIcon = {
                // Ikon pencarian di kiri input
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            },
            singleLine = true, // Mengunci input hanya satu baris saja
            shape = RoundedCornerShape(12.dp), // Membuat ujung melingkar
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFEFF4FF), // Latar belakang biru-abu sangat muda sesuai gambar
                focusedContainerColor = Color(0xFFEFF4FF),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary, // Border berwarna hijau saat aktif
            ),
        )

        // Baris gulir horizontal berisi tombol filter chip (hanya In Progress & Completed)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()), // Aktif scroll ke samping
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Chip status: In Progress
            FilterChipItem(
                label = "In Progress",
                selected = activeFilter == StatusAktivitas.BERLANGSUNG,
                onClick = {
                    onFilterChange(
                        if (activeFilter == StatusAktivitas.BERLANGSUNG) null
                        else StatusAktivitas.BERLANGSUNG
                    )
                },
            )
            // Chip status: Completed
            FilterChipItem(
                label = "Completed",
                selected = activeFilter == StatusAktivitas.SELESAI,
                onClick = {
                    onFilterChange(
                        if (activeFilter == StatusAktivitas.SELESAI) null
                        else StatusAktivitas.SELESAI
                    )
                },
            )
        }
    }
}

/**
 * Item filter individual berbentuk kapsul pill.
 */
@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Penyesuaian warna latar belakang berdasarkan terpilih (selected) atau tidak
    val containerColor = if (selected)
        Color(0xFF7CF994) // Hijau muda cerah saat dipilih sesuai gambar
    else
        Color(0xFFD9E3F6) // Abu-biru muda saat pasif sesuai gambar

    // Penyesuaian warna teks agar kontras dengan warna latar belakang chip
    val textColor = if (selected)
        Color(0xFF007230) // Hijau tua saat terpilih
    else
        Color(0xFF3D4A3D) // Abu-abu gelap saat pasif

    Box(
        modifier = Modifier
            .clip(CircleShape) // Memotong dengan bentuk kapsul melingkar
            .background(containerColor)
            .clickable(onClick = onClick) // Menangani interaksi tap user
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Card komponen visual representasi dari satu kegiatan aktivitas.
 */
@Composable
fun AktivitasCard(
    aktivitas: Aktivitas,
    onClick: () -> Unit,
) {
    val isCompleted = aktivitas.status == StatusAktivitas.SELESAI
    val isDraft = aktivitas.status == StatusAktivitas.DRAFT
    
    val statusLabel = when (aktivitas.status) {
        StatusAktivitas.BERLANGSUNG -> "In Progress"
        StatusAktivitas.SELESAI -> "Completed"
        StatusAktivitas.DRAFT -> "Draft"
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCompleted) Modifier.graphicsLayer(alpha = 0.9f) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
         ) {
             // Info Column
             Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = aktivitas.judul,
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
                            color = if (aktivitas.status == StatusAktivitas.BERLANGSUNG) MaterialTheme.colorScheme.primaryContainer else Color(0xFFD9E3F6),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (aktivitas.status == StatusAktivitas.BERLANGSUNG) MaterialTheme.colorScheme.primary else Color(0xFF3D4A3D)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = aktivitas.deskripsi,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom sync-style banner
                val bannerColor = if (aktivitas.isPending) {
                    Color(0xFFFFF3CD)
                } else {
                    when (aktivitas.status) {
                        StatusAktivitas.BERLANGSUNG -> Color(0xFFE8F5E9)
                        StatusAktivitas.SELESAI -> Color(0xFFE8F5E9)
                        StatusAktivitas.DRAFT -> Color(0xFFFFF3CD)
                    }
                }
                val bannerTextColor = if (aktivitas.isPending) {
                    Color(0xFF856404)
                } else {
                    when (aktivitas.status) {
                        StatusAktivitas.BERLANGSUNG -> Color(0xFF2E7D32)
                        StatusAktivitas.SELESAI -> Color(0xFF2E7D32)
                        StatusAktivitas.DRAFT -> Color(0xFF856404)
                    }
                }
                val bannerText = if (aktivitas.isPending) {
                    "Pending (Menunggu Sinkronisasi)"
                } else {
                    when (aktivitas.status) {
                        StatusAktivitas.BERLANGSUNG -> "Aktivitas Berlangsung"
                        StatusAktivitas.SELESAI -> "Aktivitas Selesai (Completed)"
                        StatusAktivitas.DRAFT -> "Draf Aktivitas (Draft)"
                    }
                }

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

/**
 * Status Badge berbentuk kapsul kecil untuk menyorot status pengerjaan kegiatan (English).
 */
@Composable
private fun StatusBadge(status: StatusAktivitas) {
    // Destructuring Triple: Menentukan warna latar, warna teks, dan teks label bahasa Inggris sesuai gambar
    val (label, containerColor, contentColor) = when (status) {
        StatusAktivitas.BERLANGSUNG -> Triple(
            "In Progress",
            Color(0xFF7CF994), // Hijau muda cerah sesuai gambar
            Color(0xFF007230),
        )
        StatusAktivitas.SELESAI -> Triple(
            "Completed",
            Color(0xFFEFF4FF), // Biru-abu sangat muda sesuai gambar
            Color(0xFF3D4A3D),
        )
        StatusAktivitas.DRAFT -> Triple(
            "Draft",
            Color(0xFFEFF4FF),
            Color(0xFF3D4A3D),
        )
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Menampilkan baris tingkat progres dalam bentuk bar persentase (English).
 */
@Composable
private fun ProgressSection(progress: Float) {
    // Mengonversi nilai progres pecahan ke satuan persen bilangan bulat (contoh: 0.65f -> 65)
    val progressPercent = (progress * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF3D4A3D),
            )
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF121C2A),
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Indikator linear progres M3 dengan ujung melingkar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = Color(0xFF006E2F), // Warna hijau utama progres sesuai gambar
            trackColor = Color(0xFFEFF4FF), // Latar belakang bar abu-biru sesuai gambar
        )
    }
}

/**
 * Badge penanda status terverifikasi (Selesai).
 */
@Composable
private fun VerifiedBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Ikon centang bulat berwarna hijau
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF006E2F), // Hijau utama sesuai gambar
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Verified & Synced", // Bahasa Inggris sesuai gambar
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF006E2F),
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Badge penanda status draf (Draft).
 */
@Composable
private fun DraftBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Ikon kertas pensil (Edit Note) berwarna abu-abu
        Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = null,
            tint = Color(0xFF6D7B6C),
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Draft",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF6D7B6C),
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Menampilkan ilustrasi dan petunjuk teks ketika list kegiatan kosong/tidak cocok dengan filter.
 */
@Composable
private fun EmptyAktivitasState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp), // Menggeser jarak ke bawah agar visual seimbang di tengah
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ikon berkas catatan aktivitas berukuran besar
        Icon(
            imageVector = Icons.Outlined.EventNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        // Informasi utama list kosong (Bahasa Inggris)
        Text(
            text = "No activities found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Arahan aksi untuk mereset filter
        Text(
            text = "Try searching for another keyword or change filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}