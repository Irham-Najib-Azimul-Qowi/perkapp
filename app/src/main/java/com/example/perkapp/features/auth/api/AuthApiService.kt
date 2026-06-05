package com.example.perkapp.features.auth.api

import com.example.perkapp.core.network.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val role: String = "member")

// Disesuaikan dengan DokumentasiLengkap.md
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

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthDataResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthDataResponse>

    @GET("auth/me")
    suspend fun getProfile(): ApiResponse<UserDto>
}
