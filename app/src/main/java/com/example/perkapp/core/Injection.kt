package com.example.perkapp.core

import android.content.Context
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.datastore.UserPreferences
import com.example.perkapp.core.datastore.dataStore
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.features.auth.api.AuthApiService
import com.example.perkapp.features.auth.data.AuthRepository

object Injection {
    fun provideAuthRepository(context: Context): AuthRepository {
        val userPreferences = UserPreferences(context.dataStore)
        val database = AppDatabase.getDatabase(context)
        val apiService = RetrofitClient.instance.create(AuthApiService::class.java)
        
        return AuthRepository(apiService, userPreferences, database.userDao())
    }
}
