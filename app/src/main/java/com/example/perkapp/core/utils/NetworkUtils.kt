package com.example.perkapp.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * NetworkUtils — Alat bantu untuk mengecek status jaringan (Internet).
 *
 * Sangat penting untuk aplikasi offline-first agar tahu kapan harus menyimpan
 * data ke database lokal (saat offline) dan kapan harus upload ke server (saat online).
 */
object NetworkUtils {

    /**
     * Fungsi pengecekan tambahan (ping): 
     * Terkadang HP tersambung ke Wi-Fi tapi Wi-Fi-nya tidak ada internet.
     * Fungsi ini mencoba mengetuk pintu server lokal untuk memastikan.
     */
    private fun isServerReachable(): Boolean {
        var reachable = false
        val thread = Thread {
            try {
                // Mencoba menyambung ke server
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("127.0.0.1", 8000), 1000)
                socket.close() // Sukses nyambung
                reachable = true
                android.util.Log.d("NetworkUtils", "isServerReachable: true")
            } catch (e: Exception) {
                // Gagal nyambung (server mati / tidak ada internet)
                android.util.Log.d("NetworkUtils", "isServerReachable failed: ${e.message}")
                reachable = false
            }
        }
        thread.start()
        try {
            thread.join(1200) // Tunggu maksimal 1,2 detik
        } catch (e: Exception) {
            android.util.Log.e("NetworkUtils", "Thread join interrupted", e)
        }
        android.util.Log.d("NetworkUtils", "isServerReachable final: $reachable")
        return reachable
    }

    /**
     * Mengecek apakah HP sedang terhubung ke internet saat fungsi ini dipanggil.
     * Mengembalikan true jika ada koneksi, false jika offline.
     */
    fun isOnline(context: Context): Boolean {
        // Cek indikator internet bawaan sistem Android (Wi-Fi / Data Seluler)
        val systemOnline = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            if (network != null) {
                val capabilities = cm.getNetworkCapabilities(network)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                false // Mode pesawat atau mati data
            }
        } catch (e: Exception) {
            false
        }

        android.util.Log.d("NetworkUtils", "isOnline systemOnline: $systemOnline")
        if (systemOnline) return true
        
        // Jika sistem bilang offline, kita pastikan lagi dengan isServerReachable
        val reachable = isServerReachable()
        android.util.Log.d("NetworkUtils", "isOnline final: $reachable")
        return reachable
    }

    /**
     * Membuat 'Flow' (aliran data) yang mengawasi perubahan sinyal internet.
     * 
     * Berbeda dengan isOnline yang hanya mengecek SEKALI, observeNetworkStatus 
     * akan diam-diam memantau. Jika tadinya tidak ada sinyal lalu tiba-tiba ada, 
     * dia akan otomatis memberi tahu aplikasi ("Eh, internetnya udah nyala!").
     *
     * @return Flow<Boolean> (true = online, false = offline)
     */
    fun observeNetworkStatus(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Membuat pendengar (callback) yang dipanggil otomatis oleh sistem Android 
        // saat terjadi perubahan status jaringan
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isOnline(context)) // Internet masuk
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(isOnline(context)) // Jenis internet berubah (misal pindah Wi-Fi ke 4G)
            }

            override fun onLost(network: Network) {
                trySend(false) // Sinyal hilang
            }

            override fun onUnavailable() {
                trySend(false) // Tidak dapat sinyal
            }
        }

        // Mendaftarkan pendengar ke sistem
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(callback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
        }

        // Kirim status internet saat ini juga (sebagai nilai awal)
        trySend(isOnline(context))

        // Membersihkan pendengar jika Flow berhenti diamati
        awaitClose {
            cm.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged() // Jangan kirim pemberitahuan berulang jika statusnya sama (true ke true)
}
