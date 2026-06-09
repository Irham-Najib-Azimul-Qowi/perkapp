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

// Data class penampung hasil pembedahan (parsing) deskripsi gabungan kegiatan dari API
data class ParsedDescription(
    val peminjam: String, // String daftar nama peminjam alat
    val lokasi: String, // String lokasi kegiatan
    val kategori: String, // String kategori kegiatan
    val deskripsi: String, // String deskripsi/catatan kegiatan
    val isApproved: Boolean = false, // Status persetujuan peminjaman alat
    val returnedTools: List<String> = emptyList() // List ID alat-alat yang sudah dikembalikan
)

// Fungsi pembantu untuk membedah string deskripsi gabungan dari server
fun parseDescription(fullDesc: String?): ParsedDescription {
    // Jika deskripsi kosong/null, kembalikan objek parsed kosong
    if (fullDesc == null) return ParsedDescription("", "", "", "")
    var peminjam = "" // Inisialisasi string peminjam
    var lokasi = "" // Inisialisasi string lokasi
    var kategori = "" // Inisialisasi string kategori
    var deskripsi = "" // Inisialisasi string deskripsi
    var isApproved = false // Inisialisasi status approval default false
    val returnedTools = mutableListOf<String>() // Inisialisasi list penampung alat dikembalikan
    
    val lines = fullDesc.split("\n") // Memisahkan deskripsi baris per baris
    for (line in lines) { // Perulangan setiap baris deskripsi
        val trimmed = line.trim() // Menghapus spasi di awal dan akhir baris
        when { // Pencocokan awalan baris
            trimmed.startsWith("Peminjam: ") -> peminjam = trimmed.removePrefix("Peminjam: ") // Mengambil data nama peminjam
            trimmed.startsWith("Lokasi: ") -> lokasi = trimmed.removePrefix("Lokasi: ") // Mengambil data lokasi kegiatan
            trimmed.startsWith("Kategori: ") -> kategori = trimmed.removePrefix("Kategori: ") // Mengambil data kategori kegiatan
            trimmed.startsWith("StatusAlat: ") -> { // Mengecek status persetujuan peminjaman alat
                val statusStr = trimmed.removePrefix("StatusAlat: ").trim() // Membaca teks status alat
                if (statusStr.equals("Approved", ignoreCase = true)) { // Jika statusnya adalah Approved
                    isApproved = true // Set status persetujuan menjadi true
                }
            }
            trimmed.startsWith("ReturnedTools: ") -> { // Mengecek daftar alat yang telah dikembalikan
                val toolsStr = trimmed.removePrefix("ReturnedTools: ") // Membaca teks daftar alat
                if (toolsStr.isNotBlank()) { // Jika daftar alat tidak kosong
                    returnedTools.addAll(toolsStr.split(",").map { it.trim() }) // Masukkan ke list dengan menghapus spasi
                }
            }
            trimmed.startsWith("Deskripsi: ") -> deskripsi = trimmed.removePrefix("Deskripsi: ") // Mengambil data uraian deskripsi
            else -> { // Jika baris tidak memiliki tag di atas
                if (trimmed.isNotBlank()) { // Jika baris tidak kosong
                    if (deskripsi.isEmpty()) { // Jika deskripsi utama masih kosong
                        deskripsi = trimmed // Isi dengan baris ini
                    } else {
                        deskripsi += "\n$trimmed" // Gabungkan baris ini dengan baris deskripsi sebelumnya
                    }
                }
            }
        }
    }
    // Mengembalikan objek ParsedDescription yang telah diisi lengkap
    return ParsedDescription(peminjam, lokasi, kategori, deskripsi, isApproved, returnedTools)
}
/**
 * FUNGSI: KegiatanRepository
 * TUJUAN: Antarmuka (Interface) untuk sumber data fitur Kegiatan.
 *
 * Mendefinisikan aturan dan fungsi apa saja yang bisa dipanggil oleh ViewModel
 * terkait kegiatan (peminjaman, event, dll).
 */
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

