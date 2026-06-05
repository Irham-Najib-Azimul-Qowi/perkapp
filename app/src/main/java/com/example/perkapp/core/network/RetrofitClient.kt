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

object RetrofitClient {
    private const val BASE_URL = "https://api.perkapp.com/v1/"

    // Digunakan oleh fitur Alat/Media yang belum di-refactor ke UserPreferences
    var authToken: String = ""

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor yang membaca token dari UserPreferences (jika ada) ATAU dari static authToken
    fun getAuthInterceptor(userPreferences: UserPreferences? = null, context: Context? = null): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
            
            var token = authToken
            if (userPreferences != null) {
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

            if (token.isNotBlank()) {
                request.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(request.build())
        }
    }

    // Instance default (lazy) dengan fallback context/static token
    val instance: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                if (authToken.isNotBlank()) {
                    request.addHeader("Authorization", "Bearer $authToken")
                }
                chain.proceed(request.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Method untuk Adam / Auth flow
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
     * Digunakan sebagai solusi sementara atau untuk testing.
     */
    suspend fun performSilentLogin(context: Context): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val loginClient = OkHttpClient.Builder().build()
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val jsonBody = "{\"email\":\"test@test.com\",\"password\":\"password123\"}"
                val body = jsonBody.toRequestBody(mediaType)
                val request = okhttp3.Request.Builder()
                    .url(BASE_URL + "auth/login")
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .build()

                val response = loginClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val jsonObject = org.json.JSONObject(responseBody)
                        if (jsonObject.getBoolean("success")) {
                            val dataObject = jsonObject.getJSONObject("data")
                            val token = dataObject.getString("token")
                            authToken = token
                            // Simpan juga ke userPreferences agar ter-sync
                            val userPrefs = UserPreferences(context.dataStore)
                            userPrefs.saveAuthToken(token)
                            return@withContext true
                        }
                    }
                }
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
