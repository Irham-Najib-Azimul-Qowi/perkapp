package com.example.perkapp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property — Cara praktis (shortcut) agar seluruh aplikasi bisa 
 * memanggil DataStore langsung dari `Context` aplikasi (seperti `context.dataStore`).
 *
 * DataStore adalah pengganti SharedPreferences. Tempat yang cocok untuk menyimpan
 * data-data kecil (seperti setelan bahasa, tema, atau token login).
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "perkapp_prefs")

/**
 * UserPreferences — Kelas khusus untuk membaca dan menyimpan data kecil milik pengguna.
 *
 * Di proyek ini, fungsinya khusus untuk menyimpan "Kunci Akses" (Auth Token)
 * yang didapat dari server saat kita berhasil Login.
 */
class UserPreferences(private val dataStore: DataStore<Preferences>) {
    // Definisi nama "kunci" yang akan dicari di DataStore
    private val TOKEN_KEY = stringPreferencesKey("auth_token")

    /**
     * Membaca Token.
     * Menggunakan Flow agar aplikasinya "langsung bereaksi" kalau token ini 
     * tiba-tiba berubah (misal otomatis pindah ke halaman Login kalau tokennya dihapus).
     */
    val getAuthToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    /**
     * Menyimpan Token secara permanen ke memori HP.
     * Biasanya dipanggil tepat setelah server membalas dengan status "Login Sukses".
     */
    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    /**
     * Menghapus Token.
     * Dipanggil saat pengguna klik tombol "Logout".
     * Begitu dihapus, aplikasi tidak akan bisa mengakses data dari server.
     */
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
}