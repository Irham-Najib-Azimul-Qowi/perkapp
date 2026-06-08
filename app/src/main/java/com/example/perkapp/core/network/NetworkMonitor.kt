package com.example.perkapp.core.network

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
    
    // Mengecek apakah ada koneksi Wi-Fi atau Kuota Data
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
