package com.example.perkapp.core.features.kegiatan.ui

/**
 * AktivitasScreen.kt
 *
 * File ini adalah halaman utama "Aktivitas" yang tampil di navbar.
 * Berisi daftar semua aktivitas/kegiatan yang bisa dicari dan difilter.
 *
 * Struktur file ini:
 *  - Data Models  → definisi enum StatusAktivitas dan data class Aktivitas
 *  - AktivitasScreen → composable utama (root screen)
 *  - OfflineBanner → banner merah saat tidak ada koneksi internet
 *  - SearchFilterSection → kolom pencarian + filter chip status
 *  - FilterChipItem → tombol chip individual (Berlangsung / Selesai / Draft)
 *  - AktivitasCard → card satu aktivitas di dalam list
 *  - StatusBadge → label berwarna status di pojok kiri atas card
 *  - ProgressSection → progress bar persentase untuk aktivitas berlangsung
 *  - VerifiedBadge → label "Terverifikasi" untuk aktivitas selesai
 *  - DraftBadge → label "Belum dikirim" untuk aktivitas draft
 *  - EmptyAktivitasState → tampilan ketika list kosong
 */

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel

// =============================================================================
// DATA MODELS
// Mendefinisikan struktur data yang digunakan di halaman ini.
// Nantinya pindahkan ke folder domain/ jika sudah terhubung ke API.
// =============================================================================

/**
 * Enum untuk status sebuah aktivitas.
 * - BERLANGSUNG : aktivitas sedang dikerjakan, tampil progress bar
 * - SELESAI     : aktivitas sudah selesai dan tersinkron
 * - DRAFT       : aktivitas belum dikirim ke server
 */
enum class StatusAktivitas { BERLANGSUNG, SELESAI, DRAFT }

/**
 * Data class yang merepresentasikan satu aktivitas/kegiatan.
 *
 * @param id       ID unik aktivitas (dari API/database)
 * @param judul    Nama/judul aktivitas yang ditampilkan di card
 * @param deskripsi Penjelasan singkat aktivitas
 * @param status   Status saat ini (lihat enum StatusAktivitas)
 * @param progress Persentase progres dalam bentuk float 0f–1f.
 *                 Contoh: 0.65f = 65%. Hanya dipakai jika status BERLANGSUNG.
 * @param tanggal  Teks tanggal yang ditampilkan di pojok kanan card
 */
data class Aktivitas(
    val id: String,
    val judul: String,
    val deskripsi: String,
    val status: StatusAktivitas,
    val progress: Float,
    val tanggal: String,
)

// =============================================================================
// ROOT SCREEN
// =============================================================================

/**
 * Composable utama untuk halaman Aktivitas.
 * Dipanggil dari NavGraph.kt milik Adam ketika user tap menu "Aktivitas" di navbar.
 *
 * @param viewModel          ViewModel yang menyuplai data dan logika (di-inject otomatis oleh Hilt)
 * @param onTambahAktivitas  Callback navigasi ke halaman TambahKegiatanScreen
 * @param onDetailAktivitas  Callback navigasi ke halaman DetailKegiatanScreen, membawa ID aktivitas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AktivitasScreen(
    viewModel: AktivitasViewModel = hiltViewModel(),
    onTambahAktivitas: () -> Unit = {},
    onDetailAktivitas: (String) -> Unit = {},
) {
    // Mengambil state terbaru dari ViewModel secara reaktif.
    // Setiap kali uiState berubah di ViewModel, UI ini otomatis recompose.
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        // FAB tombol "+" di pojok kanan bawah untuk membuat aktivitas baru
        floatingActionButton = {
            FloatingActionButton(
                onClick = onTambahAktivitas,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Aktivitas")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // innerPadding memastikan konten tidak tertutup FAB atau system bar
                .padding(innerPadding)
        ) {
            // Banner offline muncul/hilang dengan animasi slide vertikal.
            // AnimatedVisibility akan render OfflineBanner hanya jika isOffline = true.
            AnimatedVisibility(
                visible = uiState.isOffline,
                enter = expandVertically(),  // animasi muncul dari atas ke bawah
                exit = shrinkVertically(),   // animasi hilang dari bawah ke atas
            ) {
                OfflineBanner()
            }

            // LazyColumn = RecyclerView di Compose.
            // Hanya merender item yang terlihat di layar, lebih efisien untuk list panjang.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = 100.dp, // padding bawah agar card terakhir tidak tertutup navbar
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp), // jarak antar item
            ) {
                // Item pertama di list adalah section search + filter chip
                item {
                    SearchFilterSection(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        activeFilter = uiState.activeFilter,
                        onFilterChange = viewModel::onFilterChange,
                    )
                }

                // Cek apakah list kosong setelah filter diterapkan
                if (uiState.aktivitasList.isEmpty()) {
                    // Tampilkan ilustrasi kosong
                    item { EmptyAktivitasState() }
                } else {
                    // Render setiap aktivitas sebagai card.
                    // key = { it.id } membantu Compose mengidentifikasi item saat ada
                    // perubahan (insert, delete, reorder) agar animasi lebih smooth.
                    items(
                        items = uiState.aktivitasList,
                        key = { it.id },
                    ) { aktivitas ->
                        AktivitasCard(
                            aktivitas = aktivitas,
                            // Saat card di-tap, navigasi ke detail dengan membawa ID
                            onClick = { onDetailAktivitas(aktivitas.id) },
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// OFFLINE BANNER
// =============================================================================

/**
 * Banner merah yang muncul di bagian atas layar saat koneksi internet terputus.
 * Menampilkan icon cloud_off + teks peringatan.
 * Dipanggil dari AktivitasScreen dengan AnimatedVisibility.
 */
