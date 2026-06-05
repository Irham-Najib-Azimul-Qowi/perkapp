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

    fun login(request: LoginRequest) {
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
                _loginState.value = AuthState.Error(exception.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun register(request: RegisterRequest) {
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
                _registerState.value = AuthState.Error(exception.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
