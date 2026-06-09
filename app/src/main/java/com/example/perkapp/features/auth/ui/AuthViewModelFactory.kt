/**
 * File: AuthViewModelFactory.kt
 *
 * FUNGSI UTAMA:
 * Kelas pabrik (Factory) kustom untuk membuat instance AuthViewModel dengan parameter dependensi (AuthRepository).
 *
 * PENJELASAN MENDALAM:
 * ViewModel di Android biasanya tidak boleh memiliki parameter di constructor-nya karena
 * siklus hidupnya dikelola oleh sistem (ViewModelProvider).
 * Namun, karena AuthViewModel membutuhkan AuthRepository, kita harus membuat Factory kustom
 * agar sistem tahu *cara* membuat ViewModel tersebut dengan memberikan repository ke dalamnya.
 */
package com.example.perkapp.features.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.perkapp.features.auth.data.AuthRepository

/**
 * AuthViewModelFactory — Factory yang bertanggung jawab menyuntikkan AuthRepository
 * ke dalam AuthViewModel saat instansiasi.
 */

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