@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Offline – perubahan akan disinkronkan otomatis",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

// =============================================================================
// SEARCH & FILTER SECTION
// =============================================================================

/**
 * Bagian atas list yang berisi:
 * 1. TextField untuk mencari aktivitas berdasarkan judul/deskripsi
 * 2. Baris chip untuk memfilter berdasarkan status (Berlangsung / Selesai / Draft)
 *
 * @param query         Nilai teks pencarian saat ini (dari uiState)
 * @param onQueryChange Dipanggil setiap kali user mengetik di search bar
 * @param activeFilter  Filter status yang sedang aktif. null = tampilkan semua.
 * @param onFilterChange Dipanggil saat user tap chip filter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterSection(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilter: StatusAktivitas?,
    onFilterChange: (StatusAktivitas?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Search Bar ────────────────────────────────────────────────────────
        // OutlinedTextField dengan border transparan agar terlihat seperti filled field.
        // Border hanya muncul (warna primaryContainer) saat field sedang difokus.
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = {
                Text(
                    "Cari aktivitas...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            },
            leadingIcon = {
                // Icon kaca pembesar di sisi kiri field
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            },
            singleLine = true, // cegah field bisa multi-baris
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedBorderColor = Color.Transparent, // border hilang saat tidak fokus
                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )

        // ── Filter Chips ──────────────────────────────────────────────────────
        // horizontalScroll memungkinkan chip di-scroll ke kanan jika tidak muat di layar
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Chip "Berlangsung" — tap lagi untuk deselect (toggle behavior)
            FilterChipItem(
                label = "Berlangsung",
                selected = activeFilter == StatusAktivitas.BERLANGSUNG,
                onClick = {
                    // Jika chip ini sudah aktif → hapus filter (null = tampilkan semua)
                    // Jika belum aktif → aktifkan filter ini
                    onFilterChange(
                        if (activeFilter == StatusAktivitas.BERLANGSUNG) null
                        else StatusAktivitas.BERLANGSUNG
                    )
                },
            )
            FilterChipItem(
                label = "Selesai",
                selected = activeFilter == StatusAktivitas.SELESAI,
                onClick = {
                    onFilterChange(
                        if (activeFilter == StatusAktivitas.SELESAI) null
                        else StatusAktivitas.SELESAI
                    )
                },
            )
            FilterChipItem(
                label = "Draft",
                selected = activeFilter == StatusAktivitas.DRAFT,
                onClick = {
                    onFilterChange(
                        if (activeFilter == StatusAktivitas.DRAFT) null
                        else StatusAktivitas.DRAFT
                    )
                },
            )
        }
    }
}

/**
 * Satu tombol chip filter berbentuk pill (kapsul).
 * Warna berubah antara "aktif" (secondaryContainer) dan "nonaktif" (surfaceContainerHigh).
 *
 * @param label    Teks yang ditampilkan di dalam chip
 * @param selected true jika chip ini sedang aktif/dipilih
 * @param onClick  Aksi saat chip di-tap
 */
@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Warna background chip berubah berdasarkan kondisi selected
    val containerColor = if (selected)
        MaterialTheme.colorScheme.secondaryContainer  // hijau muda saat aktif
    else
        MaterialTheme.colorScheme.surfaceContainerHigh // abu-abu saat nonaktif

    // Warna teks chip juga menyesuaikan agar kontras dengan background
    val textColor = if (selected)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(CircleShape)           // bentuk pill/kapsul
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

// =============================================================================
// AKTIVITAS CARD
// =============================================================================

/**
 * Card yang menampilkan satu aktivitas dalam list.
 * Desain mengikuti HTML asli: ada garis aksen warna di sisi kiri card (border-l-4).
 *
 * Warna garis kiri:
 *  - BERLANGSUNG → primary (hijau)
 *  - SELESAI     → outline (abu-abu), card sedikit transparan
 *  - DRAFT       → error (merah)
 *
 * @param aktivitas Data aktivitas yang akan ditampilkan
 * @param onClick   Dipanggil saat user tap card → navigasi ke DetailKegiatanScreen
 */
