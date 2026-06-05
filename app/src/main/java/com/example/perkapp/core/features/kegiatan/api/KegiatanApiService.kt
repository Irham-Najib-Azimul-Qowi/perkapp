package com.example.perkapp.features.kegiatan.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ============================================================
// FILE: KegiatanApiService.kt
// LOKASI: features/kegiatan/api/KegiatanApiService.kt
// FUNGSI: Mendefinisikan endpoint API untuk fitur kegiatan.
//         Retrofit akan otomatis membuat implementasinya.
//
// CATATAN: File ini bergantung pada RetrofitClient.kt milik Adam.
//          Koordinasi dengan Adam untuk memastikan base URL
//          dan header autentikasi sudah terpasang di sana.
// ============================================================


// Response DTO dari API untuk satu kegiatan
// DTO = Data Transfer Object, bentuk data yang datang dari server
// Berbeda dengan domain model (Kegiatan.kt) karena nama field
// mengikuti format JSON dari backend (snake_case)
data class KegiatanResponse(
    val id: String,
    val kategori: String,
    val judul: String,
    val lokasi: String,
    val label_waktu: String,    // snake_case dari JSON backend
    val progress: Float,
    val status: String          // "AKTIF", "MAINTENANCE", "AUDIT"
)


// Response DTO untuk statistik inventori
data class InventoryStatsResponse(
    val borrowed_count: Int,
    val available_count: Int,
    val pending_sync_count: Int
)


// Response wrapper dari API (koordinasi dengan Adam soal format ini)
data class HomeDataResponse(
    val stats: InventoryStatsResponse,
    val kegiatan_aktif: List<KegiatanResponse>
)


// Interface endpoint API untuk fitur kegiatan
// Retrofit membaca anotasi @GET, @POST, dll untuk tahu cara request
interface KegiatanApiService {

    // Ambil semua data yang dibutuhkan halaman Home sekaligus
    // GET /home/data
    @GET("home/data")
    suspend fun getHomeData(): HomeDataResponse

    // Ambil daftar semua kegiatan (untuk halaman "See All")
    // GET /kegiatan?status=aktif
    @GET("kegiatan")
    suspend fun getSemuaKegiatan(
        @Query("status") status: String? = null  // filter opsional: "aktif", "selesai", dll
    ): List<KegiatanResponse>

    // Ambil detail satu kegiatan berdasarkan ID
    // GET /kegiatan/{id}
    @GET("kegiatan/{id}")
    suspend fun getDetailKegiatan(
        @Path("id") id: String
    ): KegiatanResponse
}