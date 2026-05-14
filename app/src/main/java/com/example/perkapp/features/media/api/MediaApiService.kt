package com.example.perkapp.features.media.api

import com.example.perkapp.core.network.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ImageUploadResponse(
    val image_url: String
)
interface MediaApiService {
    @Multipart
    @POST("upload-image")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("entity_type") entityType: RequestBody,
        @Part("entity_id") entityId: RequestBody
    ): Response<ApiResponse<ImageUploadResponse>>
}