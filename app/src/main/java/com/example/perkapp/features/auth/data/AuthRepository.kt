package com.example.perkapp.features.auth.data

import com.example.perkapp.core.database.dao.UserDao
import com.example.perkapp.core.database.entity.UserEntity
import com.example.perkapp.core.datastore.UserPreferences
import com.example.perkapp.core.network.ApiResponse
import com.example.perkapp.features.auth.api.AuthApiService
import com.example.perkapp.features.auth.api.LoginRequest
import com.example.perkapp.features.auth.api.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiService: AuthApiService,
    private val userPreferences: UserPreferences,
    private val userDao: UserDao
) {
    suspend fun login(request: LoginRequest): Result<ApiResponse<com.example.perkapp.features.auth.api.AuthDataResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                // Di sini nanti panggil API asli
                // val response = apiService.login(request)
                // return@withContext Result.success(response)

                // OFFLINE FIRST LOGIC (Mock): Cek dari Room Database lokal dulu
                kotlinx.coroutines.delay(1000)
                
                val user = userDao.loginUser(request.email, request.password)
                
                if (user == null) {
                    throw Exception("Email atau password salah / belum terdaftar di HP ini.")
                }
                
                // Simpan token ke DataStore sebagai tanda sudah login
                userPreferences.saveAuthToken("offline_token_12345")

                Result.success(
                    ApiResponse(
                        success = true,
                        message = "Login offline berhasil",
                        data = com.example.perkapp.features.auth.api.AuthDataResponse(
                            token = "offline_token_12345",
                            user = null
                        )
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun register(request: RegisterRequest): Result<ApiResponse<com.example.perkapp.features.auth.api.AuthDataResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                // val response = apiService.register(request)
                // return@withContext Result.success(response)

                // OFFLINE FIRST LOGIC (Mock): Simpan data register ke Room Database
                kotlinx.coroutines.delay(1000)
                
                val newUser = UserEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = request.name,
                    email = request.email,
                    password = request.password,
                    role = "member",
                    created_at = "2026-06-04" // Hardcode untuk mock
                )
                
                userDao.insertUser(newUser)

                Result.success(
                    ApiResponse(
                        success = true,
                        message = "Pendaftaran offline berhasil tersimpan di perangkat",
                        data = null
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            userPreferences.clearToken()
            userDao.clearUser()
        }
    }

    fun getCurrentUser() = userDao.getUser()
}
