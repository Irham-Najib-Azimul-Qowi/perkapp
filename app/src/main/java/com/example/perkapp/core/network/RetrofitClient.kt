package com.example.perkapp.core.network

import com.example.perkapp.core.datastore.UserPreferences
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Sesuai instruksi: Base URL https://api.perkapp.com/v1
    private const val BASE_URL = "https://api.perkapp.com/v1/"

    fun getClient(userPreferences: UserPreferences): Retrofit {
        // Pasang interceptor agar semua request API otomatis diselipkan Bearer Token
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(userPreferences))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
