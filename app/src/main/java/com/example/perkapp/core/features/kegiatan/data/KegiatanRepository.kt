package com.example.perkapp.features.kegiatan.data

import com.example.perkapp.features.kegiatan.api.KegiatanApiService
import com.example.perkapp.features.kegiatan.domain.InventoryStats
import com.example.perkapp.features.kegiatan.domain.Kegiatan
import com.example.perkapp.features.kegiatan.domain.StatusKegiatan
import com.example.perkapp.features.kegiatan.domain.UserInfo
import com.example.perkapp.features.kegiatan.mapper.toDomain
import com.example.perkapp.features.kegiatan.mapper.toDomainList
import java.util.Calendar

// ============================================================
// FILE: KegiatanRepository.kt
// LOKASI: features/kegiatan/data/KegiatanRepository.kt
// FUNGSI: Lapisan DATA dalam MVVM.
//         Repository adalah "jembatan" antara sumber data (API/DB)
//         dengan ViewModel. ViewModel tidak perlu tahu data
//         datang dari API, database, atau cache.
//
// Kenapa pakai Repository?
// - ViewModel jadi lebih bersih, hanya panggil fungsi repository
// - Kalau mau ganti sumber data (API → Room DB), hanya ubah sini
// - Lebih mudah di-test (bisa mock repository-nya)
// ============================================================


// Interface Repository: mendefinisikan kontrak fungsi yang tersedia
// ViewModel hanya kenal interface ini, bukan implementasinya
interface KegiatanRepository {
    suspend fun getInventoryStats(): Result<InventoryStats>
    suspend fun getKegiatanAktif(): Result<List<Kegiatan>>
    suspend fun getUserInfo(): UserInfo
}


// Implementasi nyata dari KegiatanRepository
// Menerima KegiatanApiService dari Retrofit (buatan Adam)
class KegiatanRepositoryImpl(
    private val apiService: KegiatanApiService

    // TODO: Tambahkan parameter ini saat Adam sudah selesai:
    // private val userPreferences: UserPreferences  ← dari Adam (datastore)
    // private val db: AppDatabase                   ← dari core/database (bersama)

) : KegiatanRepository {

    // ------------------------------------------------------------
    // Ambil statistik inventori dari API
    // Result<T> = wrapper sukses/gagal bawaan Kotlin
    // Kalau sukses → Result.success(data)
    // Kalau gagal  → Result.failure(exception)
    // ------------------------------------------------------------
    override suspend fun getInventoryStats(): Result<InventoryStats> {
        return try {
            val response = apiService.getHomeData()
            // Konversi response API → domain model menggunakan Mapper
            Result.success(response.stats.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // ------------------------------------------------------------
    // Ambil daftar kegiatan yang sedang aktif dari API
    // ------------------------------------------------------------
    override suspend fun getKegiatanAktif(): Result<List<Kegiatan>> {
        return try {
            val response = apiService.getHomeData()
            // Konversi list response API → list domain model
            Result.success(response.kegiatan_aktif.toDomainList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // ------------------------------------------------------------
    // Ambil info user yang sedang login
    // TODO: Ganti dengan data dari UserPreferences milik Adam
    //       saat sudah tersedia di branch Adam
    // ------------------------------------------------------------
    override suspend fun getUserInfo(): UserInfo {
        // Sementara pakai data dummy dulu
        // Nanti ganti dengan: userPreferences.getUserName(), dll
        return UserInfo(
            nama = "Alex",
            sapaan = getSapaanBerdasarkanJam(),
            fotoUrl = ""
        )
    }


    // ------------------------------------------------------------
    // Helper: menentukan sapaan berdasarkan jam saat ini
    // Pagi (00-11)  → "Good Morning"
    // Siang (12-17) → "Good Afternoon"
    // Malam (18-23) → "Good Evening"
    // ------------------------------------------------------------
    private fun getSapaanBerdasarkanJam(): String {
        val jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            jam < 12 -> "Good Morning"
            jam < 18 -> "Good Afternoon"
            else     -> "Good Evening"
        }
    }
}

// ============================================================
// Fake/Mock Repository untuk pengujian halaman Home
// ============================================================
class FakeKegiatanRepository : KegiatanRepository {

    // Mengembalikan data dummy statistik inventori secara langsung
    override suspend fun getInventoryStats(): Result<InventoryStats> {
        return Result.success(
            InventoryStats(
                borrowedCount = 5,       // 5 barang sedang dipinjam
                availableCount = 28,     // 28 barang tersedia
                pendingSyncCount = 1     // 1 barang menunda sinkronisasi
            )
        )
    }

    // Mengembalikan list dummy kegiatan yang sedang aktif secara langsung
    override suspend fun getKegiatanAktif(): Result<List<Kegiatan>> {
        return Result.success(
            listOf(
                Kegiatan(
                    id = "101",
                    kategori = "Research",
                    judul = "Audit Fasilitas Hutan",
                    lokasi = "Zona A - Sektor 3",
                    labelWaktu = "Sisa 2 jam",
                    progress = 0.7f,
                    statusType = StatusKegiatan.AKTIF
                ),
                Kegiatan(
                    id = "102",
                    kategori = "Maintenance",
                    judul = "Servis Panel Surya",
                    lokasi = "Gedung Energi",
                    labelWaktu = "Aktif",
                    progress = 0.45f,
                    statusType = StatusKegiatan.MAINTENANCE
                ),
                Kegiatan(
                    id = "103",
                    kategori = "Audit",
                    judul = "Pengecekan Genset Utama",
                    lokasi = "Gedung Genset",
                    labelWaktu = "Baru Mulai",
                    progress = 0.1f,
                    statusType = StatusKegiatan.AUDIT
                )
            )
        )
    }

    // Mengembalikan info user dummy beserta sapaan dinamis berdasarkan waktu
    override suspend fun getUserInfo(): UserInfo {
        return UserInfo(
            nama = "Reja", // Nama diubah menjadi Reja sebagai pengguna saat ini
            sapaan = getSapaanBerdasarkanJam(), // Memakai fungsi sapaan berbasis jam
            fotoUrl = "" // Foto profil kosong
        )
    }

    // Fungsi pembantu tambahan untuk menentukan sapaan berbasis jam saat ini
    private fun getSapaanBerdasarkanJam(): String {
        val jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            jam < 12 -> "Good Morning"
            jam < 18 -> "Good Afternoon"
            else     -> "Good Evening"
        }
    }
}