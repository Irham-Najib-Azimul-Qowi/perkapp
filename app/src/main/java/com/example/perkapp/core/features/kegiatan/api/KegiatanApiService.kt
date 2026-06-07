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

data class AlatPivotResponse(
    val id: String,
    val name: String,
    val category: String,
    val image_path: String?,
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

// Response DTO untuk statistik inventori
data class InventoryStatsResponse(
    val borrowed_count: Int,
    val available_count: Int,
    val pending_sync_count: Int
)

// Response wrapper dari API
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

// Interface endpoint API untuk fitur kegiatan
interface KegiatanApiService {

    @GET("home/data")
    suspend fun getHomeData(): HomeDataResponse

    @GET("kegiatan")
    suspend fun getSemuaKegiatan(
        @Query("status") status: String? = null
    ): KegiatanListWrapperResponse

    @GET("kegiatan/{id}")
    suspend fun getDetailKegiatan(
        @Path("id") id: String
    ): KegiatanWrapperResponse

    @POST("kegiatan")
    suspend fun createKegiatan(
        @Body request: CreateKegiatanRequest
    ): KegiatanWrapperResponse

    @PUT("kegiatan/{id}")
    suspend fun updateKegiatan(
        @Path("id") id: String,
        @Body request: UpdateKegiatanRequest
    ): KegiatanWrapperResponse

    @DELETE("kegiatan/{id}")
    suspend fun deleteKegiatan(
        @Path("id") id: String
    ): GeneralApiResponse

    @POST("kegiatan-alat")
    suspend fun addToolToKegiatan(
        @Body request: AddToolToKegiatanRequest
    ): GeneralApiResponse
}