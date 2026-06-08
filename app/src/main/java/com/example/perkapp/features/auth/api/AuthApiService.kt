package com.example.perkapp.features.auth.api

import com.example.perkapp.core.network.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Data Transfer Object (DTO) untuk format paket data yang DIKIRIM ke server
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val role: String = "member")

// Disesuaikan dengan dokumentasi API Laravel (Paket data yang DITERIMA dari server)
data class AuthDataResponse(
    val token: String?,
    val user: UserDto?
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val created_at: String
)

/**
 * AuthApiService — Antarmuka (Interface) untuk rute API fitur Autentikasi.
 *
 * Retrofit akan membaca anotasinya (@POST, @GET) lalu membuatkan
 * kode implementasinya secara otomatis di balik layar.
 */
interface AuthApiService {
    
    // Rute untuk Login
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthDataResponse>

    // Rute untuk Mendaftar akun baru
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthDataResponse>

    // Rute untuk mengambil profil saya (akun yang sedang dipakai)
    @GET("auth/me")
    suspend fun getProfile(): ApiResponse<UserDto>

    // Rute untuk melihat seluruh pengguna yang terdaftar di aplikasi
    @GET("users")
    suspend fun getAllUsers(): ApiResponse<List<UserDto>>
}
