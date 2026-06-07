package com.example.perkapp

// Mengimpor kelas Application bawaan Android
import android.app.Application
// Mengimpor anotasi HiltAndroidApp untuk inisialisasi Hilt
import dagger.hilt.android.HiltAndroidApp

/**
 * Kelas PerkappApplication adalah kelas utama Aplikasi.
 * Anotasi @HiltAndroidApp menandakan bahwa kelas ini memicu pembuatan kode oleh Hilt,
 * termasuk kelas dasar untuk aplikasi yang berfungsi sebagai kontainer dependensi tingkat atas.
 */
@HiltAndroidApp
class PerkappApplication : Application() {
    
    companion object {
        lateinit var instance: PerkappApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
