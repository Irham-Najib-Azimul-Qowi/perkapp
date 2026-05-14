package com.example.perkapp.features.alat.api

import com.example.perkapp.core.network.ApiResponse
import com.example.perkapp.features.alat.data.remote.AlatResponse
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.net.ResponseCache

interface AlatApiService {

    @GET("alat")
    suspend fun getAllAlat(): Response<ApiResponse<List<AlatResponse>>>

    @GET("alat/{id}")
    suspend fun getAlatById(
        @Path("id") id: String
    ): Response<ApiResponse<AlatResponse>>

    @POST("alat")
    suspend fun createAlat(
        @Body request: CreateAlatRequest
    ): Response<ApiResponse<AlatResponse>>

    @PUT("alat/{id}")
    suspend fun updateAlat(
        @Path("id") id: String,
        @Body request: CreateAlatRequest
    ): Response<ApiResponse<AlatResponse>>

    @DELETE("alat/{id}")
    suspend fun deleteAlat(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}