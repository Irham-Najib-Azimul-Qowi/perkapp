package com.example.perkapp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * NetworkMonitor — Kelas sederhana pendeteksi sinyal internet.
 *
 * Hampir mirip dengan NetworkUtils, namun ini adalah versi objek 
 * sederhana yang bisa di-inject via Dependency Injection.
 */
class NetworkMonitor(private val context: Context) {
    
    /**
     * FUNGSI: isConnected
     * TUJUAN: Menjadi alat bantu diagnostik jaringan untuk mengetahui apakah ponsel 
     * pengguna saat ini terhubung ke sumber internet apa pun (baik Wi-Fi maupun Kuota/Data Seluler).
     * Sering kali digunakan sebelum aplikasi memutuskan untuk menembak API (jika false, 
     * batalkan API dan tampilkan antrean offline saja).
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. Meminta `ConnectivityManager` dari sistem Android (alat bawaan HP).
     * 2. Menarik info `activeNetwork` (jaringan yang saat ini sedang aktif menyala). 
     *    Bila mode pesawat menyala, ini langsung `null` dan me-return false.
     * 3. Menganalisis `NetworkCapabilities` untuk melihat "Jalur apa" yang dipakai jaringan tersebut.
     * 4. Jika menggunakan `TRANSPORT_WIFI` atau `TRANSPORT_CELLULAR`, maka sinyal aman (true).
     * 
     * @return Boolean `true` jika internet dianggap tersedia, `false` bila sedang blank-spot.
     */
    fun isConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}
