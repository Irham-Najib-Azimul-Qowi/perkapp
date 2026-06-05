package com.example.perkapp.core.network

import okhttp3.Interceptor
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    private const val BASE_URL = "https://cakramanggalapnm.com/api/v1/"

    var authToken: String = ""

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
        if (authToken.isNotBlank()) {
            request.addHeader("Authorization", "Bearer $authToken")
        }
        chain.proceed(request.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Melakukan silent auto-login di latar belakang untuk mendapatkan token JWT.
     * Digunakan sebagai solusi sementara karena halaman login belum dibuat.
     */
    suspend fun performSilentLogin(context: android.content.Context): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Gunakan client terpisah untuk menghindari interceptor token lama yang expired
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