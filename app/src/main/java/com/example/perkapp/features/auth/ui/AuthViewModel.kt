package com.example.perkapp.features.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perkapp.features.auth.api.LoginRequest
import com.example.perkapp.features.auth.api.RegisterRequest
import com.example.perkapp.features.auth.data.AuthRepository
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
     * Memproses permintaan login dari user.
     *
     * Alur kerja:
     * 1. Validasi input → jika kosong, kirim state Error dan berhenti
     * 2. Ubah state ke Loading → UI menampilkan loading indicator
     * 3. Panggil repository.login() → mengirim request ke server
     * 4. Jika sukses → ubah state ke Success → UI navigasi ke Home
     * 5. Jika gagal → identifikasi jenis error → ubah state ke Error dengan pesan
     *
     * @param request — Objek berisi email dan password yang diketik user
     */
    fun login(request: LoginRequest) {
        // Validasi dilakukan SEBELUM mengirim ke server untuk menghemat bandwidth
        if (request.email.isBlank() && request.password.isBlank()) {
            _loginState.value = AuthState.Error("Email dan Password tidak boleh kosong.")
            return // Hentikan fungsi di sini, tidak perlu lanjut
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
            
            // onSuccess: dipanggil jika API merespon tanpa Exception (meski bisa jadi error dari sisi logika server)
            result.onSuccess { response ->
                if (response.success) {
                    // Server mengkonfirmasi login berhasil
                    _loginState.value = AuthState.Success
                } else {
                    // Server merespons tapi menolak login (misal: akun salah)
                    _loginState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
                // onFailure: dipanggil jika ada Exception (koneksi putus, server mati, dll)
                val errorMsg = when {
                    // Masalah koneksi internet atau server tidak bisa dijangkau
                    exception is java.net.UnknownHostException || exception is java.net.ConnectException -> {
                        "Gagal terhubung ke server. Periksa koneksi internet Anda."
                    }
                    // Server merespons tapi dengan kode status HTTP error (4xx, 5xx)
                    exception is retrofit2.HttpException -> {
                        when (exception.code()) {
                            401 -> "Email atau password salah." // Unauthorized
                            422 -> "Format email atau password tidak valid." // Unprocessable Entity
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
     * Memproses permintaan pendaftaran akun baru.
     *
     * Alurnya mirip dengan login, memvalidasi form lalu mengirimkan 
     * data register ke server melalui Repository.
     *
     * @param request — Objek berisi nama, email, dan password untuk akun baru
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
     * Mengeluarkan pengguna dari sesi saat ini.
     *
     * Menghapus token dari lokal dan memperbarui status agar kembali
     * menjadi belum login (mengarahkan ke LoginScreen).
     */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
