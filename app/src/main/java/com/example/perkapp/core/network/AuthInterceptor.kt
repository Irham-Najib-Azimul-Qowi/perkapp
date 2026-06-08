package com.example.perkapp.core.network

import com.example.perkapp.core.datastore.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * AuthInterceptor — Penjaga gawang untuk keamanan API.
 *
 * Ini adalah versi kelas terpisah dari fungsi pencegat (Interceptor)
 * yang ada di RetrofitClient. Tujuannya sama: memastikan setiap 
 * permintaan data diselipkan Token Login dari DataStore.
 */
class AuthInterceptor(private val userPreferences: UserPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Ambil token dari penyimpanan (tunggu sampai dapat menggunakan runBlocking)
        val token = runBlocking { userPreferences.getAuthToken.first() }
        val request = chain.request().newBuilder()

        // Jika tokennya ada, pasang ke dalam Header 'Authorization'
        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }

        // Lanjutkan mengirim request yang sudah dimodifikasi ini ke server
        return chain.proceed(request.build())
    }
}