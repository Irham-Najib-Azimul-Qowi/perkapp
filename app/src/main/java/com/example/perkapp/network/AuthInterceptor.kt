package com.example.perkapp.network

import com.example.perkapp.database.UserPreferences
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
    /**
     * FUNGSI: intercept
     * TUJUAN: Merupakan "Pos Pemeriksaan". Sebelum aplikasi mengirim permintaan data (Request) 
     * ke server Laravel, fungsi ini akan "menyetop" paket tersebut sesaat.
     * Kemudian ia mengambil Token Login dari memori HP (DataStore), dan jika ada,
     * menyelipkannya ke dalam struktur amplop HTTP Header (dengan label "Authorization").
     * Tanpa ini, server akan selalu membalas dengan status 401 Unauthorized.
     * 
     * @param chain Saluran pipa komunikasi yang memuat permintaan asli.
     * @return Response balasan dari server setelah permintaan modifikasi dikirimkan.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Ambil token dari penyimpanan (tunggu sampai dapat menggunakan runBlocking)
        val token = runBlocking { userPreferences.getAuthToken.first() }
        
        // 2. Bongkar paket permintaan agar bisa ditambah header baru
        val request = chain.request().newBuilder()

        // 3. Jika tokennya ada, pasang ke dalam Header 'Authorization' dengan format Bearer Token
        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }

        // 4. Lanjutkan mengirim request yang sudah dimodifikasi ini ke jalan tol internet
        return chain.proceed(request.build())
    }
}
