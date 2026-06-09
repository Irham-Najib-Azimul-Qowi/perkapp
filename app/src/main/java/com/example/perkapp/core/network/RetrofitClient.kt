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
     * FUNGSI: getAuthInterceptor
     * TUJUAN: Membuat "petugas bea cukai" (Interceptor) yang akan merazia dan menempelkan 
     * surat izin (Token JWT) ke setiap paket data HTTP sebelum meluncur ke internet.
     * Ia akan mencari token di beberapa tempat:
     * 1. Variabel Global RAM (`authToken`).
     * 2. Parameter `UserPreferences` jika disuntikkan secara eksplisit.
     * 3. Parameter `Context` jika ada, untuk membuat instance DataStore baru.
     * 
     * @param userPreferences Pilihan tempat baca token pertama.
     * @param context Pilihan tempat baca token kedua.
     * @return Objek Interceptor yang siap dipasang ke OkHttp.
     */
    fun getAuthInterceptor(userPreferences: UserPreferences? = null, context: Context? = null): Interceptor {
        return Interceptor { chain ->
            // Siapkan paket data yang mau dikirim
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json") // Minta balasan berupa format JSON agar tidak di-redirect ke halaman web HTML
            
            var token = authToken
            
            // Mencari token dari sistem penyimpanan lokal (DataStore)
            if (userPreferences != null) {
                // runBlocking memaksa fungsi asynchronous berjalan sinkron agar OkHttp tidak terhenti karena menunggu coroutine
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
     * PROPERTI: instance
     * TUJUAN: Menyiapkan klien HTTP dan Retrofit (alat parsing) utama secara global.
     * Memakai kata kunci `by lazy` sehingga ia tidak akan memakan RAM sebelum 
     * baris kodenya benar-benar dipanggil. Klien ini sudah dikemas lengkap dengan 
     * sistem logging (untuk melihat balasan server di Logcat Android Studio) 
     * dan injeksi token otomatis mengambil konteks aplikasi global (`PerkappApplication.instance`).
     */
    val instance: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                
                var token = authToken
                try {
                    // Mengambil context secara global agar tidak kerepotan passing Context
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
            .addConverterFactory(GsonConverterFactory.create()) // Pengonversi teks JSON dari server ke dalam bentuk objek/Class Kotlin
            .build()
    }

    /**
     * FUNGSI: getClient
     * TUJUAN: Sebuah pabrik (Factory) untuk menciptakan objek Retrofit baru yang secara
     * spesifik menggunakan `UserPreferences` tertentu (biasanya disediakan lewat Hilt DI).
     * Metode ini memastikan fitur seperti Autentikasi (AuthRepository) memiliki pengontrol 
     * sesi yang ketat dan tidak bercampur.
     * 
     * @param userPreferences Kelas DataStore tempat token disembunyikan.
     * @return Objek Retrofit yang sudah dipersenjatai Interceptor dan Logger.
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
     * FUNGSI: performSilentLogin
     * TUJUAN: Menjalankan *script* Login di balik layar tanpa sepengetahuan antarmuka pengguna (UI).
     * Sering digunakan sebagai trik pemulihan (Recovery) saat sinkronisasi gagal karena JWT token hangus.
     * Menggunakan kredensial "Admin Default" sebagai contoh implementasi di mode testing/development.
     * 
     * ALUR LOGIKA:
     * 1. Menyiapkan bungkusan JSON berisi email & password (Hardcoded untuk tahap dev).
     * 2. Menembakkan permintaan POST murni (tanpa Retrofit) ke rute `/auth/login`.
     * 3. Jika jawaban HTTP adalah kode 2xx (Sukses), ia akan membedah teks balasan 
     *    untuk menarik keluar string `token`.
     * 4. Menyimpan token baru itu ke dalam variabel `authToken` (RAM) 
     *    dan `UserPreferences` (Penyimpanan Fisik).
     * 
     * @param context Konteks aplikasi untuk bisa membuka brankas DataStore.
     * @return True bila login gaib ini berhasil, False bila gagal (misal server mati).
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
