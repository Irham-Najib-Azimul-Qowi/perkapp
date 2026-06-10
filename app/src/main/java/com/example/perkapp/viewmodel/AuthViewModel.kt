package com.example.perkapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perkapp.network.LoginRequest
import com.example.perkapp.network.RegisterRequest
import com.example.perkapp.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AuthState — Menyimpan berbagai kondisi (state) dari proses autentikasi.
 *
 * Ini digunakan agar UI (layar) tahu apa yang sedang terjadi dan bisa 
 * menampilkan tampilan yang sesuai (misal: loading, sukses, atau error).
 */
sealed class AuthState {
    // Kondisi awal, belum ada aksi apapun
    object Idle : AuthState()
    // Sedang memproses ke server, UI bisa menampilkan loading spinner
    object Loading : AuthState()
    // Proses berhasil, UI bisa pindah ke halaman lain
    object Success : AuthState()
    // Proses gagal, membawa pesan error untuk ditampilkan ke user
    data class Error(val message: String) : AuthState()
}

/**
 * AuthViewModel — Mengelola semua logika autentikasi: login, register, dan logout.
 *
 * Dalam pola arsitektur MVVM (Model-View-ViewModel), kelas ini adalah bagian "ViewModel".
 * Tugasnya: menjadi perantara antara tampilan (LoginScreen, RegisterScreen) dan data (AuthRepository).
 *
 * Cara kerjanya:
 * - Menerima aksi dari user (klik tombol Login/Register) → memproses validasi → memanggil Repository
 * - Memperbarui "state" (kondisi) → tampilan otomatis bereaksi sesuai state terbaru
 *
 * ViewModel tidak mengetahui tampilan secara langsung — komunikasi hanya lewat StateFlow.
 */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    // _loginState adalah "kotak state login" yang hanya bisa diubah dari dalam ViewModel.
    // Awalan underscore (_) adalah konvensi Kotlin: menandakan ini versi private/mutable.
    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    
    // loginState adalah versi "read-only" dari _loginState yang diekspos ke Screen.
    // Screen hanya bisa membaca (tidak bisa mengubah) — ini melindungi state dari perubahan sembarangan.
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    // State untuk proses register, dipisahkan dari login agar tidak saling tumpang tindih
    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    // Mengambil data user yang sedang aktif dari database lokal (otomatis update jika ada perubahan)
    val currentUser = repository.getCurrentUser()
    
    // Mengambil token autentikasi (untuk mengecek apakah user sedang login atau tidak)
    val authToken = repository.authTokenFlow

    init {
        // viewModelScope.launch: menjalankan kode secara asynchronous (di background)
        // Saat ViewModel dibuat, otomatis cek profil ke server jika perlu di-refresh
        viewModelScope.launch {
            repository.refreshProfileIfNeeded()
        }
    }

    /**
     * FUNGSI: login
     * TUJUAN: Menangani seluruh alur kejadian ketika pengguna mengeklik tombol "Login".
     *
     * ALUR LOGIKA PENGERJAAN:
     * 1. Validasi Pra-Syarat: Mengecek apakah kolom Email dan Password kosong. 
     *    Jika iya, langsung *return Error* (menghemat kuota internet dan tenaga server).
     * 2. Persiapan UI: Mengubah status `_loginState` menjadi `Loading`. Hal ini akan 
     *    otomatis dibaca oleh `LoginScreen` untuk memunculkan animasi berputar (Spinner) 
     *    dan menonaktifkan tombol sementara waktu.
     * 3. Delegasi Tugas: Menyerahkan eksekusi berat kepada `AuthRepository.login()` 
     *    yang berjalan di *Background Thread*.
     * 4. Evaluasi Hasil (Sukses): Jika tidak ada error programatik, ia mengecek status boolean dari server.
     *    Jika `true`, maka perbarui status ke `Success` (berpindah halaman).
     *    Jika `false` (misal kata sandi salah), ubah status ke `Error`.
     * 5. Penanganan Pengecualian (*Exception*): Bila server mati atau internet putus (`UnknownHostException`),
     *    pesan error akan diubah menjadi bahasa Indonesia yang ramah pengguna.
     *
     * @param request Bungkusan `Email` dan `Password`.
     */
    fun login(request: LoginRequest) {
        // Validasi dilakukan SEBELUM mengirim ke server untuk menghemat bandwidth
        if (request.email.isBlank() && request.password.isBlank()) {
            _loginState.value = AuthState.Error("Email dan Password tidak boleh kosong.")
            return
        } else if (request.email.isBlank()) {
            _loginState.value = AuthState.Error("Email tidak boleh kosong.")
            return
        } else if (request.password.isBlank()) {
            _loginState.value = AuthState.Error("Password tidak boleh kosong.")
            return
        }
        
        // Menjalankan proses login di background thread
        viewModelScope.launch {
            // Ubah state ke Loading agar tombol dinonaktifkan dan spinner tampil
            _loginState.value = AuthState.Loading
            
            // Delegasikan proses login ke Repository
            val result = repository.login(request)
            
            // onSuccess: dipanggil jika API merespon tanpa Exception
            result.onSuccess { response ->
                if (response.success) {
                    _loginState.value = AuthState.Success
                } else {
                    _loginState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
                // onFailure: dipanggil jika ada Exception (koneksi putus, server mati, dll)
                val errorMsg = when {
                    exception is java.net.UnknownHostException || exception is java.net.ConnectException -> {
                        "Gagal terhubung ke server. Periksa koneksi internet Anda."
                    }
                    exception is retrofit2.HttpException -> {
                        when (exception.code()) {
                            401 -> "Email atau password salah."
                            422 -> "Format email atau password tidak valid."
                            else -> "Gagal melakukan login. Silakan periksa kembali akun Anda."
                        }
                    }
                    else -> {
                        exception.message ?: "Terjadi kesalahan"
                    }
                }
                _loginState.value = AuthState.Error(errorMsg)
            }
        }
    }

    /**
     * FUNGSI: register
     * TUJUAN: Menangani seluruh kejadian saat pengguna menekan tombol "Daftar".
     *
     * ALUR LOGIKA PENGERJAAN:
     * 1. Sama seperti fitur Login, jalankan validasi kelengkapan form secara luring (offline) terlebih dahulu.
     * 2. Jika lolos, kirimkan objek request ke Repository.
     * 3. Jika berhasil didaftarkan, Repository sudah merangkap fungsi "Auto Login",
     *    sehingga `_registerState` langsung bernilai `Success`.
     * 4. Jika gagal (misal email telah terpakai/HTTP 409), tangkap error dan 
     *    tampilkan pop-up / tulisan merah di atas form lewat status `Error`.
     *
     * @param request Bungkusan Data Pendaftaran (Username, Email, Password).
     */
    fun register(request: RegisterRequest) {
        // Validasi kelengkapan data
        if (request.name.isBlank() && request.email.isBlank() && request.password.isBlank()) {
            _registerState.value = AuthState.Error("Username, Email, dan Password tidak boleh kosong.")
            return
        } else if (request.name.isBlank()) {
            _registerState.value = AuthState.Error("Username tidak boleh kosong.")
            return
        } else if (request.email.isBlank()) {
            _registerState.value = AuthState.Error("Email tidak boleh kosong.")
            return
        } else if (request.password.isBlank()) {
            _registerState.value = AuthState.Error("Password tidak boleh kosong.")
            return
        }
        
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            
            // Mengirim request pendaftaran
            val result = repository.register(request)
            
            result.onSuccess { response ->
                if (response.success) {
                    _registerState.value = AuthState.Success
                } else {
                    _registerState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
                // Menangani error jaringan atau error dari server
                val errorMsg = when {
                    exception is java.net.UnknownHostException || exception is java.net.ConnectException -> {
                        "Gagal terhubung ke server. Periksa koneksi internet Anda."
                    }
                    exception is retrofit2.HttpException -> {
                        when (exception.code()) {
                            409 -> "Email sudah terdaftar." // Conflict (sudah ada)
                            422 -> "Data pendaftaran tidak valid."
                            else -> "Pendaftaran gagal. Silakan coba lagi."
                        }
                    }
                    else -> {
                        exception.message ?: "Terjadi kesalahan"
                    }
                }
                _registerState.value = AuthState.Error(errorMsg)
            }
        }
    }

    /**
     * FUNGSI: logout
     * TUJUAN: Menjadi pemicu (Trigger) dari tombol Keluar di layar Profil.
     * Mengakhiri sesi pengguna dengan cara memanggil fungsi pembersih 
     * di repositori utama. Karena menggunakan `viewModelScope.launch`,
     * ia akan langsung beraksi tanpa mem-blokir proses klik tombol.
     */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
