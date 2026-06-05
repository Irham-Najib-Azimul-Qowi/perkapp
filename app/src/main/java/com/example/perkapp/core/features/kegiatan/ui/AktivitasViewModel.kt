package com.example.perkapp.core.features.kegiatan.ui

// Mengimpor kelas ViewModel dari Android Jetpack
import androidx.lifecycle.ViewModel
// Mengimpor StateFlow, MutableStateFlow, dan fungsi pembantu update untuk manajemen state reaktif
import kotlinx.coroutines.flow.*

/**
 * UI State untuk menampung seluruh kondisi data dan status halaman Aktivitas.
 *
 * @param aktivitasList List aktivitas yang saat ini ditampilkan setelah filter
 * @param isLoading Menandakan proses pemuatan sedang berlangsung atau selesai
 * @param isOffline Menandakan koneksi internet terputus
 * @param searchQuery Kata pencarian yang diketik user
 * @param activeFilter Filter status yang aktif
 * @param errorMessage Pesan kesalahan jika terjadi kegagalan pemuatan data
 */
data class AktivitasUiState(
    val aktivitasList: List<Aktivitas> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: StatusAktivitas? = null,
    val errorMessage: String? = null,
)

/**
 * AktivitasViewModel mengelola alur data searah (UDF) untuk halaman AktivitasScreen.
 */
class AktivitasViewModel : ViewModel() {

    // _uiState menyimpan state UI secara private (mutable)
    private val _uiState = MutableStateFlow(AktivitasUiState())
    // uiState mengekspos state UI secara read-only (public) ke halaman AktivitasScreen
    val uiState: StateFlow<AktivitasUiState> = _uiState.asStateFlow()

    // _allAktivitas menyimpan sumber data asli kegiatan sebelum filter diterapkan
    private val _allAktivitas = MutableStateFlow<List<Aktivitas>>(emptyList())

    // Dijalankan otomatis saat pertama kali class AktivitasViewModel di-instantiate
    init {
        // Memuat data dummy untuk keperluan development UI
        loadDummyData()

        // TODO: Aktifkan observasi jaringan setelah NetworkMonitor dari Adam sudah di-merge
        // observeNetwork()
    }

    /**
     * Dipanggil setiap kali terjadi perubahan input teks pencarian oleh user.
     *
     * @param query Teks terbaru di bar pencarian
     */
    fun onSearchQueryChange(query: String) {
        // Memperbarui properti searchQuery di dalam UI State
        _uiState.update { it.copy(searchQuery = query) }
        // Menerapkan filter gabungan (pencarian + status)
        applyFilter()
    }

    /**
     * Dipanggil sewaktu pengguna menekan salah satu chip filter status (Berlangsung/Selesai/Draft).
     *
     * @param filter StatusAktivitas yang dipilih, atau null untuk menghapus filter status
     */
    fun onFilterChange(filter: StatusAktivitas?) {
        // Memperbarui properti activeFilter di dalam UI State
        _uiState.update { it.copy(activeFilter = filter) }
        // Menerapkan kembali filter gabungan
        applyFilter()
    }

    /**
     * Dipanggil ketika pengguna memicu pembaruan data (misalnya geser layar ke bawah / pull to refresh).
     */
    fun refresh() {
        // TODO: Ganti ke pemanggilan repository.syncAktivitas() jika API siap digunakan
        loadDummyData()
    }

    /**
     * Menyaring daftar kegiatan asli (_allAktivitas) berdasarkan input teks pencarian dan filter status.
     * Hasil saringan disimpan ke properti aktivitasList pada uiState agar UI me-render ulang secara otomatis.
     */
    private fun applyFilter() {
        // Mengambil snapshot dari state saat ini
        val state = _uiState.value
        // Menyaring data list kegiatan asli
        val filtered = _allAktivitas.value
            .filter { aktivitas ->
                // Memeriksa apakah kolom pencarian kosong, atau judul/deskripsi mengandung teks pencarian (case-insensitive)
                val matchQuery = state.searchQuery.isBlank() ||
                        aktivitas.judul.contains(state.searchQuery, ignoreCase = true) ||
                        aktivitas.deskripsi.contains(state.searchQuery, ignoreCase = true)

                // Memeriksa apakah filter status non-aktif (null), atau status kegiatan cocok dengan filter aktif
                val matchFilter = state.activeFilter == null ||
                        aktivitas.status == state.activeFilter

                // Data lolos saringan hanya jika memenuhi kriteria pencarian AND kriteria filter status
                matchQuery && matchFilter
            }

        // Memperbarui list aktivitas yang siap ditayangkan di UI
        _uiState.update { it.copy(aktivitasList = filtered) }
    }

    /**
     * Memasukkan data dummy awal ke list lokal agar antarmuka pengguna dapat dicoba.
     */
    private fun loadDummyData() {
        val dummy = listOf(
            Aktivitas(
                id = "1",
                judul = "Campus Facility Audit", // Disesuaikan dengan gambar
                deskripsi = "Quarterly safety and infrastructure inspection for West Wing.", // Disesuaikan dengan gambar
                status = StatusAktivitas.BERLANGSUNG, // In Progress
                progress = 0.65f, // 65%
                tanggal = "In Progress", // Status waktu di pojok kanan atas
            ),
            Aktivitas(
                id = "2",
                judul = "Annual Stocktake 2023", // Disesuaikan dengan gambar
                deskripsi = "Global verification of all categorized assets and IT equipment.", // Disesuaikan dengan gambar
                status = StatusAktivitas.SELESAI, // Completed
                progress = 1f, // 100% selesai
                tanggal = "Oct 18, 2023", // Tanggal rilis selesai
            ),
            Aktivitas(
                id = "3",
                judul = "Building C AC Maintenance", // Bahasa Inggris untuk kegiatan draf
                deskripsi = "Routine AC unit checking across all rooms on Building C floors 2-4.",
                status = StatusAktivitas.DRAFT, // Tetap disimpan di repo lokal
                progress = 0f, // Belum mulai
                tanggal = "Draft",
            ),
        )

        // Simpan ke raw list (untuk keperluan filter ulang)
        _allAktivitas.value = dummy

        // Langsung tampilkan semua ke UI tanpa filter
        _uiState.update { it.copy(aktivitasList = dummy) }
    }

    // =========================================================================
    // BLOK LOGIKA PENGAMAT KONEKSI INTERNET
    // Aktifkan blok ini setelah NetworkMonitor.kt dari Adam selesai
    // =========================================================================

    /**
     * Mengamati status online/offline jaringan secara reaktif.
     */
    // private fun observeNetwork() {
    //     viewModelScope.launch {
    //         networkMonitor.isOnline.collect { isOnline ->
    //             _uiState.update { it.copy(isOffline = !isOnline) }
    //         }
    //     }
    // }
}
