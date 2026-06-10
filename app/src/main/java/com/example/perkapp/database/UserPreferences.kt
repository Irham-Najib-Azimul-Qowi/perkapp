package com.example.perkapp.database

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
     * FUNGSI/PROPERTI: getAuthToken
     * TUJUAN: Membaca Token. Menggunakan Flow agar aplikasinya "langsung bereaksi" 
     * kalau token ini tiba-tiba berubah (misal otomatis pindah ke halaman Login kalau tokennya terhapus).
     * @return Flow dari token String, atau null jika belum ada token.
     */
    val getAuthToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    /**
     * FUNGSI: saveAuthToken
     * TUJUAN: Menyimpan Token secara permanen ke memori HP menggunakan fitur enkripsi 
     * DataStore. Biasanya dipanggil tepat setelah server merespons "Login Sukses" 
     * dan memberikan string panjang (JWT token) yang menjadi tiket masuk di request berikutnya.
     * @param token String kunci rahasia dari server.
     */
    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    /**
     * FUNGSI: clearToken
     * TUJUAN: Menghapus Token. Dipanggil saat pengguna mengeklik tombol "Logout".
     * Begitu dihapus, aplikasi memotong tali akses ke server, membuat sesi 
     * habis secara total pada sisi *client*.
     */
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
}
