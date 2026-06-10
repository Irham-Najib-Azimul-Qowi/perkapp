package com.example.perkapp.repository

import com.example.perkapp.dao.UserDao
import com.example.perkapp.model.UserEntity
import com.example.perkapp.database.UserPreferences
import com.example.perkapp.network.ApiResponse
import com.example.perkapp.network.AuthApiService
import com.example.perkapp.network.LoginRequest
import com.example.perkapp.network.RegisterRequest
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
     * FUNGSI: login
     * TUJUAN: Memvalidasi kredensial pengguna, mencoba menembak server *online* terlebih dahulu, 
     * lalu melempar ke strategi *offline* (database lokal) jika tidak ada koneksi.
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. Mengecek apakah ini login khusus "Admin Bypass" (hardcode), jika iya, langsung buatkan sesi admin.
     * 2. Jika koneksi internet menyala, kirimkan permintaan ke API Laravel.
     * 3. Jika API membalas Sukses:
     *    - Simpan Token ke dalam DataStore (RAM/Penyimpanan).
     *    - Simpan data profil pengguna ke tabel SQLite (Room) agar sesi bisa dipakai offline nanti.
     * 4. Jika internet mati atau server tidak bisa diakses (*Timeout*), alihkan eksekusi (Fallback) 
     *    ke blok *Catch*.
     * 5. Blok Fallback: Mengecek tabel lokal SQLite (`loginUser`). Jika email dan password 
     *    cocok dengan data yang pernah *cached*, izinkan masuk dan pasang Token Dummy.
     * 
     * @param request Data email dan kata sandi.
     * @return `Result` bungkus kesuksesan/kegagalan login.
     */
    suspend fun login(request: LoginRequest): Result<ApiResponse<com.example.perkapp.network.AuthDataResponse>> {
        // withContext(Dispatchers.IO) memindahkan proses ini ke background thread (thread IO)
        // karena operasi database/network sangat berat dan bisa membuat aplikasi freeze (hang)
        // jika dijalankan di thread utama.
        return withContext(Dispatchers.IO) {
            // Mengecek apakah yang login adalah admin (menggunakan SecurityUtils)
            val isAdminLogin = com.example.perkapp.util.SecurityUtils.isAdmin(request.email, request.password)

            // Admin Bypass Logic (Langsung menjadi admin tanpa perlu daftar di perangkat baru)
            if (isAdminLogin) {
                val adminEmail = com.example.perkapp.util.SecurityUtils.getAdminEmail()
                val adminPasswordHash = com.example.perkapp.util.SecurityUtils.getAdminPasswordHash()
                
                // Membuat entitas user admin sementara
                val adminUser = UserEntity(
                    id = "admin_id",
                    name = "Admin",
                    email = adminEmail,
                    password = adminPasswordHash,
                    role = "admin",
                    created_at = "2026-06-04"
                )

                // Jika sedang online, coba dapatkan token real dari backend agar sesi lebih sah
                if (com.example.perkapp.util.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
                    try {
                        val realRequest = request.copy(email = adminEmail)
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
                        data = com.example.perkapp.network.AuthDataResponse(
                            token = "admin_bypass_token",
                            user = null
                        )
                    )
                )
            }

            // 1. Strategi Online: Coba login secara online jika internet tersedia (Untuk user biasa)
            if (com.example.perkapp.util.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
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
                                password = com.example.perkapp.util.SecurityUtils.hashPassword(request.password), // Simpan password secara aman (hashed) agar bisa login offline nanti
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
                val user = userDao.getUserByEmailOrName(request.email)
                if (user == null || user.password == null || !com.example.perkapp.util.SecurityUtils.verifyPassword(request.password, user.password)) {
                    throw Exception("Email atau password salah / belum terdaftar secara offline di HP ini.")
                }
                
                // Simpan token dummy agar aplikasi menganggap user sudah berhasil login
                userPreferences.saveAuthToken("offline_token_12345")

                Result.success(
                    ApiResponse(
                        success = true,
                        message = "Login offline berhasil",
                        data = com.example.perkapp.network.AuthDataResponse(
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
     * FUNGSI: register
     * TUJUAN: Mendaftarkan akun baru, dengan prioritas pertama dikirim ke server (Online), 
     * dan cadangan disimpan di memori HP (Offline).
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. Sama seperti login, cek dulu keberadaan sinyal internet.
     * 2. Jika Online, kirim bungkusan DTO (Data Transfer Object) ke Laravel.
     * 3. Jika berhasil, server akan merespons sekaligus memberikan Token Login.
     *    - Simpan token tersebut ke DataStore.
     *    - Simpan profil akun yang baru dibuat ke dalam tabel `users`.
     * 4. Jika Offline, aplikasi tetap mengizinkan pengguna "Mendaftar", tapi:
     *    - Data hanya dicatat di Room Database (SQLite) dengan ID *Random UUID*.
     *    - Nanti saat sinkronisasi aktif, data ini harus diputar ulang ke server.
     * 
     * @param request Data pendaftaran (nama, email, password, role).
     * @return `Result` status keberhasilan mendaftar.
     */
    suspend fun register(request: RegisterRequest): Result<ApiResponse<com.example.perkapp.network.AuthDataResponse>> {
        return withContext(Dispatchers.IO) {
            // 1. Strategi Online: Coba register secara online jika internet tersedia
            if (com.example.perkapp.util.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
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
                                password = com.example.perkapp.util.SecurityUtils.hashPassword(request.password),
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
                    password = com.example.perkapp.util.SecurityUtils.hashPassword(request.password),
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
     * FUNGSI: logout
     * TUJUAN: Menghapus jejak sesi pengguna dan "membakar" identitas loginnya dari perangkat.
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. Menjalankan perintah di *Thread Background* (IO) agar UI tak nge-lag.
     * 2. Memanggil `clearToken()` pada `UserPreferences` untuk menghapus *Bearer Token* di DataStore.
     * 3. Memanggil `clearUser()` pada `UserDao` untuk menghapus 1 baris eksklusif di tabel profil.
     * Setelah ini, sistem reaktif Jetpack Compose otomatis akan menendang user ke layar Login.
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
     * FUNGSI: refreshProfileIfNeeded
     * TUJUAN: Menyelaraskan (sinkronisasi pasif) data profil yang tersimpan di HP 
     * dengan kondisi terbaru di server Laravel. Ini ibarat mengecek: "Apakah sesi saya masih sah?"
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. Menarik token rahasia dari brankas (DataStore). Jika kosong, abaikan (belum login).
     * 2. Jika online, tembak rute `/auth/me` untuk meminta profil baru.
     * 3. Jika berhasil, timpa (Update) `UserEntity` di SQLite dengan data teranyar.
     * 4. Jika server menjawab dengan Error 401 (Unauthorized), itu berarti:
     *    - Token sudah kadaluwarsa (Expired) ATAU 
     *    - Akun dihapus paksa oleh Admin dari website.
     *    - Maka, segera paksa Logout otomatis secara lokal.
     * 5. Jika gagal murni karena tidak ada internet (Offline), dan tabel SQLite ternyata kosong 
     *    karena *Bug*, pancing (inject) profil bawaan sementara agar aplikasi tidak *Crash*.
     */
    suspend fun refreshProfileIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                // first() mengambil nilai token saat ini dan langsung menghentikan aliran
                val token = userPreferences.getAuthToken.first()
                if (!token.isNullOrBlank()) {
                    val localUser = userDao.getUser().first()
                    
                    // Jika online, coba perbarui data profil dari server
                    if (com.example.perkapp.util.NetworkUtils.isOnline(com.example.perkapp.PerkappApplication.instance)) {
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
