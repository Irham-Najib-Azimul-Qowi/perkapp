package com.example.perkapp.features.media.api

import com.example.perkapp.core.network.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// Respons balasan server saat berhasil upload foto (berupa URL tempat fotonya tersimpan)
data class ImageUploadResponse(
    val image_url: String
)

/**
 * MediaApiService — Rute API khusus untuk mengunggah berkas/file fisik.
 */
interface MediaApiService {
    
    // @Multipart menandakan format pengiriman file (berbeda dengan JSON biasa)
    // Tipe ini memungkinkan pengiriman file foto beserta data teks secara bersamaan
    @Multipart
    @POST("upload-image")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,           // File fisiknya (gambar)
        @Part("entity_type") entityType: RequestBody, // Tipe pemilik foto (misal: "alat")
        @Part("entity_id") entityId: RequestBody      // ID dari si pemilik foto tersebut
    ): Response<ApiResponse<ImageUploadResponse>>
}