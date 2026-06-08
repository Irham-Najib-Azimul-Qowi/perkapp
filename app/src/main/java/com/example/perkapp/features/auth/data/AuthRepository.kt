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

/**
 * AuthRepository — Sumber data tunggal untuk semua operasi autentikasi.
 *
 * Repository adalah "perantara" antara ViewModel dan sumber data.
 * Dalam proyek ini, setiap Repository menggabungkan DUA sumber data:
 * - Remote (API server): diakses via Retrofit (butuh internet)
 * - Local (Room Database): diakses via DAO (selalu tersedia, bahkan offline)
 *
 * Aturan utama: ViewModel TIDAK BOLEH akses API atau DAO langsung.
 * Semua permintaan data harus melalui Repository.
 */
class AuthRepository(
    // apiService: "kurir" yang mengirim dan menerima data dari server
    private val apiService: AuthApiService,
    // userPreferences: "dompet kecil" untuk simpan token JWT secara aman
    private val userPreferences: UserPreferences,
    // userDao: pintu masuk ke tabel 'users' di database lokal
    private val userDao: UserDao
) {
    /**
     * Memproses permintaan login, mencoba server online dulu lalu fallback ke database lokal.
     *
     * @param request Data email dan password dari user
     * @return Hasil dari proses login (sukses beserta datanya atau gagal dengan error)
     */
    suspend fun login(request: LoginRequest): Result<ApiResponse<com.example.perkapp.features.auth.api.AuthDataResponse>> {
        // withContext(Dispatchers.IO) memindahkan proses ini ke background thread (thread IO)
        // karena operasi database/network sangat berat dan bisa membuat aplikasi freeze (hang)
        // jika dijalankan di thread utama.
        return withContext(Dispatchers.IO) {
            // Mengecek apakah yang login adalah admin (hardcoded untuk keperluan bypass/testing)
            val isAdminLogin = (request.email == "admin@cakramanggala.com" || request.email == "admin") && request.password == "admin123"

            // Admin Bypass Logic (Langsung menjadi admin tanpa perlu daftar di perangkat baru)
            if (isAdminLogin) {
                // Membuat entitas user admin sementara
                val adminUser = UserEntity(
                    id = "admin_id",
                    name = "Admin",
                    email = "admin@cakramanggala.com",
                    password = "admin123",
                    role = "admin",
                    created_at = "2026-06-04"
                )

                // Jika sedang online, coba dapatkan token real dari backend agar sesi lebih sah
                if (com.example.perkapp.core.utils.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
                    try {
                        val realRequest = request.copy(email = "admin@cakramanggala.com")
                        val response = apiService.login(realRequest)
                        if (response.success && response.data != null) {
                            val token = response.data.token
                            if (!token.isNullOrBlank()) {
                                userPreferences.saveAuthToken(token)
                            } else {
                                userPreferences.saveAuthToken("admin_bypass_token")
                            }
                            userDao.clearUser()
                            userDao.insertUser(adminUser)
                            return@withContext Result.success(response)
                        }
                    } catch (e: Exception) {
                        // Abaikan error API untuk admin bypass, biarkan lanjut ke mode offline
                    }
                }

                // Fallback jika offline atau API gagal: tetap izinkan admin masuk dengan token bypass
                userPreferences.saveAuthToken("admin_bypass_token")
                userDao.clearUser()
                userDao.insertUser(adminUser)
                return@withContext Result.success(
                    ApiResponse(
                        success = true,
                        message = "Login admin bypass berhasil",
                        data = com.example.perkapp.features.auth.api.AuthDataResponse(
                            token = "admin_bypass_token",
                            user = null
                        )
                    )
                )
            }

            // 1. Strategi Online: Coba login secara online jika internet tersedia (Untuk user biasa)
            if (com.example.perkapp.core.utils.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
                try {
                    // Coba request ke server
                    val response = apiService.login(request)
                    if (response.success && response.data != null) {
                        val authData = response.data
                        val token = authData.token
                        
                        // Simpan token ke DataStore agar user tetap login saat app ditutup
                        if (!token.isNullOrBlank()) {
                            userPreferences.saveAuthToken(token)
                        }
                        
                        val userDto = authData.user
                        if (userDto != null) {
                            // Konversi data dari server menjadi format database lokal (Entity)
                            val userEntity = UserEntity(
                                id = userDto.id,
                                name = userDto.name,
                                email = userDto.email,
                                password = request.password, // Simpan password agar bisa login offline nanti
                                role = userDto.role,
                                created_at = userDto.created_at
                            )
                            userDao.clearUser() // Hapus user lama jika ada
                            userDao.insertUser(userEntity) // Simpan user baru ke database
                        }
                        return@withContext Result.success(response)
                    } else {
                        // Server mengembalikan status gagal
                        return@withContext Result.failure(Exception(response.message))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Jika login gagal karena kredensial salah (HttpException 401/422 dll), kembalikan error langsung
                    if (e is retrofit2.HttpException && (e.code() == 401 || e.code() == 422)) {
                        return@withContext Result.failure(e)
                    }
                    // Jika error jaringan (server down/timeout), biarkan jatuh ke fallback offline di bawah
                }
            }

            // 2. Fallback offline: Cek dari Room Database lokal (Untuk user biasa)
            // Ini dijalankan hanya jika tidak ada internet ATAU request ke server gagal karena masalah jaringan
            try {
                // Mencocokkan email dan password dengan data di database lokal
                val user = userDao.loginUser(request.email, request.password)
                if (user == null) {
                    throw Exception("Email atau password salah / belum terdaftar secara offline di HP ini.")
                }
                
                // Simpan token dummy agar aplikasi menganggap user sudah berhasil login
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
                // Proses login offline juga gagal
                Result.failure(e)
            }
        }
    }

    /**
     * Memproses permintaan registrasi, mencoba online dulu lalu fallback offline.
     *
     * @param request Data pendaftaran (nama, email, password)
     * @return Hasil dari proses pendaftaran
     */
    suspend fun register(request: RegisterRequest): Result<ApiResponse<com.example.perkapp.features.auth.api.AuthDataResponse>> {
        return withContext(Dispatchers.IO) {
            // 1. Strategi Online: Coba register secara online jika internet tersedia
            if (com.example.perkapp.core.utils.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
                try {
                    // Coba request ke server
                    val response = apiService.register(request)
                    if (response.success) {
                        val authData = response.data
                        val token = authData?.token
                        
                        // Simpan token jika pendaftaran langsung login
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
                    // Menangkap error khusus dari server seperti email sudah dipakai (409)
                    if (e is retrofit2.HttpException && (e.code() == 401 || e.code() == 422 || e.code() == 409)) {
                        return@withContext Result.failure(e)
                    }
                    // Jika error jaringan, biarkan jatuh ke fallback offline
                }
            }

            // 2. Fallback offline: Simpan data register ke Room Database
            // Dijalankan jika sedang offline, akun akan tersimpan di HP ini saja sementara waktu
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

    /**
     * Menghapus semua sesi dan data pengguna lokal untuk proses logout.
     */
    suspend fun logout() {
        // Operasi database harus di thread IO
        withContext(Dispatchers.IO) {
            userPreferences.clearToken()
            userDao.clearUser()
        }
    }

    // Mengambil aliran data token (Flow) secara reaktif dari DataStore
    val authTokenFlow = userPreferences.getAuthToken

    // Mengambil aliran data pengguna (Flow) secara reaktif dari database lokal
    fun getCurrentUser() = userDao.getUser()

    /**
     * Sinkronisasi data profil pengguna dengan server jika token masih valid.
     * Dipanggil otomatis saat ViewModel inisialisasi untuk memastikan data lokal tidak usang.
     */
    suspend fun refreshProfileIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                // first() mengambil nilai token saat ini dan langsung menghentikan aliran
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
                            // Jika token invalid (401 Unauthorized), artinya sesi berakhir dari server
                            // Hapus sesi lokal dan biarkan UI mengarahkan kembali ke halaman login
                            if (e is retrofit2.HttpException && e.code() == 401) {
                                logout()
                                return@withContext
                            }
                        }
                    }
                    
                    // Jika offline atau API gagal dan tidak ada data lokal, buat fallback mock user sementara
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
