package com.example.perkapp.features.media.data

import android.content.Context
import com.example.perkapp.core.utils.NetworkUtils
import com.example.perkapp.features.media.api.MediaApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class MediaRepository(
    private val api: MediaApiService,
    private val dao: ImageDao,
    private val context: Context
) {
    suspend fun getImagesForAlat(alatId: String): List<ImageEntity> {
        return dao.getImagesForEntity("alat", alatId)
    }

    /**
     * Simpan gambar secara lokal (offline).
     * Gambar akan disimpan dengan status "pending" dan di-sync nanti.
     */
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

        // Coba upload langsung jika online
        if (NetworkUtils.isOnline(context)) {
            tryUploadImage(entity)
        }
    }

    /**
     * Upload gambar ke server.
     * Return true jika berhasil.
     */
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
                // Gagal upload, simpan lokal dengan status pending
                saveImageLocally(entityType, entityId, file.absolutePath)
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Error (misal: tidak ada internet), simpan lokal
            saveImageLocally(entityType, entityId, file.absolutePath)
            false
        }
    }

    private suspend fun tryUploadImage(image: ImageEntity): Boolean {
        if (image.local_path.isBlank()) return false
        val file = com.example.perkapp.core.utils.ImageUtils.getFileFromUri(context, image.local_path)
            ?: return false
        if (!file.exists()) return false

        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            val typeBody = image.entity_type.toRequestBody("text/plain".toMediaTypeOrNull())
            val idBody = image.entity_id.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = api.uploadImage(body, typeBody, idBody)
            if (response.isSuccessful) {
                response.body()?.data?.let { result ->
                    dao.updateImage(
                        image.copy(
                            image_url = result.image_url,
                            sync_status = "synced"
                        )
                    )

                    // Jika tipe entitas adalah 'alat', perbarui juga image_path di tabel 'alat'
                    if (image.entity_type == "alat") {
                        val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                        val alatDao = db.alatDao()
                        val alat = alatDao.getAlatById(image.entity_id)
                        if (alat != null) {
                            alatDao.updateAlat(alat.copy(image_path = result.image_url))
                        }
                    }
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

    /**
     * Sync semua gambar pending ke server.
     * Dipanggil oleh SyncWorker saat ada koneksi internet.
     */
    suspend fun syncPendingImages(): Boolean {
        val pendingImages = dao.getPendingImages()
        var allSuccess = true

        for (image in pendingImages) {
            if (!tryUploadImage(image)) {
                allSuccess = false
            }
        }
        return allSuccess
    }
}