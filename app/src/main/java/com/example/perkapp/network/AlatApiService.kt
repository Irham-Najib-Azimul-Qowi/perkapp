package com.example.perkapp.network

import com.example.perkapp.network.ApiResponse
import com.example.perkapp.model.AlatResponse
import com.example.perkapp.model.CreateAlatRequest
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

    /**
     * FUNGSI: getAllAlat
     * TUJUAN: Menembak rute `/alat` (GET) untuk mengunduh seluruh daftar barang inventaris 
     * dari server secara bersamaan.
     * @return Daftar alat (`List<AlatResponse>`) berbalut kotak `ApiResponse` dan dibungkus 
     *         oleh `Response` (agar bisa baca kode HTTP 200).
     */
    @GET("alat")
    suspend fun getAllAlat(): Response<ApiResponse<List<AlatResponse>>>

    /**
     * FUNGSI: getAlatById
     * TUJUAN: Mengambil rincian lebih dalam untuk 1 buah barang spesifik menggunakan rute `/alat/{id}` (GET).
     * @param id String identitas unik barang.
     */
    @GET("alat/{id}")
    suspend fun getAlatById(
        @Path("id") id: String
    ): Response<ApiResponse<AlatResponse>>

    /**
     * FUNGSI: createAlat
     * TUJUAN: Menambahkan/mendaftarkan alat baru ke server melalui rute `/alat` (POST).
     * @param request Data form barang yang diinput pengguna.
     */
    @POST("alat")
    suspend fun createAlat(
        @Body request: CreateAlatRequest
    ): Response<ApiResponse<AlatResponse>>

    /**
     * FUNGSI: updateAlat
     * TUJUAN: Mengubah (Edit) informasi alat yang sudah ada di rute `/alat/{id}` (PUT).
     * @param id ID barang yang ingin diedit.
     * @param request Data form barang terbaru (hasil editan).
     */
    @PUT("alat/{id}")
    suspend fun updateAlat(
        @Path("id") id: String,
        @Body request: CreateAlatRequest
    ): Response<ApiResponse<AlatResponse>>

    /**
     * FUNGSI: deleteAlat
     * TUJUAN: Menghapus sebuah alat dari database server melalui rute `/alat/{id}` (DELETE).
     * @param id ID barang yang akan dimusnahkan.
     */
    @DELETE("alat/{id}")
    suspend fun deleteAlat(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>> // 'Unit' berarti server tidak membalas data apa-apa selain status 200
}