/**
 * FUNGSI: KegiatanRepositoryImpl
 * TUJUAN: Implementasi nyata dari KegiatanRepository.
 *
 * ALUR LOGIKA PENGERJAAN:
 * Kelas ini menggabungkan dua sumber data utama:
 * 1. Remote (API server via Retrofit) — untuk sinkronisasi ke cloud
 * 2. Local (Room Database via DAO) — untuk offline-first (bisa dipakai tanpa internet)
 *
 * Aturan utama: Semua data dikembalikan dari database lokal (single source of truth).
 * Server hanya digunakan untuk update/sync data di balik layar.
 *
 * @param apiService "Kurir" untuk mengirim/menerima data dari server
 * @param dao "Pintu masuk" ke tabel kegiatan di database lokal SQLite
 * @param context Konteks aplikasi (untuk cek status koneksi jaringan)
 */
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

    // Membantu memformat tanggal Laravel (YYYY-MM-DD) -> lokal (DD/MM/YYYY)
    private fun formatFromLaravelDate(laravelDate: String?): String {
        // Jika null atau kosong, default tanggal ke 01/01/2026
        if (laravelDate.isNullOrBlank()) return "01/01/2026"
        // Membersihkan string tanggal dari jam jika ada (split spasi)
        val cleanDate = laravelDate.split(" ").firstOrNull() ?: laravelDate
        // Memecah berdasarkan tanda strip (-)
        val parts = cleanDate.split("-")
        // Jika ada 3 bagian format (tahun, bulan, hari)
        if (parts.size == 3) {
            // Gabungkan menjadi hari/bulan/tahun
            return "${parts[2]}/${parts[1]}/${parts[0]}"
        }
        // Jika format lain, kembalikan teks aslinya
        return cleanDate
    }

    // Memetakan status lokal ke status server (Laravel)
    private fun mapToLaravelStatus(localStatus: String): String {
        // Cek nama status lokal
        return when (localStatus.uppercase()) {
            "BERLANGSUNG" -> "ongoing" // ongoing untuk berlangsung
            "SELESAI" -> "completed" // completed untuk selesai
            else -> "draft" // draft jika status draf
        }
    }

    // Memetakan status server (Laravel) ke status lokal
    private fun mapFromLaravelStatus(laravelStatus: String?): String {
        // Cek nilai status laravel
        return when (laravelStatus?.lowercase()) {
            "ongoing" -> "BERLANGSUNG" // ongoing jadi berlangsung
            "completed" -> "SELESAI" // completed jadi selesai
            else -> "DRAFT" // default draft
        }
    }

    // Mengambil status statistik jumlah barang dipinjam, tersedia, dan antrean sync
    override suspend fun getInventoryStats(): Result<InventoryStats> {
        // Jalankan block try catch aman
        return try {
            // Mengambil instance database lokal
            val db = AppDatabase.getDatabase(context)
            // Mengambil DAO alat
            val alatDao = db.alatDao()
            // Mengambil semua data alat
            val allAlat = alatDao.getAllAlat()
            
            // Menjumlahkan seluruh stok alat tersedia
            val availableCount = allAlat.sumOf { it.available_qty }
            // Menghitung jumlah alat dengan status sync pending
            val pendingSyncCount = allAlat.count { it.sync_status == "pending" }
            
            // Query langsung ke SQLite untuk jumlah alat yang belum dikembalikan
            val allKegiatanAlat = db.openHelper.readableDatabase.compileStatement(
                "SELECT COUNT(*) FROM kegiatan_alat WHERE isReturned = 0"
            ).simpleQueryForLong().toInt()

            // Kembalikan status sukses beserta objek InventoryStats
            Result.success(
                InventoryStats(
                    borrowedCount = allKegiatanAlat,
                    availableCount = availableCount,
                    pendingSyncCount = pendingSyncCount
                )
            )
        } catch (e: Exception) { // Tangkap error jika terjadi exception
            e.printStackTrace() // Cetak stack trace error
            // Kembalikan default status sukses berisi 0
            Result.success(InventoryStats(borrowedCount = 0, availableCount = 0, pendingSyncCount = 0))
        }
    }

    /**
     * FUNGSI: getKegiatanAktif
     * TUJUAN: Mengambil daftar kegiatan yang sedang aktif/berlangsung.
     *
     * ALUR LOGIKA PENGERJAAN:
     * 1. Jika online, ambil data terbaru dari server (sync down).
     * 2. Simpan/timpa data lokal dengan data server (kecuali yang statusnya pending/punya perubahan lokal).
     * 3. Hapus kegiatan lokal jika di server sudah dihapus.
     * 4. Terakhir, selalu kembalikan daftar kegiatan dari database lokal.
     *
     * @return Daftar kegiatan dalam bentuk domain model (Kegiatan).
     */
    override suspend fun getKegiatanAktif(): Result<List<Kegiatan>> {
        // Jika internet tersedia, ambil data dari server dulu agar data selalu up-to-date
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
                                    isReturned = kegDto.status == "completed" || parsed.returnedTools.contains("${kegDto.id}_${alatDto.id}"),
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
                val allTools = dao.getAlatForKegiatan(kegiatanId)
                val returnedIds = allTools.filter { it.isReturned }.map { it.id }
                val returnedStr = if (returnedIds.isNotEmpty()) "\nReturnedTools: ${returnedIds.joinToString(",")}" else ""
                
                // Include StatusAlat: Approved in the description sent to server
                val desc = "Peminjam: ${existing.peminjam}\nLokasi: ${existing.lokasi}\nKategori: ${existing.kategori}\nStatusAlat: Approved${returnedStr}\nDeskripsi: ${existing.deskripsi}"
                
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

    /**
     * FUNGSI: insertKegiatanLocal
     * TUJUAN: Menambahkan kegiatan baru berserta daftar alat yang dipinjam.
     *
     * ALUR LOGIKA PENGERJAAN (Offline-first):
     * 1. Buat ID acak lokal dan simpan kegiatan ke Room Database dengan status "pending create".
     * 2. Simpan daftar alat (internal & eksternal) ke tabel `kegiatan_alat` dengan status "pending create".
     * 3. Kurangi jumlah stok alat yang tersedia (`available_qty`) di tabel alat secara lokal.
     * 4. Jika sedang online, langsung coba kirim semua data ini ke server.
     *
     * @return ID dari kegiatan yang baru dibuat.
     */
    override suspend fun insertKegiatanLocal(
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String,
        peminjam: String,
        deskripsi: String,
        tools: List<Pair<String, Int>>, // Daftar ID alat internal dan jumlahnya
        externalTools: List<String>     // Daftar alat dari luar (bukan milik inventaris)
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

    /**
     * FUNGSI: updateKegiatanAlatStatusLocal
     * TUJUAN: Memperbarui status peminjaman satu alat di dalam sebuah kegiatan.
     * Dipanggil saat user mencentang/menghapus centang "Dikembalikan" pada alat.
     *
     * ALUR LOGIKA PENGERJAAN:
     * 1. Ubah status `isReturned` di database lokal menjadi true/false.
     * 2. Sesuaikan stok alat di tabel inventaris utama (tambah stok jika dikembalikan).
     * 3. Jika online, langsung laporkan pembaruan ini ke server (masuk ke string deskripsi).
     */
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
                    val kegiatan = dao.getKegiatanById(tool.kegiatanId)
                    if (kegiatan != null) {
                        val allTools = dao.getAlatForKegiatan(tool.kegiatanId)
                        val returnedIds = allTools.filter { it.isReturned }.map { it.id }
                        val returnedStr = if (returnedIds.isNotEmpty()) "\nReturnedTools: ${returnedIds.joinToString(",")}" else ""
                        val approvalTag = if (kegiatan.alat_approved) "\nStatusAlat: Approved" else ""
                        val desc = "Peminjam: ${kegiatan.peminjam}\nLokasi: ${kegiatan.lokasi}\nKategori: ${kegiatan.kategori}${approvalTag}${returnedStr}\nDeskripsi: ${kegiatan.deskripsi}"
                        
                        apiService.updateKegiatan(
                            id = kegiatan.id,
                            request = UpdateKegiatanRequest(
                                name = kegiatan.judul,
                                description = desc,
                                date = formatToLaravelDate(kegiatan.tanggal),
                                status = mapToLaravelStatus(kegiatan.status)
                            )
                        )
                    }
                    dao.updateKegiatanAlat(updated.copy(sync_status = "synced", pending_action = null))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * FUNGSI: syncPendingKegiatan
     * TUJUAN: Mengeksekusi antrean perubahan (pending_action) untuk disinkronisasi ke server.
     *
     * ALUR LOGIKA PENGERJAAN:
     * "Tukang pos" ini akan mengecek tabel Kegiatan dan Kegiatan_Alat:
     * - "create" → Kirim POST untuk data baru
     * - "update" → Kirim PUT/PATCH untuk perubahan data
     * - "delete" → Kirim DELETE
     *
     * Jika sukses dikirim, status "pending" diubah menjadi "synced".
     *
     * @return true jika semua antrean berhasil dikirim, false jika ada yang gagal/error
     */
    override suspend fun syncPendingKegiatan(): Boolean {
        if (!NetworkUtils.isOnline(context)) return false
        var success = true
        try {
            val pendingKegiatan = dao.getPendingKegiatan()
            for (keg in pendingKegiatan) {
                if (keg.pending_action == "create" || keg.pending_action == "update") {
                    val allTools = dao.getAlatForKegiatan(keg.id)
                    val returnedIds = allTools.filter { it.isReturned }.map { it.id }
                    val returnedStr = if (returnedIds.isNotEmpty()) "\nReturnedTools: ${returnedIds.joinToString(",")}" else ""
                    val approvalTag = if (keg.alat_approved) "\nStatusAlat: Approved" else ""
                    val desc = "Peminjam: ${keg.peminjam}\nLokasi: ${keg.lokasi}\nKategori: ${keg.kategori}${approvalTag}${returnedStr}\nDeskripsi: ${keg.deskripsi}"
                    
                    if (keg.pending_action == "create") {
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
                    } else {
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
                    if (pa.pending_action == "create") {
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
                    } else if (pa.pending_action == "update") {
                        try {
                            val kegiatan = dao.getKegiatanById(pa.kegiatanId)
                            if (kegiatan != null) {
                                val allTools = dao.getAlatForKegiatan(pa.kegiatanId)
                                val returnedIds = allTools.filter { it.isReturned }.map { it.id }
                                val returnedStr = if (returnedIds.isNotEmpty()) "\nReturnedTools: ${returnedIds.joinToString(",")}" else ""
                                val approvalTag = if (kegiatan.alat_approved) "\nStatusAlat: Approved" else ""
                                val desc = "Peminjam: ${kegiatan.peminjam}\nLokasi: ${kegiatan.lokasi}\nKategori: ${kegiatan.kategori}${approvalTag}${returnedStr}\nDeskripsi: ${kegiatan.deskripsi}"
                                
                                val response = apiService.updateKegiatan(
                                    id = kegiatan.id,
                                    request = UpdateKegiatanRequest(
                                        name = kegiatan.judul,
                                        description = desc,
                                        date = formatToLaravelDate(kegiatan.tanggal),
                                        status = mapToLaravelStatus(kegiatan.status)
                                    )
                                )
                                if (response.success) {
                                    dao.updateKegiatanAlat(pa.copy(sync_status = "synced", pending_action = null))
                                } else {
                                    success = false
                                }
                            } else {
                                dao.updateKegiatanAlat(pa.copy(sync_status = "synced", pending_action = null))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            success = false
                        }
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

    /**
     * Menghapus kegiatan beserta alat-alat yang dipinjam di dalamnya.
     *
     * Jika dihapus, stok alat yang belum dikembalikan akan dikembalikan
     * ke inventaris utama (available_qty bertambah).
     *
     * Penghapusan menggunakan "soft-delete" lokal (tandai "pending delete")
     * agar SyncWorker tahu harus menghapusnya di server nanti saat online.
     */
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
                    val allTools = dao.getAlatForKegiatan(id)
                    val returnedIds = allTools.filter { it.isReturned }.map { it.id }
                    val returnedStr = if (returnedIds.isNotEmpty()) "\nReturnedTools: ${returnedIds.joinToString(",")}" else ""
                    val approvalTag = if (updated.alat_approved) "\nStatusAlat: Approved" else ""

                    val desc = "Peminjam: $peminjam\nLokasi: $lokasi\nKategori: $kategori${approvalTag}${returnedStr}\nDeskripsi: $deskripsi"
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

/**
 * FakeKegiatanRepository — Implementasi palsu untuk keperluan preview (Jetpack Compose) atau testing.
 *
 * Meneruskan semua panggilan ke KegiatanRepositoryImpl, 
 * sering dipakai agar parameter ViewModel tidak error saat dirender di mode Preview.
 */
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