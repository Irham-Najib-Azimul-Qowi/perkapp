package com.example.perkapp.core

import android.content.Context
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.datastore.UserPreferences
import com.example.perkapp.core.datastore.dataStore
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.features.auth.api.AuthApiService
import com.example.perkapp.features.auth.data.AuthRepository

/**
 * Injection — Pabrik pembuat dependensi (Dependency Injection manual).
 *
 * Mengumpulkan kelas-kelas kecil (seperti API, Database, DataStore) dan
 * merakitnya menjadi satu kelas besar (Repository).
 *
 * Dengan pola ini, ViewModel tidak perlu tahu cara membuat Repository dari nol,
 * cukup minta ke Injection.
 */
object Injection {
    /**
     * Merakit dan menghasilkan AuthRepository yang sudah siap pakai.
     */
    fun provideAuthRepository(context: Context): AuthRepository {
        // 1. Siapkan tempat baca token (DataStore)
        val userPreferences = UserPreferences(context.dataStore)
        // 2. Siapkan database lokal (SQLite)
        val database = AppDatabase.getDatabase(context)
        // 3. Siapkan koneksi internet (Retrofit API)
        val apiService = RetrofitClient.getClient(userPreferences).create(AuthApiService::class.java)
        
        // 4. Gabungkan ketiganya menjadi AuthRepository
        return AuthRepository(apiService, userPreferences, database.userDao())
    }
}
