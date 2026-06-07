package com.example.perkapp.features.kegiatan.data

import android.content.Context
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.database.dao.KegiatanDao
import com.example.perkapp.core.database.entity.KegiatanEntity
import com.example.perkapp.core.database.entity.KegiatanAlatEntity
import com.example.perkapp.core.utils.NetworkUtils
import com.example.perkapp.features.kegiatan.api.*
import com.example.perkapp.features.kegiatan.domain.InventoryStats
import com.example.perkapp.features.kegiatan.domain.Kegiatan
import com.example.perkapp.features.kegiatan.domain.StatusKegiatan
import com.example.perkapp.features.kegiatan.domain.UserInfo
import java.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

data class ParsedDescription(val peminjam: String, val lokasi: String, val kategori: String, val deskripsi: String, val isApproved: Boolean = false)

fun parseDescription(fullDesc: String?): ParsedDescription {
    if (fullDesc == null) return ParsedDescription("", "", "", "")
    var peminjam = ""
    var lokasi = ""
    var kategori = ""
    var deskripsi = ""
    var isApproved = false
    
    val lines = fullDesc.split("\n")
    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("Peminjam: ") -> peminjam = trimmed.removePrefix("Peminjam: ")
            trimmed.startsWith("Lokasi: ") -> lokasi = trimmed.removePrefix("Lokasi: ")
            trimmed.startsWith("Kategori: ") -> kategori = trimmed.removePrefix("Kategori: ")
            trimmed.startsWith("StatusAlat: ") -> {
                val statusStr = trimmed.removePrefix("StatusAlat: ").trim()
                if (statusStr.equals("Approved", ignoreCase = true)) {
                    isApproved = true
                }
            }
            trimmed.startsWith("Deskripsi: ") -> deskripsi = trimmed.removePrefix("Deskripsi: ")
            else -> {
                if (trimmed.isNotBlank()) {
                    if (deskripsi.isEmpty()) {
                        deskripsi = trimmed
                    } else {
                        deskripsi += "\n$trimmed"
                    }
                }
            }
        }
    }
    return ParsedDescription(peminjam, lokasi, kategori, deskripsi, isApproved)
}

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
        peminjam: String,
        deskripsi: String,
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
        status: String,
        peminjam: String,
        deskripsi: String
    )
    suspend fun approveAlatForKegiatan(kegiatanId: String)
}

