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
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val apiService: AuthApiService,
    private val userPreferences: UserPreferences,
    private val userDao: UserDao
) {
    suspend fun login(request: LoginRequest): Result<ApiResponse<com.example.perkapp.features.auth.api.AuthDataResponse>> {
        return withContext(Dispatchers.IO) {
            // 1. Coba login secara online jika internet tersedia
            if (com.example.perkapp.core.utils.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
                try {
                    val response = apiService.login(request)
                    if (response.success && response.data != null) {
                        val authData = response.data
                        val token = authData.token
                        if (!token.isNullOrBlank()) {
                            userPreferences.saveAuthToken(token)
                        }
                        val userDto = authData.user
                        if (userDto != null) {
                            val userEntity = UserEntity(
                                id = userDto.id,
                                name = userDto.name,
                                email = userDto.email,
                                password = request.password, // Simpan password agar bisa login offline nanti
                                role = userDto.role,
                                created_at = userDto.created_at
                            )
                            userDao.insertUser(userEntity)
                        }
                        return@withContext Result.success(response)
                    } else {
                        return@withContext Result.failure(Exception(response.message))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Jika login gagal karena kredensial salah (HttpException 401/422 dll), kembalikan error langsung
                    if (e is retrofit2.HttpException && (e.code() == 401 || e.code() == 422)) {
                        return@withContext Result.failure(e)
                    }
                    // Jika error jaringan, biarkan jatuh ke fallback offline
                }
            }

            // 2. Fallback offline: Cek dari Room Database lokal
            try {
                val user = userDao.loginUser(request.email, request.password)
                if (user == null) {
                    throw Exception("Email atau password salah / belum terdaftar secara offline di HP ini.")
                }
                
                // Simpan token dummy agar bypass login
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
            // 1. Coba register secara online jika internet tersedia
            if (com.example.perkapp.core.utils.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
                try {
                    val response = apiService.register(request)
                    if (response.success) {
                        val authData = response.data
                        val token = authData?.token
                        if (!token.isNullOrBlank()) {
                            userPreferences.saveAuthToken(token)
                        }
                        val userDto = authData?.user
                        if (userDto != null) {
                            val userEntity = UserEntity(
                                id = userDto.id,
                                name = userDto.name,
                                email = userDto.email,
                                password = request.password,
                                role = userDto.role,
                                created_at = userDto.created_at
                            )
                            userDao.insertUser(userEntity)
                        }
                        return@withContext Result.success(response)
                    } else {
                        return@withContext Result.failure(Exception(response.message))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (e is retrofit2.HttpException && (e.code() == 401 || e.code() == 422 || e.code() == 409)) {
                        return@withContext Result.failure(e)
                    }
                    // Jika error jaringan, biarkan jatuh ke fallback offline
                }
            }

            // 2. Fallback offline: Simpan data register ke Room Database
            try {
                val newUser = UserEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = request.name,
                    email = request.email,
                    password = request.password,
                    role = request.role,
                    created_at = "2026-06-04"
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

    val authTokenFlow = userPreferences.getAuthToken

    fun getCurrentUser() = userDao.getUser()

    suspend fun refreshProfileIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                val token = userPreferences.getAuthToken.first()
                if (!token.isNullOrBlank()) {
                    val localUser = userDao.getUser().first()
                    
                    // Jika online, coba perbarui data profil dari server
                    if (com.example.perkapp.core.utils.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
                        try {
                            val response = apiService.getProfile()
                            if (response.success && response.data != null) {
                                val dto = response.data
                                val newUser = UserEntity(
                                    id = dto.id,
                                    name = dto.name,
                                    email = dto.email,
                                    password = localUser?.password ?: "", // Pertahankan password lokal jika ada
                                    role = dto.role,
                                    created_at = dto.created_at
                                )
                                userDao.insertUser(newUser)
                                return@withContext
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Jika token invalid (401 Unauthorized), hapus sesi & arahkan ke login
                            if (e is retrofit2.HttpException && e.code() == 401) {
                                logout()
                                return@withContext
                            }
                        }
                    }
                    
                    // Jika offline atau API gagal dan tidak ada data lokal, buat fallback mock
                    if (localUser == null) {
                        val mockUser = UserEntity(
                            id = "default_id",
                            name = "User Perkapp",
                            email = "user@perkapp.com",
                            password = "",
                            role = "member",
                            created_at = "2026-06-04"
                        )
                        userDao.insertUser(mockUser)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
