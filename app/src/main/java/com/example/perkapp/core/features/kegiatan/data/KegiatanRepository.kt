package com.example.perkapp.features.kegiatan.data

import android.content.Context
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.database.dao.KegiatanDao
import com.example.perkapp.core.database.entity.KegiatanEntity
import com.example.perkapp.core.database.entity.KegiatanAlatEntity
import com.example.perkapp.core.utils.NetworkUtils
import com.example.perkapp.features.kegiatan.api.KegiatanApiService
import com.example.perkapp.features.kegiatan.domain.InventoryStats
import com.example.perkapp.features.kegiatan.domain.Kegiatan
import com.example.perkapp.features.kegiatan.domain.StatusKegiatan
import com.example.perkapp.features.kegiatan.domain.UserInfo
import java.util.*

interface KegiatanRepository {
    suspend fun getInventoryStats(): Result<InventoryStats>
    suspend fun getKegiatanAktif(): Result<List<Kegiatan>>
    suspend fun getUserInfo(): UserInfo
    
    suspend fun getAllKegiatanLocal(): List<KegiatanEntity>
    suspend fun getAlatForKegiatanLocal(kegiatanId: String): List<KegiatanAlatEntity>
    
    suspend fun insertKegiatanLocal(
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String,
        tools: List<Pair<String, Int>>,
        externalTools: List<String>
    ): String
    
    suspend fun updateKegiatanAlatStatusLocal(kegiatanAlatId: String, isReturned: Boolean)
    suspend fun syncPendingKegiatan(): Boolean
    suspend fun deleteKegiatan(kegiatanId: String)
    suspend fun updateKegiatanLocal(
        id: String,
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String
    )
}

