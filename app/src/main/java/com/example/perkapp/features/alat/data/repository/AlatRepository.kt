package com.example.perkapp.features.alat.data.repository

import android.content.Context
import com.example.perkapp.core.utils.NetworkUtils
import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * AlatRepository — Sumber data tunggal untuk fitur inventaris alat.
 *
 * Repository ini adalah "perantara" antara ViewModel dan sumber data.
 * Tugasnya: mengelola penggabungan data dari API server (online) 
 * dengan Room Database (offline). 
 *
 * Dengan pendekatan ini, aplikasi tetap bisa membaca, menambah, mengubah,
 * dan menghapus data alat meskipun HP sedang tidak ada sinyal (offline-first).
 */
class AlatRepository(
    // api: layanan HTTP untuk berkomunikasi dengan server
    private val api: AlatApiService,
    // dao: akses ke tabel alat di database lokal SQLite (Room)
    private val dao: AlatDao,
    // context: diperlukan untuk mengecek status jaringan atau akses database tambahan
    private val context: Context
) {
    companion object {
        // Mutex bertindak sebagai "gembok" untuk mencegah proses sinkronisasi ganda.
        // Jika ada proses sync yang sedang berjalan, proses lain harus menunggu sampai selesai,
        // mencegah data bentrok atau terkirim dua kali ke server.
        private val syncMutex = Mutex()
    }

    /**
     * Mengambil semua daftar alat.
     *
     * Cara kerja:
     * 1. Jika online: minta data terbaru dari server, simpan/perbarui database lokal, 
     *    kecuali data yang masih berstatus "pending" (punya perubahan lokal).
     * 2. Jika offline: langsung melewati proses API.
     * 3. Selalu mengembalikan daftar alat dari database lokal (single source of truth).
     *
     * @return List berisi AlatEntity dari database lokal.
     */
    suspend fun getAllAlat(): List<AlatEntity> {
        // Mengecek apakah perangkat sedang terhubung ke internet
        if (NetworkUtils.isOnline(context)) {
            try {
                // Request data terbaru dari server
                val response = api.getAllAlat()
                if (response.isSuccessful) {
                    response.body()?.data?.let { alatList ->
                        // Ambil data lokal yang masih punya perubahan tertunda (pending)
                        val pendingItems = dao.getPendingAlat()
                        // Simpan ID alat yang sedang pending
                        val pendingIds = pendingItems.map { it.id }.toSet()

                        val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                        val imageDao = db.imageDao()

                        // Mengonversi data dari server menjadi format database lokal
                        val entities = alatList.map { item ->
                            val existing = dao.getAlatById(item.id)
                            var imagePath = existing?.image_path
                            
                            val serverImage = item.images?.firstOrNull()?.image_url
                            if (!serverImage.isNullOrBlank()) {
                                imagePath = serverImage
                            } else if (imagePath.isNullOrBlank()) {
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
                                sync_status = "synced", // Tandai data ini sudah sama persis dengan server
                                image_path = imagePath,
                                pending_action = null
                            )
                        }
                        
                        // Insert/update data dari server HANYA JIKA data tersebut tidak sedang
                        // dimodifikasi secara lokal (pendingIds). Ini menjaga perubahan lokal tidak tertimpa.
                        entities.filter { it.id !in pendingIds }.forEach { entity ->
                            dao.insertAlat(entity)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // AUTO-REPAIR BLOCK: Memperbaiki masalah data gambar jika image_path kosong
        // tapi di tabel image ternyata tersimpan gambarnya.
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
                            dao.insertAlat(updated) // Simpan perbaikan path gambar
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Selalu kembalikan data yang ada di tabel SQLite lokal (baik offline maupun online)
        return dao.getAllAlat()
    }

    /**
     * Menyimpan alat baru.
     *
     * Cara kerja:
     * - Menyimpan data ke database lokal terlebih dulu dengan status "pending create".
     * - Menyimpan fotonya ke penyimpanan lokal.
     * - Jika internet tersedia, mencoba langsung mengirimkannya ke server.
     * - Jika tidak ada internet, operasi tetap sukses dan akan dikirim di lain waktu.
     */
    suspend fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
        // Karena belum dikirim ke server, buat ID lokal secara acak
        val localId = UUID.randomUUID().toString()
        val localEntity = AlatEntity(
            id = localId,
            name = name,
            category = category,
            total_qty = totalQty,
            available_qty = totalQty,
            condition = condition,
            sync_status = "pending", // Status pending karena belum tentu masuk ke server
            image_path = imagePath,
            pending_action = "create" // Aksi yang perlu dikerjakan nanti adalah 'create'
        )
        // Simpan alat baru ke database lokal (offline-first)
        dao.insertAlat(localEntity)

        // Simpan gambar secara lokal menggunakan MediaRepository
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

        // Langsung coba kirim ke server jika saat ini sedang online
        if (NetworkUtils.isOnline(context)) {
            try {
                syncPendingData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Memperbarui informasi alat yang sudah ada.
     *
     * Jika alat yang diupdate asalnya masih "pending create", statusnya tetap "create",
     * tetapi nilainya diperbarui. Jika asalnya dari server, status diubah menjadi "pending update".
     */
    suspend fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        val updated = alat.copy(
            name = request.name,
            category = request.category,
            total_qty = request.total_qty,
            condition = request.condition,
            sync_status = "pending", // Menandakan butuh sinkronisasi
            image_path = request.image_path,
            // Logika penting: Jika kita mengedit alat yang belum pernah sampai ke server (create),
            // aksi sinkronisasinya tetap 'create'. Tapi jika alat dari server diedit, aksinya 'update'.
            pending_action = if (alat.pending_action == "create") "create" else "update"
        )
        dao.updateAlat(updated)

        // Simpan referensi gambar ke lokal jika fotonya diganti dengan yang baru
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
                syncPendingData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Menghapus alat.
     *
     * Menangani kondisi "soft delete": alih-alih menghapus data sepenuhnya,
     * alat ditandai 'delete'. Ini memastikan aplikasi ingat untuk menghapus alat ini
     * di server saat koneksi internet kembali.
     */
    suspend fun deleteAlat(id: String) {
        val existing = dao.getAlatById(id)
        if (existing != null) {
            if (existing.pending_action == "create") {
                // Alat ini dibuat saat offline, dan dihapus saat offline juga.
                // Server sama sekali tidak tahu, jadi kita bisa langsung menghapusnya di lokal dengan bersih.
                dao.deleteAlat(id)
            } else {
                // Alat ini sudah ada di server, jadi hanya ubah penanda (flag) di database lokal
                // menjadi 'delete', sehingga nanti SyncWorker tahu alat ini harus dihapus dari server.
                dao.updateAlat(
                    existing.copy(
                        sync_status = "pending",
                        pending_action = "delete"
                    )
                )
            }
        }

        if (NetworkUtils.isOnline(context)) {
            try {
                syncPendingData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Mengambil satu data alat spesifik berdasarkan ID dari database lokal.
     */
    suspend fun getAlatById(id: String): AlatEntity? {
        return dao.getAlatById(id)
    }

    /**
     * Menjalankan seluruh proses sinkronisasi antrean tugas offline (pending_action).
     *
     * Berfungsi mirip "tukang pos" yang mengantarkan pesan tertunda saat jalan internet terbuka.
     * Akan membaca tabel SQLite, mencari yang statusnya "pending", dan mengeksekusi
     * create/update/delete ke API server satu per satu.
     *
     * @return true jika semua tugas sukses terkirim, false jika ada satu/lebih yang gagal
     */
    suspend fun syncPendingData(): Boolean {
        // syncMutex.withLock memastikan bahwa fungsi ini tidak dijalankan bersamaan
        // oleh beberapa coroutine (misal user klik-klik dan SyncWorker juga jalan bersamaan).
        return syncMutex.withLock {
            val pendingItems = dao.getPendingAlat() // Ambil antrean dari database lokal
            var allSuccess = true

            for (item in pendingItems) {
                try {
                    // Mengecek jenis aksi tertunda yang harus dijalankan
                    when (item.pending_action) {
                        "create" -> {
                            // Mengirim request untuk menambahkan alat baru ke server
                            val request = CreateAlatRequest(
                                name = item.name,
                                category = item.category,
                                total_qty = item.total_qty,
                                condition = item.condition
                            )
                            val response = api.createAlat(request)
                            if (response.isSuccessful) {
                                response.body()?.data?.let { apiAlat ->
                                    // Berhasil! Hapus data alat dengan ID acak lokal
                                    dao.deleteAlat(item.id)
                                    // Gantikan dengan alat yang punya ID resmi dari server (UUID dari backend)
                                    dao.insertAlat(
                                        AlatEntity(
                                            id = apiAlat.id,
                                            name = apiAlat.name,
                                            category = apiAlat.category,
                                            total_qty = apiAlat.total_qty,
                                            available_qty = apiAlat.available_qty,
                                            condition = apiAlat.condition,
                                            sync_status = "synced", // Status sekarang beres (synced)
                                            image_path = item.image_path,
                                            pending_action = null
                                        )
                                    )

                                    // Mengupdate ID tabel gambar: gambar yang asalnya menunjuk ke ID lokal acak
                                    // harus diubah agar menunjuk ke ID server yang baru
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
                                allSuccess = false // Gagal dari server, kita akan coba lagi lain kali
                            }
                        }
                        "update" -> {
                            // Mengirim perubahan pada alat yang sudah ada
                            val request = CreateAlatRequest(
                                name = item.name,
                                category = item.category,
                                total_qty = item.total_qty,
                                condition = item.condition,
                                image_path = item.image_path
                            )
                            val response = api.updateAlat(item.id, request)
                            if (response.isSuccessful) {
                                // Sukses diperbarui, hilangkan status pending
                                dao.updateAlat(
                                    item.copy(sync_status = "synced", pending_action = null)
                                )
                            } else {
                                allSuccess = false
                            }
                        }
                        "delete" -> {
                            // Mengirim permintaan untuk benar-benar menghapus alat dari server
                            val response = api.deleteAlat(item.id)
                            if (response.isSuccessful) {
                                // Server berhasil menghapus, jadi sekarang hapus juga dari database lokal selamanya
                                dao.deleteAlat(item.id)
                            } else {
                                allSuccess = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    allSuccess = false // Kalau jaringan tiba-tiba putus, tandai sebagai gagal
                }
            }
            
            // Sync all pending images at the end of alat sync
            // Mengantarkan antrean gambar-gambar alat ke server juga
            try {
                val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                val mediaApi = com.example.perkapp.core.network.RetrofitClient.instance.create(com.example.perkapp.features.media.api.MediaApiService::class.java)
                val mediaRepository = com.example.perkapp.features.media.data.MediaRepository(mediaApi, db.imageDao(), context)
                if (!mediaRepository.syncPendingImages()) {
                    allSuccess = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                allSuccess = false
            }
            
            allSuccess
        }
    }
}
