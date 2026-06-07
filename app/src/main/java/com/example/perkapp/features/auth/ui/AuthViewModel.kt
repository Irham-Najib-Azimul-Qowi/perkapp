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

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    val currentUser = repository.getCurrentUser()
    val authToken = repository.authTokenFlow

    init {
        viewModelScope.launch {
            repository.refreshProfileIfNeeded()
        }
    }

    fun login(request: LoginRequest) {
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
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            val result = repository.login(request)
            result.onSuccess { response ->
                if (response.success) {
                    _loginState.value = AuthState.Success
                } else {
                    _loginState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
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

    fun register(request: RegisterRequest) {
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
            val result = repository.register(request)
            result.onSuccess { response ->
                if (response.success) {
                    _registerState.value = AuthState.Success
                } else {
                    _registerState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
                val errorMsg = when {
                    exception is java.net.UnknownHostException || exception is java.net.ConnectException -> {
                        "Gagal terhubung ke server. Periksa koneksi internet Anda."
                    }
                    exception is retrofit2.HttpException -> {
                        when (exception.code()) {
                            409 -> "Email sudah terdaftar."
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

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
