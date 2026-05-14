package com.example.perkapp.features.alat.data.repository

// 1. Pastikan Nama Service Sesuai (Cek folder features/alat/api)
import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import java.util.UUID

class AlatRepository (
    private val api: AlatApiService, // Samakan dengan import di atas
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
        val localEntity = AlatEntity(
            id  = UUID.randomUUID().toString(),
            name = name,
            category = category,
            total_qty =  totalQty,
            available_qty =  totalQty,
            condition = condition,
            sync_status =  "pending"
        )
        dao.insertAlat(localEntity)

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
                            total_qty =  apiAlat.total_qty,
                            available_qty =  apiAlat.available_qty,
                            condition = apiAlat.condition,
                            sync_status =  "synced"
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
            name =  request.name,
            category =  request.category,
            total_qty =  request.total_qty,
            condition =  request.condition,
            sync_status =  "pending"
        )
        dao.updateAlat(updated)

        try {
            val response = api.updateAlat(alat.id, request)
            if (response.isSuccessful) {
                dao.updateAlat(updated.copy(sync_status =  "synced"))
            }
        } catch ( e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAlatById(id: String): AlatEntity? {
        return dao.getAlatById(id)
    }
}
