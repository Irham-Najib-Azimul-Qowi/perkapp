package com.example.perkapp.features.kegiatan.api

import retrofit2.http.*

// Response DTO dari API untuk satu kegiatan
data class KegiatanResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val date: String?,
    val status: String?,
    val created_by: String?,
    val alats: List<AlatPivotResponse>? = null
)

// Pivot menunjukkan relasi Many-to-Many antara Kegiatan dan Alat
data class AlatPivotResponse(
    val id: String,
    val name: String,
    val category: String,
    val image_path: String?,
    val images: List<com.example.perkapp.features.alat.data.remote.ImageResponse>? = null,
    val pivot: PivotQty?
)

data class PivotQty(
    val qty: Int
)

data class KegiatanListWrapperResponse(
    val success: Boolean,
    val message: String,
    val data: List<KegiatanResponse>
)

data class KegiatanWrapperResponse(
    val success: Boolean,
    val message: String,
    val data: KegiatanResponse
)

// Response DTO untuk statistik inventori (Dashboard)
data class InventoryStatsResponse(
    val borrowed_count: Int,
    val available_count: Int,
    val pending_sync_count: Int
)

// Response wrapper dari API (Dashboard Beranda)
data class HomeDataResponse(
    val stats: InventoryStatsResponse,
    val kegiatan_aktif: List<KegiatanResponse>
)

data class CreateKegiatanRequest(
    val id: String? = null,
    val name: String,
    val description: String?,
    val date: String,
    val status: String
)

data class UpdateKegiatanRequest(
    val name: String,
    val description: String?,
    val date: String,
    val status: String
)

data class AddToolToKegiatanRequest(
    val kegiatan_id: String,
    val alat_id: String,
    val qty: Int
)

data class GeneralApiResponse(
    val success: Boolean,
    val message: String
)

/**
 * KegiatanApiService — Rute API untuk fitur Peminjaman/Kegiatan.
 */
interface KegiatanApiService {

    // Mengambil data rangkuman statistik untuk halaman depan (Home/Beranda)
    @GET("home/data")
    suspend fun getHomeData(): HomeDataResponse

    // Mengambil seluruh riwayat acara/kegiatan yang ada
    @GET("kegiatan")
    suspend fun getSemuaKegiatan(
        @Query("status") status: String? = null // Bisa difilter misalnya: "?status=ongoing"
    ): KegiatanListWrapperResponse

    // Mengambil rincian detail 1 kegiatan tertentu
    @GET("kegiatan/{id}")
    suspend fun getDetailKegiatan(
        @Path("id") id: String
    ): KegiatanWrapperResponse

    // Membuat kegiatan baru
    @POST("kegiatan")
    suspend fun createKegiatan(
        @Body request: CreateKegiatanRequest
    ): KegiatanWrapperResponse

    // Memperbarui informasi kegiatan (termasuk acc peminjaman)
    @PUT("kegiatan/{id}")
    suspend fun updateKegiatan(
        @Path("id") id: String,
        @Body request: UpdateKegiatanRequest
    ): KegiatanWrapperResponse

    // Menghapus kegiatan
    @DELETE("kegiatan/{id}")
    suspend fun deleteKegiatan(
        @Path("id") id: String
    ): GeneralApiResponse

    // Menyisipkan daftar pinjaman alat ke dalam sebuah kegiatan
    @POST("kegiatan-alat")
    suspend fun addToolToKegiatan(
        @Body request: AddToolToKegiatanRequest
    ): GeneralApiResponse
}