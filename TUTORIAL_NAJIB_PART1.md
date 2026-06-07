# Tutorial Lengkap Perbaikan Project Perkapp — Bagian Najib

## Daftar Isi
- [Tahap 1: Perbaiki Dependencies (build.gradle)](#tahap-1)
- [Tahap 2: Buat Infrastruktur Core](#tahap-2)
- [Tahap 3: Perbaiki Fitur Alat](#tahap-3)
- [Tahap 4: Perbaiki UI Screens](#tahap-4)
- [Tahap 5: Hubungkan ke MainActivity](#tahap-5)

---

## Tahap 1: Perbaiki Dependencies {#tahap-1}

### File: `gradle/libs.versions.toml`
Ganti SELURUH isi file dengan ini:

```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
coreKtx = "1.15.0"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.09.00"
room = "2.6.1"
retrofit = "2.9.0"
okhttp = "4.12.0"
gson = "2.10.1"
navigationCompose = "2.8.4"
coroutines = "1.7.3"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-runtime-livedata = { group = "androidx.compose.runtime", name = "runtime-livedata" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Retrofit
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```

---

### File: `build.gradle.kts` (ROOT — di folder paling luar)
Ganti SELURUH isi file dengan:

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

---

### File: `app/build.gradle.kts`
Ganti SELURUH isi file dengan:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.perkapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.perkapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Coroutines
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

---

### File: `AndroidManifest.xml`
Tambahkan permission INTERNET. Ganti SELURUH isi file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Perkapp">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.Perkapp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

> ✅ Setelah tahap ini: Sync Gradle di Android Studio (File > Sync Project with Gradle Files)

---

## Tahap 2: Buat Infrastruktur Core {#tahap-2}

### File BARU: `core/network/ApiResponse.kt`
Lokasi: `app/src/main/java/com/example/perkapp/core/network/ApiResponse.kt`

```kotlin
package com.example.perkapp.core.network

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T
)
```

---

### File BARU: `core/network/RetrofitClient.kt`
Lokasi: `app/src/main/java/com/example/perkapp/core/network/RetrofitClient.kt`

```kotlin
package com.example.perkapp.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://domain.com/api/v1/"

    // Token disimpan di sini, nanti diisi setelah login oleh Adam
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
}
```

---

### File BARU: `core/database/AppDatabase.kt`
Lokasi: `app/src/main/java/com/example/perkapp/core/database/AppDatabase.kt`

```kotlin
package com.example.perkapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.alat.data.local.AlatEntity

@Database(
    entities = [AlatEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alatDao(): AlatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "perkapp_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

## Tahap 3: Perbaiki Fitur Alat {#tahap-3}

### File: `features/alat/data/local/AlatEntity.kt`
Ganti SELURUH isi file:

```kotlin
package com.example.perkapp.features.alat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alat")
data class AlatEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val total_qty: Int,
    val available_qty: Int,
    val condition: String,
    val sync_status: String = "synced"
)
```

---

### File: `features/alat/data/local/AlatDao.kt`
Ganti SELURUH isi file:

```kotlin
package com.example.perkapp.features.alat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlatDao {
    @Query("SELECT * FROM alat")
    suspend fun getAllAlat(): List<AlatEntity>

    @Query("SELECT * FROM alat WHERE id = :id")
    suspend fun getAlatById(id: String): AlatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlat(alat: AlatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAlat(alat: List<AlatEntity>)

    @Update
    suspend fun updateAlat(alat: AlatEntity)

    @Query("DELETE FROM alat WHERE id = :id")
    suspend fun deleteAlat(id: String)
}
```

---

### File: `features/alat/data/remote/AlatResponse.kt`
File ini sudah benar, tidak perlu diubah.

---

### File: `features/alat/data/remote/CreateAlatRequest.kt`
File ini sudah benar, tidak perlu diubah.

---

### File: `features/alat/api/AlatApiService.kt`
Ganti SELURUH isi file:

```kotlin
package com.example.perkapp.features.alat.api

import com.example.perkapp.core.network.ApiResponse
import com.example.perkapp.features.alat.data.remote.AlatResponse
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AlatApiService {

    @GET("alat")
    suspend fun getAllAlat(): Response<ApiResponse<List<AlatResponse>>>

    @GET("alat/{id}")
    suspend fun getAlatById(
        @Path("id") id: String
    ): Response<ApiResponse<AlatResponse>>

    @POST("alat")
    suspend fun createAlat(
        @Body request: CreateAlatRequest
    ): Response<ApiResponse<AlatResponse>>

    @PUT("alat/{id}")
    suspend fun updateAlat(
        @Path("id") id: String,
        @Body request: CreateAlatRequest
    ): Response<ApiResponse<AlatResponse>>

    @DELETE("alat/{id}")
    suspend fun deleteAlat(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}
```

---

### File: `features/alat/data/repository/AlatRepository.kt`
Ganti SELURUH isi file:

```kotlin
package com.example.perkapp.features.alat.data.repository

import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import java.util.UUID

class AlatRepository(
    private val api: AlatApiService,
    private val dao: AlatDao
) {
    suspend fun getAllAlat(): List<AlatEntity> {
        try {
            val response = api.getAllAlat()
            if (response.isSuccessful) {
                response.body()?.data?.let { alatList ->
                    val entities = alatList.map { item ->
                        AlatEntity(
                            id = item.id,
                            name = item.name,
                            category = item.category,
                            total_qty = item.total_qty,
                            available_qty = item.available_qty,
                            condition = item.condition,
                            sync_status = "synced"
                        )
                    }
                    dao.insertAllAlat(entities)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dao.getAllAlat()
    }

    suspend fun insertLocalAlat(alat: AlatEntity) {
        dao.insertAlat(alat)
    }

    suspend fun createAlat(name: String, category: String, totalQty: Int, condition: String) {
        // Simpan ke lokal dulu (offline-first)
        val localEntity = AlatEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            total_qty = totalQty,
            available_qty = totalQty,
            condition = condition,
            sync_status = "pending"
        )
        dao.insertAlat(localEntity)

        // Coba kirim ke API
        try {
            val request = CreateAlatRequest(name, category, totalQty, condition)
            val response = api.createAlat(request)
            if (response.isSuccessful) {
                response.body()?.data?.let { apiAlat ->
                    dao.deleteAlat(localEntity.id)
                    dao.insertAlat(
                        AlatEntity(
                            id = apiAlat.id,
                            name = apiAlat.name,
                            category = apiAlat.category,
                            total_qty = apiAlat.total_qty,
                            available_qty = apiAlat.available_qty,
                            condition = apiAlat.condition,
                            sync_status = "synced"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        val updated = alat.copy(
            name = request.name,
            category = request.category,
            total_qty = request.total_qty,
            condition = request.condition,
            sync_status = "pending"
        )
        dao.updateAlat(updated)

        try {
            val response = api.updateAlat(alat.id, request)
            if (response.isSuccessful) {
                dao.updateAlat(updated.copy(sync_status = "synced"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAlatById(id: String): AlatEntity? {
        return dao.getAlatById(id)
    }
}
```

> Lanjut ke TUTORIAL_NAJIB_PART2.md untuk Tahap 4 & 5 (UI + MainActivity)
