# Tutorial Perbaikan Perkapp — Part 4 (FINAL): Media Upload & Shared Utils

Ini adalah bagian terakhir. Di sini kita menambahkan fitur **upload gambar**
dan komponen **shared** yang menjadi tanggung jawab Najib.

---

## Tahap 10: Buat Fitur Media (Upload Gambar)

### File BARU: `features/media/data/ImageEntity.kt`
Lokasi: `app/src/main/java/com/example/perkapp/features/media/data/ImageEntity.kt`

```kotlin
package com.example.perkapp.features.media.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey
    val id: String,
    val entity_type: String,   // "alat" atau "kegiatan"
    val entity_id: String,
    val image_url: String,     // URL dari server atau path lokal
    val local_path: String = "",
    val sync_status: String = "synced"
)
```

---

### File BARU: `features/media/data/ImageDao.kt`
Lokasi: `app/src/main/java/com/example/perkapp/features/media/data/ImageDao.kt`

```kotlin
package com.example.perkapp.features.media.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE entity_type = :type AND entity_id = :entityId")
    suspend fun getImagesForEntity(type: String, entityId: String): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllImages(images: List<ImageEntity>)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImage(id: String)
}
```

---

### File BARU: `features/media/api/MediaApiService.kt`
Lokasi: `app/src/main/java/com/example/perkapp/features/media/api/MediaApiService.kt`

```kotlin
package com.example.perkapp.features.media.api

import com.example.perkapp.core.network.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ImageUploadResponse(
    val image_url: String
)

interface MediaApiService {

    @Multipart
    @POST("upload-image")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("entity_type") entityType: RequestBody,
        @Part("entity_id") entityId: RequestBody
    ): Response<ApiResponse<ImageUploadResponse>>
}
```

---

### File BARU: `features/media/data/MediaRepository.kt`
Lokasi: `app/src/main/java/com/example/perkapp/features/media/data/MediaRepository.kt`

```kotlin
package com.example.perkapp.features.media.data

import com.example.perkapp.features.media.api.MediaApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class MediaRepository(
    private val api: MediaApiService,
    private val dao: ImageDao
) {
    suspend fun getImagesForAlat(alatId: String): List<ImageEntity> {
        return dao.getImagesForEntity("alat", alatId)
    }

    suspend fun saveImageLocally(entityType: String, entityId: String, localPath: String) {
        val entity = ImageEntity(
            id = UUID.randomUUID().toString(),
            entity_type = entityType,
            entity_id = entityId,
            image_url = "",
            local_path = localPath,
            sync_status = "pending"
        )
        dao.insertImage(entity)
    }

    suspend fun uploadImage(entityType: String, entityId: String, file: File): Boolean {
        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            val typeBody = entityType.toRequestBody("text/plain".toMediaTypeOrNull())
            val idBody = entityId.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadImage(body, typeBody, idBody)
            if (response.isSuccessful) {
                response.body()?.data?.let { result ->
                    val entity = ImageEntity(
                        id = UUID.randomUUID().toString(),
                        entity_type = entityType,
                        entity_id = entityId,
                        image_url = result.image_url,
                        local_path = file.absolutePath,
                        sync_status = "synced"
                    )
                    dao.insertImage(entity)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
```

---

## Tahap 11: Buat Shared Components

### File BARU: `features/shared/ImagePicker.kt`
Lokasi: `app/src/main/java/com/example/perkapp/features/shared/ImagePicker.kt`

```kotlin
package com.example.perkapp.features.shared

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ImagePickerButton(
    onImagePicked: (Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImagePicked(it) }
    }

    Button(onClick = { launcher.launch("image/*") }) {
        Text("Pilih Gambar")
    }
}
```

---

### File BARU: `features/shared/PermissionHandler.kt`
Lokasi: `app/src/main/java/com/example/perkapp/features/shared/PermissionHandler.kt`

```kotlin
package com.example.perkapp.features.shared

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun RequestStoragePermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted() else onDenied()
    }

    LaunchedEffect(Unit) {
        launcher.launch(permission)
    }
}
```

---

## Tahap 12: Update AppDatabase — Tambahkan ImageEntity

Buka file `core/database/AppDatabase.kt` (dari Part 1), lalu update agar
menyertakan `ImageEntity` dan `ImageDao`. Ganti SELURUH isi file:

