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

/**
 * AlatApiService — Antarmuka rute API untuk mengelola Inventaris Alat.
 *
 * Menggunakan class Retrofit `Response` sebagai tipe kembalian agar kita 
 * bisa mengecek HTTP status code (misal: 200 Sukses, 404 Tidak Ditemukan)
 * secara manual di Repository.
 */
interface AlatApiService {

    // Mengambil seluruh data alat dari server
    @GET("alat")
    suspend fun getAllAlat(): Response<ApiResponse<List<AlatResponse>>>

    // Mengambil rincian hanya 1 alat spesifik
    @GET("alat/{id}")
    suspend fun getAlatById(
        @Path("id") id: String // Mengganti teks "{id}" dengan ID alat yang dikirim
    ): Response<ApiResponse<AlatResponse>>

    // Menambahkan/mendaftarkan alat baru ke server
    @POST("alat")
    suspend fun createAlat(
        @Body request: CreateAlatRequest
    ): Response<ApiResponse<AlatResponse>>

    // Memperbarui informasi alat yang sudah ada
    @PUT("alat/{id}")
    suspend fun updateAlat(
        @Path("id") id: String,
        @Body request: CreateAlatRequest
    ): Response<ApiResponse<AlatResponse>>

    // Menghapus sebuah alat dari sistem server
    @DELETE("alat/{id}")
    suspend fun deleteAlat(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>> // 'Unit' berarti server tidak membalas apa-apa
}