class KegiatanRepositoryImpl(
    private val apiService: KegiatanApiService,
    private val dao: KegiatanDao,
    private val context: Context
) : KegiatanRepository {

    override suspend fun getInventoryStats(): Result<InventoryStats> {
        return try {
            val db = AppDatabase.getDatabase(context)
            val alatDao = db.alatDao()
            val allAlat = alatDao.getAllAlat()
            
            val availableCount = allAlat.sumOf { it.available_qty }
            val pendingSyncCount = allAlat.count { it.sync_status == "pending" }
            
            val borrowedAlats = dao.getPendingKegiatanAlat() // get all tools
            val borrowedCount = dao.getAlatForKegiatan("").filter { !it.isReturned }.size // simple count
            
            // Query total active borrowed tools
            val allKegiatanAlat = db.openHelper.readableDatabase.compileStatement(
                "SELECT COUNT(*) FROM kegiatan_alat WHERE isReturned = 0"
            ).simpleQueryForLong().toInt()

            Result.success(
                InventoryStats(
                    borrowedCount = allKegiatanAlat,
                    availableCount = availableCount,
                    pendingSyncCount = pendingSyncCount
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success(InventoryStats(borrowedCount = 0, availableCount = 0, pendingSyncCount = 0))
        }
    }

    override suspend fun getKegiatanAktif(): Result<List<Kegiatan>> {
        return try {
            val entities = dao.getAllKegiatan().filter { it.status == "BERLANGSUNG" }
            val domainList = entities.map { entity ->
                Kegiatan(
                    id = entity.id,
                    kategori = entity.kategori,
                    judul = entity.judul,
                    lokasi = entity.lokasi,
                    labelWaktu = entity.tanggal,
                    progress = 0f, // progress removed as requested
                    statusType = when (entity.status) {
                        "BERLANGSUNG" -> StatusKegiatan.AKTIF
                        "MAINTENANCE" -> StatusKegiatan.MAINTENANCE
                        else -> StatusKegiatan.AUDIT
                    },
                    isPending = entity.sync_status == "pending"
                )
            }
            Result.success(domainList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserInfo(): UserInfo {
        return UserInfo(
            nama = "Reja",
            sapaan = getSapaanBerdasarkanJam(),
            fotoUrl = ""
        )
    }

    private fun getSapaanBerdasarkanJam(): String {
        val jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            jam < 12 -> "Good Morning"
            jam < 18 -> "Good Afternoon"
            else     -> "Good Evening"
        }
    }

    override suspend fun getAllKegiatanLocal(): List<KegiatanEntity> {
        return dao.getAllKegiatan()
    }

    override suspend fun getAlatForKegiatanLocal(kegiatanId: String): List<KegiatanAlatEntity> {
        val db = AppDatabase.getDatabase(context)
        val alatDao = db.alatDao()
        val list = dao.getAlatForKegiatan(kegiatanId)
        return list.map { item ->
            if (!item.isExternal && item.image_path.isNullOrBlank()) {
                val tool = alatDao.getAlatById(item.alatId)
                if (tool != null && !tool.image_path.isNullOrBlank()) {
                    item.copy(image_path = tool.image_path)
                } else {
                    item
                }
            } else {
                item
            }
        }
    }

    override suspend fun insertKegiatanLocal(
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String,
        tools: List<Pair<String, Int>>,
        externalTools: List<String>
    ): String {
        val kegiatanId = UUID.randomUUID().toString()
        val kegiatan = KegiatanEntity(
            id = kegiatanId,
            judul = judul,
            kategori = kategori,
            lokasi = lokasi,
            tanggal = tanggal,
            status = status,
            sync_status = "pending",
            pending_action = "create"
        )
        dao.insertKegiatan(kegiatan)

        // Insert internal tools
        val db = AppDatabase.getDatabase(context)
        val alatDao = db.alatDao()
        for ((toolId, qty) in tools) {
            val toolEntity = alatDao.getAlatById(toolId)
            val name = toolEntity?.name ?: "Unknown Tool"
            val category = toolEntity?.category ?: "General"
            val imagePath = toolEntity?.image_path
            
            val kegiatanAlat = KegiatanAlatEntity(
                id = UUID.randomUUID().toString(),
                kegiatanId = kegiatanId,
                alatId = toolId,
                name = name,
                category = category,
                qty = qty,
                isExternal = false,
                isReturned = false,
                image_path = imagePath,
                sync_status = "pending",
                pending_action = "create"
            )
            dao.insertKegiatanAlat(kegiatanAlat)

            // Adjust inventory quantity locally
            if (toolEntity != null) {
                val newAvailable = (toolEntity.available_qty - qty).coerceAtLeast(0)
                alatDao.insertAlat(toolEntity.copy(available_qty = newAvailable))
            }
        }

        // Insert external tools
        for (extToolRaw in externalTools) {
            val parts = extToolRaw.split("|")
            val name = parts.getOrNull(0) ?: ""
            val imagePath = parts.getOrNull(1)?.takeIf { it.isNotBlank() }

            val kegiatanAlat = KegiatanAlatEntity(
                id = UUID.randomUUID().toString(),
                kegiatanId = kegiatanId,
                alatId = "ext_${UUID.randomUUID()}",
                name = name,
                category = "Alat Luar",
                qty = 1,
                isExternal = true,
                isReturned = false,
                image_path = imagePath,
                sync_status = "pending",
                pending_action = "create"
            )
            dao.insertKegiatanAlat(kegiatanAlat)
        }

        // Try direct sync if online
        if (NetworkUtils.isOnline(context)) {
            try {
                // Emulate sync success (in real production, call KegiatanApiService)
                dao.insertKegiatan(kegiatan.copy(sync_status = "synced", pending_action = null))
                val alats = dao.getAlatForKegiatan(kegiatanId)
                for (a in alats) {
                    dao.insertKegiatanAlat(a.copy(sync_status = "synced", pending_action = null))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return kegiatanId
    }

    override suspend fun updateKegiatanAlatStatusLocal(kegiatanAlatId: String, isReturned: Boolean) {
        val db = AppDatabase.getDatabase(context)
        val alatDao = db.alatDao()
        
        // Find by querying all alat for kegiatan since we don't have getById for kegiatan_alat
        // Let's use SQLite readable database to query it quickly or find in list
        val allPending = dao.getPendingKegiatanAlat()
        // Instead, let's load all and filter
        // Actually, we can get the specific entity by looking it up
        val matches = dao.getPendingKegiatanAlat().find { it.id == kegiatanAlatId }
            ?: db.openHelper.readableDatabase.let {
                // Fallback query to find it
                var found: KegiatanAlatEntity? = null
                val cursor = it.query("SELECT * FROM kegiatan_alat WHERE id = '$kegiatanAlatId'")
                if (cursor.moveToFirst()) {
                    val kId = cursor.getString(cursor.getColumnIndexOrThrow("kegiatanId"))
                    val aId = cursor.getString(cursor.getColumnIndexOrThrow("alatId"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val cat = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                    val qty = cursor.getInt(cursor.getColumnIndexOrThrow("qty"))
                    val isExt = cursor.getInt(cursor.getColumnIndexOrThrow("isExternal")) == 1
                    val isRet = cursor.getInt(cursor.getColumnIndexOrThrow("isReturned")) == 1
                    val imgPath = try {
                        cursor.getString(cursor.getColumnIndexOrThrow("image_path"))
                    } catch (e: Exception) {
                        null
                    }
                    found = KegiatanAlatEntity(
                        id = kegiatanAlatId,
                        kegiatanId = kId,
                        alatId = aId,
                        name = name,
                        category = cat,
                        qty = qty,
                        isExternal = isExt,
                        isReturned = isRet,
                        image_path = imgPath
                    )
                }
                cursor.close()
                found
            }

        matches?.let { tool ->
            val updated = tool.copy(
                isReturned = isReturned,
                sync_status = "pending",
                pending_action = "update"
            )
            dao.updateKegiatanAlat(updated)

            // Adjust inventory back when returned
            if (!tool.isExternal) {
                val toolEntity = alatDao.getAlatById(tool.alatId)
                if (toolEntity != null) {
                    val delta = if (isReturned) tool.qty else -tool.qty
                    val newAvailable = (toolEntity.available_qty + delta).coerceIn(0, toolEntity.total_qty)
                    alatDao.insertAlat(toolEntity.copy(available_qty = newAvailable))
                }
            }

            if (NetworkUtils.isOnline(context)) {
                try {
                    dao.updateKegiatanAlat(updated.copy(sync_status = "synced", pending_action = null))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override suspend fun syncPendingKegiatan(): Boolean {
        var success = true
        try {
            val pendingKegiatan = dao.getPendingKegiatan()
            for (keg in pendingKegiatan) {
                // Try to sync with server
                // Since this is mock/concept, we mark synced
                dao.insertKegiatan(keg.copy(sync_status = "synced", pending_action = null))
            }

            // Sync pending tool return status changes
            // Since we can't easily query all from Room without getPending, we query via SQLite helper
            val db = AppDatabase.getDatabase(context)
            val sdb = db.openHelper.writableDatabase
            val cursor = sdb.query("SELECT * FROM kegiatan_alat WHERE sync_status = 'pending'")
            val pendingAlats = mutableListOf<KegiatanAlatEntity>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val kId = cursor.getString(cursor.getColumnIndexOrThrow("kegiatanId"))
                val aId = cursor.getString(cursor.getColumnIndexOrThrow("alatId"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val cat = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val qty = cursor.getInt(cursor.getColumnIndexOrThrow("qty"))
                val isExt = cursor.getInt(cursor.getColumnIndexOrThrow("isExternal")) == 1
                val isRet = cursor.getInt(cursor.getColumnIndexOrThrow("isReturned")) == 1
                val imgPath = try {
                    cursor.getString(cursor.getColumnIndexOrThrow("image_path"))
                } catch (e: Exception) {
                    null
                }
                pendingAlats.add(
                    KegiatanAlatEntity(id, kId, aId, name, cat, qty, isExt, isRet, "pending", "update", imgPath)
                )
            }
            cursor.close()

            for (pa in pendingAlats) {
                dao.updateKegiatanAlat(pa.copy(sync_status = "synced", pending_action = null))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            success = false
        }
        return success
    }

    override suspend fun deleteKegiatan(kegiatanId: String) {
        val db = AppDatabase.getDatabase(context)
        val alatDao = db.alatDao()
        val kegiatanAlatList = dao.getAlatForKegiatan(kegiatanId)
        
        for (ka in kegiatanAlatList) {
            if (!ka.isExternal && !ka.isReturned) {
                val toolEntity = alatDao.getAlatById(ka.alatId)
                if (toolEntity != null) {
                    val newAvailable = (toolEntity.available_qty + ka.qty).coerceAtMost(toolEntity.total_qty)
                    alatDao.insertAlat(toolEntity.copy(available_qty = newAvailable))
                }
            }
        }
        
        dao.deleteKegiatan(kegiatanId)
        dao.deleteKegiatanAlatForKegiatan(kegiatanId)
    }

    override suspend fun updateKegiatanLocal(
        id: String,
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String
    ) {
        val existing = dao.getKegiatanById(id)
        if (existing != null) {
            val updated = existing.copy(
                judul = judul,
                kategori = kategori,
                lokasi = lokasi,
                tanggal = tanggal,
                status = status,
                sync_status = "pending",
                pending_action = "update"
            )
            dao.updateKegiatan(updated)
            
            // Coba sinkronisasi langsung jika online
            if (NetworkUtils.isOnline(context)) {
                try {
                    dao.insertKegiatan(updated.copy(sync_status = "synced", pending_action = null))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

class FakeKegiatanRepository(private val context: Context) : KegiatanRepository {
    private val impl = KegiatanRepositoryImpl(
        com.example.perkapp.core.network.RetrofitClient.instance.create(KegiatanApiService::class.java),
        AppDatabase.getDatabase(context).kegiatanDao(),
        context
    )

    override suspend fun getInventoryStats() = impl.getInventoryStats()
    override suspend fun getKegiatanAktif() = impl.getKegiatanAktif()
    override suspend fun getUserInfo() = impl.getUserInfo()
    override suspend fun getAllKegiatanLocal() = impl.getAllKegiatanLocal()
    override suspend fun getAlatForKegiatanLocal(kegiatanId: String) = impl.getAlatForKegiatanLocal(kegiatanId)
    override suspend fun insertKegiatanLocal(
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String,
        tools: List<Pair<String, Int>>,
        externalTools: List<String>
    ) = impl.insertKegiatanLocal(judul, kategori, lokasi, tanggal, status, tools, externalTools)

    override suspend fun updateKegiatanAlatStatusLocal(kegiatanAlatId: String, isReturned: Boolean) =
        impl.updateKegiatanAlatStatusLocal(kegiatanAlatId, isReturned)

    override suspend fun syncPendingKegiatan() = impl.syncPendingKegiatan()

    override suspend fun deleteKegiatan(kegiatanId: String) = impl.deleteKegiatan(kegiatanId)

    override suspend fun updateKegiatanLocal(
        id: String,
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String
    ) = impl.updateKegiatanLocal(id, judul, kategori, lokasi, tanggal, status)
}