```kotlin
package com.example.perkapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.media.data.ImageDao
import com.example.perkapp.features.media.data.ImageEntity

@Database(
    entities = [AlatEntity::class, ImageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alatDao(): AlatDao
    abstract fun imageDao(): ImageDao

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

## Peta File Lengkap FINAL (Semua Part)

```
app/src/main/java/com/example/perkapp/
│
├── MainActivity.kt                              ← UBAH (Part 3)
│
├── core/
│   ├── database/
│   │   └── AppDatabase.kt                       ← BUAT BARU (Part 1, update Part 4)
│   └── network/
│       ├── ApiResponse.kt                        ← BUAT BARU (Part 1)
│       └── RetrofitClient.kt                     ← BUAT BARU (Part 1)
│
├── features/
│   ├── alat/
│   │   ├── api/
│   │   │   └── AlatApiService.kt                 ← UBAH (Part 1)
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── AlatEntity.kt                 ← UBAH (Part 1)
│   │   │   │   └── AlatDao.kt                    ← UBAH (Part 1)
│   │   │   ├── remote/
│   │   │   │   ├── AlatResponse.kt               ← TIDAK DIUBAH
│   │   │   │   └── CreateAlatRequest.kt          ← TIDAK DIUBAH
│   │   │   └── repository/
│   │   │       └── AlatRepository.kt             ← UBAH (Part 1)
│   │   └── ui/
│   │       ├── component/
│   │       │   └── AlatCard.kt                   ← UBAH (Part 2)
│   │       ├── screen/
│   │       │   ├── InventarisScreen.kt           ← UBAH (Part 2)
│   │       │   ├── TambahAlatScreen.kt           ← UBAH (Part 2)
│   │       │   ├── DetailAlatScreen.kt           ← BUAT BARU (Part 2, update Part 3)
│   │       │   └── EditAlatScreen.kt             ← BUAT BARU (Part 3)
│   │       └── viewmodel/
│   │           └── AlatViewModel.kt              ← UBAH (Part 2 + Part 3)
│   │
│   ├── media/                                     ← BUAT FOLDER BARU
│   │   ├── api/
│   │   │   └── MediaApiService.kt                ← BUAT BARU (Part 4)
│   │   └── data/
│   │       ├── ImageEntity.kt                    ← BUAT BARU (Part 4)
│   │       ├── ImageDao.kt                       ← BUAT BARU (Part 4)
│   │       └── MediaRepository.kt                ← BUAT BARU (Part 4)
│   │
│   └── shared/                                    ← BUAT FOLDER BARU
│       ├── ImagePicker.kt                        ← BUAT BARU (Part 4)
│       └── PermissionHandler.kt                  ← BUAT BARU (Part 4)
│
└── ui/theme/
    ├── Color.kt                                  ← TIDAK DIUBAH
    ├── Theme.kt                                  ← TIDAK DIUBAH
    └── Type.kt                                   ← TIDAK DIUBAH
```

### File config yang diubah:
```
gradle/libs.versions.toml                          ← UBAH (Part 1)
build.gradle.kts (root)                            ← UBAH (Part 1)
app/build.gradle.kts                               ← UBAH (Part 1)
app/src/main/AndroidManifest.xml                   ← UBAH (Part 1)
```

---

## Ringkasan Total

| Kategori | Jumlah |
|----------|--------|
| File diubah | 14 |
| File baru dibuat | 10 |
| File tidak diubah | 5 |

---

## Urutan Pengerjaan yang Benar

1. `gradle/libs.versions.toml` → **Sync Gradle**
2. `build.gradle.kts` (root) → **Sync Gradle**
3. `app/build.gradle.kts` → **Sync Gradle**
4. `AndroidManifest.xml`
5. `core/network/ApiResponse.kt` (baru)
6. `core/network/RetrofitClient.kt` (baru)
7. `features/alat/data/local/AlatEntity.kt`
8. `features/alat/data/local/AlatDao.kt`
9. `features/media/data/ImageEntity.kt` (baru)
10. `features/media/data/ImageDao.kt` (baru)
11. `core/database/AppDatabase.kt` (baru, versi Part 4)
12. `features/alat/api/AlatApiService.kt`
13. `features/alat/data/repository/AlatRepository.kt`
14. `features/media/api/MediaApiService.kt` (baru)
15. `features/media/data/MediaRepository.kt` (baru)
16. `features/shared/ImagePicker.kt` (baru)
17. `features/shared/PermissionHandler.kt` (baru)
18. `features/alat/ui/viewmodel/AlatViewModel.kt`
19. `features/alat/ui/component/AlatCard.kt`
20. `features/alat/ui/screen/InventarisScreen.kt`
21. `features/alat/ui/screen/TambahAlatScreen.kt`
22. `features/alat/ui/screen/DetailAlatScreen.kt` (baru)
23. `features/alat/ui/screen/EditAlatScreen.kt` (baru)
24. `MainActivity.kt`

---

## ✅ SELESAI

Dengan Part 1 + Part 2 + Part 3 + Part 4, seluruh tanggung jawab **Najib**
sudah tercakup:

- ✅ Inventaris (list alat)
- ✅ Detail alat
- ✅ Tambah alat
- ✅ Edit alat
- ✅ Upload gambar (media)
- ✅ Image Picker
- ✅ Permission Handler
- ✅ Offline-first (Room Database)
- ✅ Sync ke API (Retrofit)
