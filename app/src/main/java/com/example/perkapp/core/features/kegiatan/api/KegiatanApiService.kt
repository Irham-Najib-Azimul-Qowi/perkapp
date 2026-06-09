/**
 * File: KegiatanApiService.kt
 *
 * FUNGSI UTAMA:
 * File ini berisi antarmuka (interface) Retrofit untuk melakukan komunikasi HTTP (REST API)
 * dengan backend (Laravel) terkait fitur kegiatan/peminjaman alat.
 *
 * PENJELASAN MENDALAM:
 * Di Retrofit, kita mendefinisikan endpoint API sebagai fungsi-fungsi Kotlin abstrak.
 * Setiap fungsi menggunakan anotasi HTTP (@GET, @POST, @PUT, @DELETE) yang menentukan:
 * 1. Method HTTP yang digunakan.
 * 2. Path (URL) endpoint relatif terhadap Base URL.
 * 3. Parameter request (Query, Path, atau Body).
 *
 * File ini juga memuat semua Data Transfer Object (DTO) — yaitu kelas-kelas data (data class)
 * yang memodelkan format JSON yang dikirimkan (Request) atau diterima (Response) dari server.
 * DTO ini akan otomatis dikonversi dari/ke JSON oleh Gson/Moshi yang dikonfigurasi di RetrofitClient.
 */
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
 * FUNGSI: KegiatanApiService
 * TUJUAN: Menjadi pintu gerbang utama antara aplikasi Android (Client) dengan
 * server backend (API) khusus untuk fitur Peminjaman/Kegiatan.
 */
interface KegiatanApiService {

    /**
     * FUNGSI: getHomeData
     * TUJUAN: Menarik rangkuman statistik untuk ditampilkan di layar Beranda (Dashboard),
     * seperti jumlah barang dipinjam, tersedia, dan daftar kegiatan yang sedang aktif.
     */
    @GET("home/data")
    suspend fun getHomeData(): HomeDataResponse

    /**
     * FUNGSI: getSemuaKegiatan
     * TUJUAN: Mengambil daftar riwayat seluruh kegiatan.
     * @param status (Opsional) Parameter filter, contoh: "?status=ongoing" untuk melihat 
     *               kegiatan yang sedang berjalan saja.
     */
    @GET("kegiatan")
    suspend fun getSemuaKegiatan(
        @Query("status") status: String? = null
    ): KegiatanListWrapperResponse

    /**
     * FUNGSI: getDetailKegiatan
     * TUJUAN: Menarik informasi rincian dari satu kegiatan spesifik berdasarkan ID-nya.
     */
    @GET("kegiatan/{id}")
    suspend fun getDetailKegiatan(
        @Path("id") id: String
    ): KegiatanWrapperResponse

    /**
     * FUNGSI: createKegiatan
     * TUJUAN: Mengirimkan form pendaftaran kegiatan baru ke server.
     */
    @POST("kegiatan")
    suspend fun createKegiatan(
        @Body request: CreateKegiatanRequest
    ): KegiatanWrapperResponse

    /**
     * FUNGSI: updateKegiatan
     * TUJUAN: Mengubah informasi atau status (misalnya dari 'pending' menjadi 'ongoing') 
     * sebuah kegiatan di server.
     */
    @PUT("kegiatan/{id}")
    suspend fun updateKegiatan(
        @Path("id") id: String,
        @Body request: UpdateKegiatanRequest
    ): KegiatanWrapperResponse

    /**
     * FUNGSI: deleteKegiatan
     * TUJUAN: Memusnahkan riwayat kegiatan tertentu dari database server.
     */
    @DELETE("kegiatan/{id}")
    suspend fun deleteKegiatan(
        @Path("id") id: String
    ): GeneralApiResponse

    /**
     * FUNGSI: addToolToKegiatan
     * TUJUAN: Mengaitkan (meminjamkan) sejumlah barang ke dalam suatu kegiatan spesifik.
     */
    @POST("kegiatan-alat")
    suspend fun addToolToKegiatan(
        @Body request: AddToolToKegiatanRequest
    ): GeneralApiResponse
}