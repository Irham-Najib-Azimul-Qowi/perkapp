package com.example.perkapp.features.alat.data.repository

import android.content.Context
import com.example.perkapp.core.utils.NetworkUtils
import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import java.util.UUID

class AlatRepository(
    private val api: AlatApiService,
    private val dao: AlatDao,
    private val context: Context
) {
    /**
     * Ambil semua alat.
     * - Jika online: fetch dari API, simpan ke local, lalu kembalikan dari local
     * - Jika offline: langsung kembalikan dari local database
     */
    suspend fun getAllAlat(): List<AlatEntity> {
        if (NetworkUtils.isOnline(context)) {
            try {
                val response = api.getAllAlat()
                if (response.isSuccessful) {
                    response.body()?.data?.let { alatList ->
                        // Ambil data pending lokal sebelum replace
                        val pendingItems = dao.getPendingAlat()
                        val pendingIds = pendingItems.map { it.id }.toSet()

                        val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                        val imageDao = db.imageDao()

                        val entities = alatList.map { item ->
                            val existing = dao.getAlatById(item.id)
                            var imagePath = existing?.image_path
                            if (imagePath.isNullOrBlank()) {
                                val images = imageDao.getImagesForEntity("alat", item.id)
                                if (images.isNotEmpty()) {
                                    imagePath = images.first().image_url ?: images.first().local_path
                                }
                            }
                            AlatEntity(
                                id = item.id,
                                name = item.name,
                                category = item.category,
                                total_qty = item.total_qty,
                                available_qty = item.available_qty,
                                condition = item.condition,
                                sync_status = "synced",
                                image_path = imagePath,
                                pending_action = null
                            )
                        }
                        // Insert data dari server (tapi jangan timpa data pending lokal)
                        entities.filter { it.id !in pendingIds }.forEach { entity ->
                            dao.insertAlat(entity)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // AUTO-REPAIR BLOCK: Reconstruct any NULL image_path from the images table
        try {
            val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
            val imageDao = db.imageDao()
            val localAlats = dao.getAllAlat()
            localAlats.forEach { alat ->
                if (alat.image_path.isNullOrBlank()) {
                    val images = imageDao.getImagesForEntity("alat", alat.id)
                    if (images.isNotEmpty()) {
                        val path = images.first().image_url ?: images.first().local_path
                        if (!path.isNullOrBlank()) {
                            val updated = alat.copy(image_path = path)
                            dao.insertAlat(updated)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return dao.getAllAlat()
    }

    /**
     * Buat alat baru.
     * - Simpan ke local database dulu (pending)
     * - Jika online: langsung sync ke API
     * - Jika offline: tetap tersimpan lokal, nanti di-sync oleh SyncWorker
     */
    suspend fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
        val localId = UUID.randomUUID().toString()
        val localEntity = AlatEntity(
            id = localId,
            name = name,
            category = category,
            total_qty = totalQty,
            available_qty = totalQty,
            condition = condition,
            sync_status = "pending",
            image_path = imagePath,
            pending_action = "create"
        )
        dao.insertAlat(localEntity)

        // Simpan gambar secara lokal (dan coba upload langsung jika online)
        if (!imagePath.isNullOrBlank()) {
            try {
                val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                val mediaApi = com.example.perkapp.core.network.RetrofitClient.instance.create(com.example.perkapp.features.media.api.MediaApiService::class.java)
                val imageDao = db.imageDao()
                val mediaRepository = com.example.perkapp.features.media.data.MediaRepository(mediaApi, imageDao, context)
                mediaRepository.saveImageLocally("alat", localId, imagePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (NetworkUtils.isOnline(context)) {
            try {
                val request = CreateAlatRequest(name, category, totalQty, condition)
                val response = api.createAlat(request)
                if (response.isSuccessful) {
                    response.body()?.data?.let { apiAlat ->
                        // Hapus entri lokal dan ganti dengan data dari server
                        dao.deleteAlat(localId)
                        dao.insertAlat(
                            AlatEntity(
                                id = apiAlat.id,
                                name = apiAlat.name,
                                category = apiAlat.category,
                                total_qty = apiAlat.total_qty,
                                available_qty = apiAlat.available_qty,
                                condition = apiAlat.condition,
                                sync_status = "synced",
                                image_path = imagePath,
                                pending_action = null
                            )
                        )

                        // Update entityId gambar di Room dari localId ke server ID
                        try {
                            val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                            val imageDao = db.imageDao()
                            val localImages = imageDao.getImagesForEntity("alat", localId)
                            for (img in localImages) {
                                imageDao.deleteImage(img.id)
                                val updatedImg = img.copy(entity_id = apiAlat.id)
                                imageDao.insertImage(updatedImg)

                                // Coba upload ulang dengan server ID yang benar
                                if (updatedImg.sync_status == "pending") {
                                    val mediaApi = com.example.perkapp.core.network.RetrofitClient.instance.create(com.example.perkapp.features.media.api.MediaApiService::class.java)
                                    val mediaRepository = com.example.perkapp.features.media.data.MediaRepository(mediaApi, imageDao, context)
                                    mediaRepository.syncPendingImages()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                // Gagal sync - data tetap tersimpan lokal dengan status pending
                e.printStackTrace()
            }
        }
    }

    /**
     * Update alat.
     * - Update di local database dulu (pending)
     * - Jika online: langsung sync ke API
     * - Jika offline: data terupdate lokal, nanti di-sync oleh SyncWorker
     */
    suspend fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        val updated = alat.copy(
            name = request.name,
            category = request.category,
            total_qty = request.total_qty,
            condition = request.condition,
            sync_status = "pending",
            image_path = request.image_path,
            // Jika item belum pernah di-sync (masih create pending), tetap "create"
            pending_action = if (alat.pending_action == "create") "create" else "update"
        )
        dao.updateAlat(updated)

        // Simpan gambar secara lokal jika baru (dan coba upload langsung jika online)
        if (!request.image_path.isNullOrBlank() && request.image_path != alat.image_path) {
            try {
                val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                val mediaApi = com.example.perkapp.core.network.RetrofitClient.instance.create(com.example.perkapp.features.media.api.MediaApiService::class.java)
                val imageDao = db.imageDao()
                val mediaRepository = com.example.perkapp.features.media.data.MediaRepository(mediaApi, imageDao, context)
                mediaRepository.saveImageLocally("alat", alat.id, request.image_path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (NetworkUtils.isOnline(context)) {
            try {
                val response = api.updateAlat(alat.id, request)
                if (response.isSuccessful) {
                    dao.updateAlat(updated.copy(sync_status = "synced", pending_action = null))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Hapus alat.
     * - Jika online: hapus dari API dan lokal
     * - Jika offline: tandai sebagai pending delete (soft delete), nanti di-sync
     */
    suspend fun deleteAlat(id: String) {
        if (NetworkUtils.isOnline(context)) {
            try {
                val response = api.deleteAlat(id)
                if (response.isSuccessful) {
                    dao.deleteAlat(id)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Offline atau gagal delete dari API: soft delete
        val existing = dao.getAlatById(id)
        if (existing != null) {
            if (existing.pending_action == "create") {
                // Belum pernah di-sync, hapus langsung dari lokal
                dao.deleteAlat(id)
            } else {
                // Tandai sebagai pending delete
                dao.updateAlat(
                    existing.copy(
                        sync_status = "pending",
                        pending_action = "delete"
                    )
                )
            }
        }
    }

    suspend fun getAlatById(id: String): AlatEntity? {
        return dao.getAlatById(id)
    }

    /**
     * Sync semua data pending ke server.
     * Dipanggil oleh SyncWorker saat ada koneksi internet.
     */
    suspend fun syncPendingData(): Boolean {
        val pendingItems = dao.getPendingAlat()
        var allSuccess = true

        for (item in pendingItems) {
            try {
                when (item.pending_action) {
                    "create" -> {
                        val request = CreateAlatRequest(
                            name = item.name,
                            category = item.category,
                            total_qty = item.total_qty,
                            condition = item.condition
                        )
                        val response = api.createAlat(request)
                        if (response.isSuccessful) {
                            response.body()?.data?.let { apiAlat ->
                                dao.deleteAlat(item.id)
                                dao.insertAlat(
                                    AlatEntity(
                                        id = apiAlat.id,
                                        name = apiAlat.name,
                                        category = apiAlat.category,
                                        total_qty = apiAlat.total_qty,
                                        available_qty = apiAlat.available_qty,
                                        condition = apiAlat.condition,
                                        sync_status = "synced",
                                        image_path = item.image_path,
                                        pending_action = null
                                    )
                                )

                                // Update entityId gambar di Room dari localId ke server ID
                                try {
                                    val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                                    val imageDao = db.imageDao()
                                    val localImages = imageDao.getImagesForEntity("alat", item.id)
                                    for (img in localImages) {
                                        imageDao.deleteImage(img.id)
                                        val updatedImg = img.copy(entity_id = apiAlat.id)
                                        imageDao.insertImage(updatedImg)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            allSuccess = false
                        }
                    }
                    "update" -> {
                        val request = CreateAlatRequest(
                            name = item.name,
                            category = item.category,
                            total_qty = item.total_qty,
                            condition = item.condition,
                            image_path = item.image_path
                        )
                        val response = api.updateAlat(item.id, request)
                        if (response.isSuccessful) {
                            dao.updateAlat(
                                item.copy(sync_status = "synced", pending_action = null)
                            )
                        } else {
                            allSuccess = false
                        }
                    }
                    "delete" -> {
                        val response = api.deleteAlat(item.id)
                        if (response.isSuccessful) {
                            dao.deleteAlat(item.id)
                        } else {
                            allSuccess = false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                allSuccess = false
            }
        }
        return allSuccess
    }
}
