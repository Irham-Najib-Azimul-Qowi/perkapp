package com.example.perkapp.model

// ============================================================
// FILE: KegiatanModel.kt
// LOKASI: features/kegiatan/domain/KegiatanModel.kt
// FUNGSI: Lapisan DOMAIN/MODEL dalam MVVM.
//         Berisi data class yang merepresentasikan data kegiatan.
//         File ini TIDAK boleh tahu soal UI atau API.
//         Hanya berisi bentuk/struktur data murni.
// ============================================================


// Data class untuk statistik inventori di halaman Home
data class InventoryStats(
    val borrowedCount: Int,     // Jumlah alat yang sedang dipinjam
    val availableCount: Int,    // Jumlah alat yang tersedia
    val pendingSyncCount: Int   // Jumlah data yang belum tersinkron ke server
)


// Data class untuk satu kegiatan yang sedang aktif
data class Kegiatan(
    val id: String,                  // ID unik kegiatan dari server
    val kategori: String,            // Contoh: "Research", "Maintenance", "Audit"
    val judul: String,               // Nama kegiatan, contoh: "Field Data Collection"
    val lokasi: String,              // Lokasi kegiatan, contoh: "Zone B - Sector 4"
    val labelWaktu: String,          // Label waktu: "2h left", "Active", "Started"
    val progress: Float,             // Progress 0.0f - 1.0f (0% - 100%)
    val statusType: StatusKegiatan,  // Tipe status untuk warna kartu
    val isPending: Boolean = false   // Status sync
)


// Enum tipe status kegiatan
// Dipakai untuk menentukan warna border bawah kartu aktivitas di UI
enum class StatusKegiatan {
    AKTIF,        // Warna hijau → kegiatan berjalan normal
    MAINTENANCE,  // Warna oranye → sedang maintenance
    AUDIT         // Warna abu → audit atau baru dimulai
}


// Data class informasi user yang sedang login
// Diambil dari UserPreferences milik Adam
data class UserInfo(
    val id: String = "",    // ID unik user
    val nama: String,       // Nama user, contoh: "Alex"
    val sapaan: String,     // Berdasarkan jam: "Good Morning", "Good Afternoon"
    val fotoUrl: String,    // URL foto profil user
    val role: String = "member" // Role user: admin atau member
)


// Data class utama yang merangkum semua data halaman Home
// ViewModel akan menyiapkan objek ini, lalu HomeScreen menampilkannya
data class HomeUiState(
    val userInfo: UserInfo,                  // Info user login
    val inventoryStats: InventoryStats,      // Statistik alat
    val kegiatanAktif: List<Kegiatan>,       // Daftar kegiatan yang sedang berjalan
    val isSynced: Boolean,                   // Status sync dengan server
    val isLoading: Boolean = false,          // Sedang loading data atau tidak
    val errorMessage: String? = null         // Pesan error jika gagal load data
)
