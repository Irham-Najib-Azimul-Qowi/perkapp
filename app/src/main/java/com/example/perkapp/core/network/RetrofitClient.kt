package com.example.perkapp.core.network

import android.content.Context
import com.example.perkapp.core.datastore.UserPreferences
import com.example.perkapp.core.datastore.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * RetrofitClient — "Kantor Pos" utama aplikasi untuk berkomunikasi dengan server internet.
 *
 * Mengatur semua persiapan pengiriman data ke backend (API Laravel), termasuk:
 * - Menempelkan Token Login secara otomatis ke setiap permintaan
 * - Menangani konversi data JSON menjadi objek Kotlin (menggunakan Gson)
 * - Mencatat (logging) isi pesan yang dikirim/diterima di Logcat Android Studio
 */
object RetrofitClient {
    // Alamat utama server backend (Pastikan diakhiri dengan tanda '/')
    private const val BASE_URL = "https://cakramanggalapnm.com/api/v1/"

    // Digunakan sementara oleh fitur Alat/Media yang belum memakai UserPreferences
    // Berfungsi sebagai "kartu identitas darurat"
    var authToken: String = ""

    // HttpLoggingInterceptor bertugas menampilkan log request dan response HTTP
    // Level BODY berarti seluruh isi data (JSON) akan dicetak di logcat (berguna untuk debugging)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Membuat pencegat (Interceptor) yang otomatis menempelkan "Surat Izin" (Token).
     * 
     * Saat kita minta data ke server, server akan bertanya "Kamu siapa?".
     * Interceptor ini menyelipkan Header 'Authorization: Bearer <token>' 
     * ke semua request agar server mengizinkan akses.
     */
    fun getAuthInterceptor(userPreferences: UserPreferences? = null, context: Context? = null): Interceptor {
        return Interceptor { chain ->
            // Siapkan paket data yang mau dikirim
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json") // Minta balasan berupa format JSON
            
            var token = authToken
            
            // Mencari token dari sistem penyimpanan lokal (DataStore)
            if (userPreferences != null) {
                // runBlocking memaksa fungsi asynchronous berjalan sinkron agar Interceptor tidak error
                val flowToken = runBlocking { userPreferences.getAuthToken.first() }
                if (!flowToken.isNullOrBlank()) {
                    token = flowToken
                }
            } else if (context != null) {
                val prefs = UserPreferences(context.dataStore)
                val flowToken = runBlocking { prefs.getAuthToken.first() }
                if (!flowToken.isNullOrBlank()) {
                    token = flowToken
                }
            }

            // Jika token ditemukan, tempelkan ke kop surat (Header)
            if (token.isNotBlank()) {
                request.addHeader("Authorization", "Bearer $token")
            }
            // Lanjutkan pengiriman data ke server
            chain.proceed(request.build())
        }
    }

    /**
     * Instance Retrofit bawaan (Default) yang dipakai secara luas di aplikasi.
     * lazy berarti: baru akan diciptakan saat pertama kali dipanggil (menghemat memori).
     */
    val instance: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                
                var token = authToken
                try {
                    // Mengambil context secara global
                    val context = com.example.perkapp.PerkappApplication.instance
                    val prefs = UserPreferences(context.dataStore)
                    val flowToken = runBlocking { prefs.getAuthToken.first() }
                    if (!flowToken.isNullOrBlank()) {
                        token = flowToken
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (token.isNotBlank()) {
                    request.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(request.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // Pasang pengaturan klien HTTP tadi
            .addConverterFactory(GsonConverterFactory.create()) // Pengonversi JSON otomatis
            .build()
    }

    /**
     * Method pembuat Retrofit Client yang lebih spesifik (Biasanya dipakai di fitur Auth).
     * Menerima UserPreferences sebagai parameter yang dikirim via Dependency Injection (DI).
     */
    fun getClient(userPreferences: UserPreferences): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(getAuthInterceptor(userPreferences))
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Melakukan silent auto-login di latar belakang untuk mendapatkan token JWT.
     * Digunakan sebagai solusi sementara atau untuk testing saat token kadaluwarsa.
     */
    suspend fun performSilentLogin(context: Context): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val loginClient = OkHttpClient.Builder().build()
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                // Data login cadangan/admin default
                val jsonBody = "{\"email\":\"admin@cakramanggala.com\",\"password\":\"admin123\"}"
                val body = jsonBody.toRequestBody(mediaType)
                
                val request = okhttp3.Request.Builder()
                    .url(BASE_URL + "auth/login")
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .build()

                val response = loginClient.newCall(request).execute()
                android.util.Log.d("RetrofitClient", "performSilentLogin response code: ${response.code}")
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    android.util.Log.d("RetrofitClient", "performSilentLogin responseBody: $responseBody")
                    if (responseBody != null) {
                        val jsonObject = org.json.JSONObject(responseBody)
                        if (jsonObject.getBoolean("success")) {
                            val dataObject = jsonObject.getJSONObject("data")
                            val token = dataObject.getString("token")
                            
                            // Simpan token ke RAM
                            authToken = token
                            android.util.Log.d("RetrofitClient", "performSilentLogin success, token: $token")
                            
                            // Simpan juga ke penyimpanan lokal (UserPreferences) agar tersinkronisasi
                            val userPrefs = UserPreferences(context.dataStore)
                            userPrefs.saveAuthToken(token)
                            return@withContext true
                        }
                    }
                } else {
                    android.util.Log.e("RetrofitClient", "performSilentLogin failed: ${response.message}")
                }
                false
            } catch (e: Exception) {
                android.util.Log.e("RetrofitClient", "performSilentLogin exception", e)
                false
            }
        }
    }
}