class KegiatanRepositoryImpl(
    private val apiService: KegiatanApiService,
    private val dao: KegiatanDao,
    private val context: Context
) : KegiatanRepository {

    private fun formatToLaravelDate(localDate: String): String {
        val parts = localDate.split("/")
        if (parts.size == 3) {
            return "${parts[2]}-${parts[1]}-${parts[0]}"
        }
        return localDate
    }

    private fun formatFromLaravelDate(laravelDate: String?): String {
        if (laravelDate.isNullOrBlank()) return "01/01/2026"
        val cleanDate = laravelDate.split(" ").firstOrNull() ?: laravelDate
        val parts = cleanDate.split("-")
        if (parts.size == 3) {
            return "${parts[2]}/${parts[1]}/${parts[0]}"
        }
        return cleanDate
    }

    private fun mapToLaravelStatus(localStatus: String): String {
        return when (localStatus.uppercase()) {
            "BERLANGSUNG" -> "ongoing"
            "SELESAI" -> "completed"
            else -> "draft"
        }
    }

    private fun mapFromLaravelStatus(laravelStatus: String?): String {
        return when (laravelStatus?.lowercase()) {
            "ongoing" -> "BERLANGSUNG"
            "completed" -> "SELESAI"
            else -> "DRAFT"
        }
    }

    override suspend fun getInventoryStats(): Result<InventoryStats> {
        return try {
            val db = AppDatabase.getDatabase(context)
            val alatDao = db.alatDao()
            val allAlat = alatDao.getAllAlat()
            
            val availableCount = allAlat.sumOf { it.available_qty }
            val pendingSyncCount = allAlat.count { it.sync_status == "pending" }
            
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
        // Fetch from server first if online to ensure latest activities show up to all users
        if (NetworkUtils.isOnline(context)) {
            try {
                val response = apiService.getSemuaKegiatan()
                if (response.success) {
                    val db = AppDatabase.getDatabase(context)
                    val alatDao = db.alatDao()
                    
                    // Collect all server kegiatan IDs to detect deletions
                    val serverKegiatanIds = mutableSetOf<String>()
                    
                    for (kegDto in response.data) {
                        // Skip kegiatan with "deleted" status from server
                        if (kegDto.status?.lowercase() == "deleted") {
                            // Remove from local DB if exists
                            dao.deleteKegiatanAlatForKegiatan(kegDto.id)
                            dao.deleteKegiatan(kegDto.id)
                            continue
                        }
                        
                        serverKegiatanIds.add(kegDto.id)
                        
                        val localCopy = dao.getKegiatanById(kegDto.id)
                        if (localCopy == null || localCopy.sync_status != "pending") {
                            val parsed = parseDescription(kegDto.description)
                            val kegiatanEntity = KegiatanEntity(
                                id = kegDto.id,
                                judul = kegDto.name ?: "",
                                kategori = parsed.kategori.ifBlank { "Umum" },
                                lokasi = parsed.lokasi,
                                tanggal = formatFromLaravelDate(kegDto.date),
                                status = mapFromLaravelStatus(kegDto.status),
                                peminjam = parsed.peminjam,
                                deskripsi = parsed.deskripsi,
                                sync_status = "synced",
                                pending_action = null,
                                created_by = kegDto.created_by,
                                alat_approved = parsed.isApproved || (localCopy?.alat_approved ?: false)
                            )
                            dao.insertKegiatan(kegiatanEntity)
                            
                            // Synced tools
                            kegDto.alats?.forEach { alatDto ->
                                val localTool = alatDao.getAlatById(alatDto.id)
                                val serverImagePath = alatDto.images?.firstOrNull()?.image_url ?: alatDto.image_path
                                val toolImagePath = localTool?.image_path ?: serverImagePath
                                val kaEntity = KegiatanAlatEntity(
                                    id = "${kegDto.id}_${alatDto.id}",
                                    kegiatanId = kegDto.id,
                                    alatId = alatDto.id,
                                    name = alatDto.name,
                                    category = alatDto.category,
                                    qty = alatDto.pivot?.qty ?: 1,
                                    isExternal = false,
                                    isReturned = kegDto.status == "completed",
                                    image_path = toolImagePath,
                                    sync_status = "synced",
                                    pending_action = null
                                )
                                dao.insertKegiatanAlat(kaEntity)
                            }
                        }
                    }
                    
                    // Remove local kegiatan that no longer exist on server (deleted by other users)
                    // Only remove synced ones, not locally pending ones
                    val allLocalKegiatan = dao.getAllKegiatan()
                    for (localKeg in allLocalKegiatan) {
                        if (localKeg.sync_status == "synced" && localKeg.id !in serverKegiatanIds) {
                            dao.deleteKegiatanAlatForKegiatan(localKeg.id)
                            dao.deleteKegiatan(localKeg.id)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return try {
            val entities = dao.getAllKegiatan().filter { it.status == "BERLANGSUNG" }
            val domainList = entities.map { entity ->
                Kegiatan(
                    id = entity.id,
                    kategori = entity.kategori,
                    judul = entity.judul,
                    lokasi = entity.lokasi,
                    labelWaktu = entity.tanggal,
                    progress = 0f,
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
        val db = AppDatabase.getDatabase(context)
        val userDao = db.userDao()
        val userEntity = userDao.getUser().first()
        val name = userEntity?.name ?: "User Perkapp"
        val role = userEntity?.role ?: "member"
        val id = userEntity?.id ?: ""
        return UserInfo(
            id = id,
            nama = name,
            sapaan = getSapaanBerdasarkanJam(),
            fotoUrl = "",
            role = role
        )
    }

    override suspend fun approveAlatForKegiatan(kegiatanId: String) {
        dao.approveAlatForKegiatan(kegiatanId)
        
        // Also sync the approval to server by updating the kegiatan
        val existing = dao.getKegiatanById(kegiatanId)
        if (existing != null && NetworkUtils.isOnline(context)) {
            try {
                // Include StatusAlat: Approved in the description sent to server
                val desc = "Peminjam: ${existing.peminjam}\nLokasi: ${existing.lokasi}\nKategori: ${existing.kategori}\nStatusAlat: Approved\nDeskripsi: ${existing.deskripsi}"
                
                apiService.updateKegiatan(
                    id = kegiatanId,
                    request = UpdateKegiatanRequest(
                        name = existing.judul,
                        description = desc,
                        date = formatToLaravelDate(existing.tanggal),
                        status = mapToLaravelStatus(existing.status)
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        peminjam: String,
        deskripsi: String,
        tools: List<Pair<String, Int>>,
        externalTools: List<String>
    ): String {
        val kegiatanId = UUID.randomUUID().toString()
        val db = AppDatabase.getDatabase(context)
        val currentUser = db.userDao().getUser().firstOrNull()
        val currentUserRole = currentUser?.role ?: "member"
        val currentUserId = currentUser?.id
        
        // Auto-add all admin users as peminjam
        var finalPeminjam = peminjam
        try {
            val registeredUserDao = db.registeredUserDao()
            val allUsers = registeredUserDao.getAllRegisteredUsers()
            val adminNames = allUsers
                .filter { it.role.lowercase() == "admin" }
                .map { it.name }
            
            val currentPeminjamList = finalPeminjam.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            for (adminName in adminNames) {
                if (currentPeminjamList.none { it.equals(adminName, ignoreCase = true) }) {
                    currentPeminjamList.add(adminName)
                }
            }
            finalPeminjam = currentPeminjamList.joinToString(", ")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // If creator is admin, alat is auto-approved
        val isCreatorAdmin = currentUserRole.lowercase() == "admin"
        
        val kegiatan = KegiatanEntity(
            id = kegiatanId,
            judul = judul,
            kategori = kategori,
            lokasi = lokasi,
            tanggal = tanggal,
            status = status,
            peminjam = finalPeminjam,
            deskripsi = deskripsi,
            sync_status = "pending",
            pending_action = "create",
            created_by = currentUserId,
            alat_approved = isCreatorAdmin
        )
        dao.insertKegiatan(kegiatan)

        // Insert internal tools
        val alatDao = db.alatDao()
        for ((toolId, qty) in tools) {
            val toolEntity = alatDao.getAlatById(toolId)
            val name = toolEntity?.name ?: "Unknown Tool"
            val category = toolEntity?.category ?: "General"
            val imagePath = toolEntity?.image_path
            
            val kegiatanAlat = KegiatanAlatEntity(
                id = "${kegiatanId}_${toolId}",
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
                val approvalTag = if (isCreatorAdmin) "\nStatusAlat: Approved" else ""
                val desc = "Peminjam: $finalPeminjam\nLokasi: $lokasi\nKategori: $kategori${approvalTag}\nDeskripsi: $deskripsi"
                val response = apiService.createKegiatan(
                    CreateKegiatanRequest(
                        id = kegiatanId,
                        name = judul,
                        description = desc,
                        date = formatToLaravelDate(tanggal),
                        status = mapToLaravelStatus(status)
                    )
                )
                if (response.success) {
                    dao.insertKegiatan(kegiatan.copy(sync_status = "synced", pending_action = null))
                    
                    // Sync internal tools
                    for ((toolId, qty) in tools) {
                        try {
                            apiService.addToolToKegiatan(
                                AddToolToKegiatanRequest(
                                    kegiatan_id = kegiatanId,
                                    alat_id = toolId,
                                    qty = qty
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    val alats = dao.getAlatForKegiatan(kegiatanId)
                    for (a in alats) {
                        dao.insertKegiatanAlat(a.copy(sync_status = "synced", pending_action = null))
                    }
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
        
        val matches = dao.getAlatForKegiatan("").find { it.id == kegiatanAlatId }
            ?: db.openHelper.readableDatabase.let {
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
                    // If returning a tool on server, it usually is handled by completing the activity,
                    // but we mark synced locally to ensure consistency.
                    dao.updateKegiatanAlat(updated.copy(sync_status = "synced", pending_action = null))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override suspend fun syncPendingKegiatan(): Boolean {
        if (!NetworkUtils.isOnline(context)) return false
        var success = true
        try {
            val pendingKegiatan = dao.getPendingKegiatan()
            for (keg in pendingKegiatan) {
                if (keg.pending_action == "create") {
                    val approvalTag = if (keg.alat_approved) "\nStatusAlat: Approved" else ""
                    val desc = "Peminjam: ${keg.peminjam}\nLokasi: ${keg.lokasi}\nKategori: ${keg.kategori}${approvalTag}\nDeskripsi: ${keg.deskripsi}"
                    val response = apiService.createKegiatan(
                        CreateKegiatanRequest(
                            id = keg.id,
                            name = keg.judul,
                            description = desc,
                            date = formatToLaravelDate(keg.tanggal),
                            status = mapToLaravelStatus(keg.status)
                        )
                    )
                    if (response.success) {
                        dao.insertKegiatan(keg.copy(sync_status = "synced", pending_action = null))
                    } else {
                        success = false
                    }
                } else if (keg.pending_action == "update") {
                    val approvalTag = if (keg.alat_approved) "\nStatusAlat: Approved" else ""
                    val desc = "Peminjam: ${keg.peminjam}\nLokasi: ${keg.lokasi}\nKategori: ${keg.kategori}${approvalTag}\nDeskripsi: ${keg.deskripsi}"
                    val response = apiService.updateKegiatan(
                        id = keg.id,
                        request = UpdateKegiatanRequest(
                            name = keg.judul,
                            description = desc,
                            date = formatToLaravelDate(keg.tanggal),
                            status = mapToLaravelStatus(keg.status)
                        )
                    )
                    if (response.success) {
                        dao.insertKegiatan(keg.copy(sync_status = "synced", pending_action = null))
                    } else {
                        success = false
                    }
                } else if (keg.pending_action == "delete") {
                    try {
                        val desc = "Peminjam: ${keg.peminjam}\nLokasi: ${keg.lokasi}\nKategori: ${keg.kategori}\nDeskripsi: ${keg.deskripsi}"
                        apiService.updateKegiatan(
                            id = keg.id,
                            request = UpdateKegiatanRequest(
                                name = keg.judul,
                                description = desc,
                                date = formatToLaravelDate(keg.tanggal),
                                status = "deleted"
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    val response = apiService.deleteKegiatan(keg.id)
                    if (response.success) {
                        dao.deleteKegiatan(keg.id)
                        dao.deleteKegiatanAlatForKegiatan(keg.id)
                    } else {
                        success = false
                    }
                }
            }

            // Sync pending tools
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
                    KegiatanAlatEntity(id, kId, aId, name, cat, qty, isExt, isRet, "pending", "create", imgPath)
                )
            }
            cursor.close()

            for (pa in pendingAlats) {
                if (!pa.isExternal) {
                    try {
                        val response = apiService.addToolToKegiatan(
                            AddToolToKegiatanRequest(
                                kegiatan_id = pa.kegiatanId,
                                alat_id = pa.alatId,
                                qty = pa.qty
                            )
                        )
                        if (response.success) {
                            dao.updateKegiatanAlat(pa.copy(sync_status = "synced", pending_action = null))
                        } else {
                            success = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        success = false
                    }
                } else {
                    dao.updateKegiatanAlat(pa.copy(sync_status = "synced", pending_action = null))
                }
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
        val existing = dao.getKegiatanById(kegiatanId)
        
        for (ka in kegiatanAlatList) {
            if (!ka.isExternal && !ka.isReturned) {
                val toolEntity = alatDao.getAlatById(ka.alatId)
                if (toolEntity != null) {
                    val newAvailable = (toolEntity.available_qty + ka.qty).coerceAtMost(toolEntity.total_qty)
                    alatDao.insertAlat(toolEntity.copy(available_qty = newAvailable))
                }
            }
        }
        
        // Mark as pending delete instead of deleting locally immediately to ensure sync works if offline
        if (existing != null) {
            val deletedEntity = existing.copy(
                status = "DELETED",
                sync_status = "pending",
                pending_action = "delete"
            )
            dao.updateKegiatan(deletedEntity)
        }

        if (NetworkUtils.isOnline(context)) {
            try {
                if (existing != null) {
                    // Update status to 'deleted' first on the server.
                    // This is a workaround for the backend soft-delete issue where soft-deleted items 
                    // still appear for other users and jump to the top. Changing status hides it.
                    val desc = "Peminjam: ${existing.peminjam}\nLokasi: ${existing.lokasi}\nKategori: ${existing.kategori}\nDeskripsi: ${existing.deskripsi}"
                    apiService.updateKegiatan(
                        id = kegiatanId,
                        request = UpdateKegiatanRequest(
                            name = existing.judul,
                            description = desc,
                            date = formatToLaravelDate(existing.tanggal),
                            status = "deleted"
                        )
                    )
                }
                val response = apiService.deleteKegiatan(kegiatanId)
                if (response.success) {
                    dao.deleteKegiatan(kegiatanId)
                    dao.deleteKegiatanAlatForKegiatan(kegiatanId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun updateKegiatanLocal(
        id: String,
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String,
        peminjam: String,
        deskripsi: String
    ) {
        val existing = dao.getKegiatanById(id)
        if (existing != null) {
            val updated = existing.copy(
                judul = judul,
                kategori = kategori,
                lokasi = lokasi,
                tanggal = tanggal,
                status = status,
                peminjam = peminjam,
                deskripsi = deskripsi,
                sync_status = "pending",
                pending_action = "update"
            )
            dao.updateKegiatan(updated)
            
            if (NetworkUtils.isOnline(context)) {
                try {
                    val desc = "Peminjam: $peminjam\nLokasi: $lokasi\nKategori: $kategori\nDeskripsi: $deskripsi"
                    val response = apiService.updateKegiatan(
                        id = id,
                        request = UpdateKegiatanRequest(
                            name = judul,
                            description = desc,
                            date = formatToLaravelDate(tanggal),
                            status = mapToLaravelStatus(status)
                        )
                    )
                    if (response.success) {
                        dao.insertKegiatan(updated.copy(sync_status = "synced", pending_action = null))
                    }
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
        peminjam: String,
        deskripsi: String,
        tools: List<Pair<String, Int>>,
        externalTools: List<String>
    ) = impl.insertKegiatanLocal(judul, kategori, lokasi, tanggal, status, peminjam, deskripsi, tools, externalTools)

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
        status: String,
        peminjam: String,
        deskripsi: String
    ) = impl.updateKegiatanLocal(id, judul, kategori, lokasi, tanggal, status, peminjam, deskripsi)

    override suspend fun approveAlatForKegiatan(kegiatanId: String) = impl.approveAlatForKegiatan(kegiatanId)
}