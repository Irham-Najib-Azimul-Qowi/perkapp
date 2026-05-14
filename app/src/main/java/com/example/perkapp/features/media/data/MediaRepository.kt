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
            entity_id =  entityId,
            image_url =  "",
            local_path =  localPath,
            sync_status =  "pending"
        )
        dao.insertImage(entity)
    }

    suspend fun uploadImage(entityType: String, entityId: String, file: File): Boolean {
        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("images", file.name, requestFile)
            val typeBody = entityType.toRequestBody("text/plain".toMediaTypeOrNull())
            val idBody = entityId.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = api.uploadImage(body, typeBody, idBody)
            if (response.isSuccessful) {
                response.body()?.data?.let { result ->
                    val entity = ImageEntity(
                        id = UUID.randomUUID().toString(),
                        entity_type =  entityType,
                        entity_id = entityId,
                        image_url =  result.image_url,
                        local_path =  file.absolutePath,
                        sync_status =  "synced"
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