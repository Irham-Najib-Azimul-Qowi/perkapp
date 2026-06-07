# Panduan Perbaikan Project Perkapp

Dokumen ini menjelaskan langkah-langkah teknis untuk memperbaiki kode project Perkapp agar dapat di-compile dan dijalankan dengan benar.

## Daftar Perbaikan

### 1. Perbaikan Model Data (Entity & API Response)
Pastikan semua model data memiliki anotasi dan struktur yang benar.

**Langkah:**
- Buka `AlatEntity.kt`, tambahkan import Room (`androidx.room.*`).
- Buat file baru `AlatResponse.kt` di folder `api` untuk menampung data dari server.

### 2. Perbaikan Local Data (Room DAO)
Perbaiki typo dan anotasi di `AlatDao.kt`.

**Langkah:**
- Ganti `#Dao` menjadi `@Dao`.
- Ganti `seelct` menjadi `select`.
- Pastikan semua fungsi memiliki import `@Query`, `@Insert`, `@Update`, dll.

### 3. Perbaikan API Service (Retrofit)
Perbaiki anotasi di `AlatApiService.kt`.

**Langkah:**
- Ganti `@Target` menjadi `@GET`.
- Gunakan `retrofit2.Response` sebagai return type, bukan `ResponseCache`.
- Buat class `RetrofitClient` di folder `core` untuk menginisialisasi Retrofit.

### 4. Perbaikan UI Screens (Jetpack Compose)
Perbaiki logika UI dan import di `InventarisScreen.kt` dan `TambahAlatScreen.kt`.

**Langkah:**
- Tambahkan import untuk `runtime.getValue`, `runtime.setValue`, dan `livedata.observeAsState`.
- Selesaikan implementasi `TambahAlatScreen` yang terpotong.
- Hubungkan `MainActivity` dengan `InventarisScreen`.

---

## Kode Referensi Perbaikan

Berikut adalah contoh perbaikan untuk beberapa file utama:

### file: `AlatDao.kt`
```kotlin
package com.example.perkapp.features.alat.data.local

import androidx.room.*

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
}
```

### file: `AlatApiService.kt`
```kotlin
package com.example.perkapp.features.alat.api

import retrofit2.Response
import retrofit2.http.*

interface AlatApiService {
    @GET("api/v1/alat")
    suspend fun getAllAlat(): Response<List<AlatResponse>>

    @POST("api/v1/alat")
    suspend fun createAlat(@Body request: CreateAlatRequest): Response<AlatResponse>
}
```

## Tugas Selanjutnya
1. **Perbaiki Dependensi**: Pastikan library Room dan Retrofit sudah ada di `libs.versions.toml`.
2. **Inisialisasi Database**: Buat class `AppDatabase` di folder `core`.
3. **Navigasi**: Tambahkan library Navigation Compose untuk berpindah antar halaman.

---
*Ingin saya memperbaiki file-file ini secara otomatis? Balas dengan "Perbaiki Semua".*
