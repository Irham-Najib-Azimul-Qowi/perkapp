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

    /**
     * FUNGSI: onCreate
     * 
     * TUJUAN:
     * Ini adalah metode yang paling pertama dieksekusi di seluruh sistem aplikasi. 
     * Berjalan hanya sekali selama aplikasi hidup. Fungsi utamanya di sini adalah 
     * mendaftarkan 'instance' aplikasi agar mudah diakses secara global.
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. `super.onCreate()` wajib dipanggil agar sistem Android dapat menginisialisasi 
     *    inti aplikasi dan *dependency injection* dari Hilt.
     * 2. `instance = this` menugaskan variabel global dengan objek aplikasi saat ini. 
     *    Ini sangat berguna di kelas-kelas yang tidak bisa disuntik *Context* (seperti `RetrofitClient`), 
     *    sehingga mereka tetap bisa membaca DataStore atau utilitas lainnya.
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.perkapp.util.SecurityUtils.init(this)
    }
}
