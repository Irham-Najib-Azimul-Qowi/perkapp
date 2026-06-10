package com.example.perkapp.network

import com.example.perkapp.network.ApiResponse
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
    
    /**
     * FUNGSI: login
     * TUJUAN: Menembak rute `/auth/login` (POST) ke server untuk mencocokkan email dan password.
     * @param request Bungkusan data (DTO) berisi email dan password.
     * @return Pembungkus standar `ApiResponse` berisi data kredensial (`AuthDataResponse`).
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthDataResponse>

    /**
     * FUNGSI: register
     * TUJUAN: Mengirim data pengguna baru ke rute `/auth/register` (POST) agar dicatat di database server.
     * @param request Bungkusan data pendaftaran (nama, email, password, role).
     * @return Respon sukses berserta token otentikasi.
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthDataResponse>

    /**
     * FUNGSI: getProfile
     * TUJUAN: Mengambil data diri "Saya" (user yang sedang login) dari rute `/auth/me` (GET).
     * Membutuhkan Token (Bearer) di Header (yang diurus oleh AuthInterceptor).
     * @return Data profil pengguna dalam bentuk DTO.
     */
    @GET("auth/me")
    suspend fun getProfile(): ApiResponse<UserDto>

    /**
     * FUNGSI: getAllUsers
     * TUJUAN: Menarik daftar seluruh pengguna yang terdaftar di sistem dari rute `/users` (GET).
     * Biasa dipakai untuk mengisi dropdown pilihan nama peminjam saat membuat form kegiatan.
     * @return List/Daftar dari sekumpulan data profil (UserDto).
     */
    @GET("users")
    suspend fun getAllUsers(): ApiResponse<List<UserDto>>
}
