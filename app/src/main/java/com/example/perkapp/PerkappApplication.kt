package com.example.perkapp

// Mengimpor kelas Application bawaan Android
import android.app.Application
// Mengimpor anotasi HiltAndroidApp untuk inisialisasi Hilt
import dagger.hilt.android.HiltAndroidApp

/**
 * PerkappApplication — Titik awal (Entry Point) berjalannya aplikasi.
 *
 * Kelas ini adalah yang pertama kali dijalankan oleh sistem Android bahkan
 * sebelum Activity/Layar pertama muncul.
 *
 * Anotasi @HiltAndroidApp menandakan bahwa kita menggunakan Hilt 
 * (Dependency Injection) di proyek ini. Hilt akan membangun pondasi di sini 
 * agar komponen seperti ViewModel bisa dibuat secara otomatis.
 */
@HiltAndroidApp
class PerkappApplication : Application() {
    
    companion object {
        // Menyimpan instance global dari aplikasi agar bisa diakses dari mana saja
        // tanpa harus mengirim Context terus-menerus (misal di dalam RetrofitClient).
        lateinit var instance: PerkappApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Menyimpan referensi aplikasi ke dalam instance saat aplikasi pertama kali dibuka
        instance = this
    }
}