@Composable
fun AktivitasCard(
    aktivitas: Aktivitas,
    onClick: () -> Unit,
) {
    // Tentukan warna garis aksen kiri berdasarkan status
    val borderColor = when (aktivitas.status) {
        StatusAktivitas.BERLANGSUNG -> MaterialTheme.colorScheme.primary
        StatusAktivitas.SELESAI     -> MaterialTheme.colorScheme.outline
        StatusAktivitas.DRAFT       -> MaterialTheme.colorScheme.error
    }

    // Card aktivitas selesai dibuat sedikit transparan (opacity 80%)
    // untuk memberi kesan "sudah tidak aktif"
    val isCompleted = aktivitas.status == StatusAktivitas.SELESAI

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // graphicsLayer(alpha) mengubah transparansi seluruh card
                if (isCompleted) Modifier.graphicsLayer(alpha = 0.8f) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // Garis aksen vertikal di sisi kiri card (mengikuti border-l-4 di HTML)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )

            // Konten utama card (status badge, judul, deskripsi, progress/badge)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Baris atas: status badge di kiri, tanggal di kanan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusBadge(status = aktivitas.status)
                    Text(
                        text = aktivitas.tanggal,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Judul aktivitas
                Text(
                    text = aktivitas.judul,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Deskripsi singkat aktivitas
                Text(
                    text = aktivitas.deskripsi,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Bagian bawah card berbeda tergantung status:
                when (aktivitas.status) {
                    StatusAktivitas.BERLANGSUNG -> {
                        // Tampilkan progress bar dengan persentase
                        ProgressSection(progress = aktivitas.progress)
                    }
                    StatusAktivitas.SELESAI -> {
                        // Tampilkan label "Terverifikasi & Tersinkron"
                        VerifiedBadge()
                    }
                    StatusAktivitas.DRAFT -> {
                        // Tampilkan label "Belum dikirim"
                        DraftBadge()
                    }
                }
            }
        }
    }
}

/**
 * Label pill berwarna di pojok kiri atas card yang menunjukkan status aktivitas.
 * Warna background dan teks otomatis menyesuaikan berdasarkan status.
 *
 * @param status Status aktivitas yang menentukan warna dan teks label
 */
@Composable
private fun StatusBadge(status: StatusAktivitas) {
    // Destructuring Triple: ambil label, warna background, dan warna teks sekaligus
    val (label, containerColor, contentColor) = when (status) {
        StatusAktivitas.BERLANGSUNG -> Triple(
            "Berlangsung",
            MaterialTheme.colorScheme.secondaryContainer,    // hijau muda
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        StatusAktivitas.SELESAI -> Triple(
            "Selesai",
            MaterialTheme.colorScheme.surfaceContainerHighest, // abu-abu
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusAktivitas.DRAFT -> Triple(
            "Draft",
            MaterialTheme.colorScheme.errorContainer,          // merah muda
            MaterialTheme.colorScheme.onErrorContainer,
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
        )
    }
}

/**
 * Progress bar yang ditampilkan di bawah deskripsi untuk aktivitas berstatus BERLANGSUNG.
 * Menampilkan label "Progress" di kiri dan persentase (misal "65%") di kanan,
 * diikuti LinearProgressIndicator berbentuk pill.
 *
 * @param progress Nilai float 0f–1f. Akan dikonversi ke persen untuk ditampilkan.
 */
@Composable
private fun ProgressSection(progress: Float) {
    // Konversi float ke integer persen untuk teks (0.65f → 65)
    val progressPercent = (progress * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Progress bar Material 3 dengan clip CircleShape agar ujungnya membulat
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

/**
 * Badge yang muncul di bagian bawah card untuk aktivitas berstatus SELESAI.
 * Menampilkan icon centang hijau + teks "Terverifikasi & Tersinkron".
 */
@Composable
private fun VerifiedBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Terverifikasi & Tersinkron",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Badge yang muncul di bagian bawah card untuk aktivitas berstatus DRAFT.
 * Menampilkan icon edit merah + teks "Belum dikirim".
 * Mengindikasikan aktivitas belum tersimpan ke server.
 */
@Composable
private fun DraftBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Belum dikirim",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// =============================================================================
// EMPTY STATE
// =============================================================================

/**
 * Tampilan pengganti list ketika tidak ada aktivitas yang ditemukan.
 * Muncul dalam dua kondisi:
 *  1. Belum ada aktivitas sama sekali
 *  2. Hasil pencarian/filter tidak menemukan data yang cocok
 *
 * Menampilkan icon besar + dua baris teks panduan.
 */
@Composable
private fun EmptyAktivitasState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp), // beri jarak dari atas agar terasa "di tengah layar"
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon besar sebagai ilustrasi visual
        Icon(
            imageVector = Icons.Outlined.EventNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "Belum ada aktivitas",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Teks panduan untuk user agar tahu cara menambah aktivitas
        Text(
            text = "Tap tombol + untuk menambahkan aktivitas baru",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}