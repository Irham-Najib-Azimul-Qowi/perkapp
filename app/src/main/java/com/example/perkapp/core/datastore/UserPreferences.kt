package com.example.perkapp.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Simpan Token di sini agar user tidak perlu login ulang
class UserPreferences(private val dataStore: DataStore<Preferences>) {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")

    val getAuthToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
}