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

object NetworkUtils {

    private fun isServerReachable(): Boolean {
        var reachable = false
        val thread = Thread {
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("127.0.0.1", 8000), 1000)
                socket.close()
                reachable = true
                android.util.Log.d("NetworkUtils", "isServerReachable: true")
            } catch (e: Exception) {
                android.util.Log.d("NetworkUtils", "isServerReachable failed: ${e.message}")
                reachable = false
            }
        }
        thread.start()
        try {
            thread.join(1200)
        } catch (e: Exception) {
            android.util.Log.e("NetworkUtils", "Thread join interrupted", e)
        }
        android.util.Log.d("NetworkUtils", "isServerReachable final: $reachable")
        return reachable
    }

    /**
     * Cek apakah perangkat sedang terhubung ke internet
     */
    fun isOnline(context: Context): Boolean {
        val systemOnline = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            if (network != null) {
                val capabilities = cm.getNetworkCapabilities(network)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }

        android.util.Log.d("NetworkUtils", "isOnline systemOnline: $systemOnline")
        if (systemOnline) return true
        val reachable = isServerReachable()
        android.util.Log.d("NetworkUtils", "isOnline final: $reachable")
        return reachable
    }

    /**
     * Flow yang mengamati perubahan konektivitas jaringan secara real-time.
     * Emit true ketika online, false ketika offline.
     */
    fun observeNetworkStatus(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isOnline(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(isOnline(context))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(callback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
        }

        // Emit status awal
        trySend(isOnline(context))

        awaitClose {
            cm.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
