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
     * FUNGSI: getAllAlat
     * TUJUAN: Mengambil dan merakit seluruh daftar barang inventaris. Berperan sebagai 
     * mekanisme *Single Source of Truth* di mana database lokal (SQLite) selalu menjadi 
     * prioritas bacaan agar aplikasi terasa instan.
     *
     * ALUR LOGIKA PENGERJAAN:
     * 1. Mengecek Internet: Bila sedang online, ia akan membajak *thread* untuk meminta data segar dari server.
     * 2. Resolusi Konflik: Membaca tabel `alat` SQLite untuk mencari barang-barang yang 
     *    memiliki status `pending` (belum sempat terkirim karena offline). 
     * 3. Menyimpan Data Segar: Ia menimpa (`Update`) atau menyisipkan (`Insert`) data 
     *    dari server ke tabel SQLite, **KECUALI** pada barang yang statusnya `pending`.
     *    (Hal ini mencegah hasil editan pengguna yang belum terkirim tiba-tiba ter-reset oleh server).
     * 4. Perbaikan Gambar (*Auto-Repair*): Memastikan setiap tautan (*path*) gambar cocok.
     * 5. Akhir: Apapun yang terjadi, kembalikan daftar barang langsung dari `AlatDao` (Database Lokal).
     *
     * @return List/Koleksi alat (`AlatEntity`).
     */
    suspend fun getAllAlat(): List<AlatEntity> {
        // 1. Cek apakah HP memiliki koneksi internet aktif saat ini
        if (NetworkUtils.isOnline(context)) {
            try {
                // 2. Kirim permintaan GET ke server untuk mengambil daftar alat terbaru
                val response = api.getAllAlat()
                // Jika server merespons sukses (HTTP 200)
                if (response.isSuccessful) {
                    response.body()?.data?.let { alatList ->
                        // 3. Tarik data alat lokal yang saat ini statusnya masih tertunda (pending)
                        val pendingItems = dao.getPendingAlat()
                        // Mengonversi daftar alat pending ke bentuk Set ID agar proses pencarian cepat
                        val pendingIds = pendingItems.map { it.id }.toSet()

                        // Mengakses database global aplikasi untuk membaca tabel gambar
                        val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                        val imageDao = db.imageDao()

                        // 4. Konversi data DTO dari server ke format Room Entity lokal
                        val entities = alatList.map { item ->
                            // Cari apakah alat dengan ID ini sudah ada di SQLite lokal
                            val existing = dao.getAlatById(item.id)
                            var imagePath = existing?.image_path
                            
                            // Gunakan alamat gambar dari server jika tersedia
                            val serverImage = item.images?.firstOrNull()?.image_url
                            if (!serverImage.isNullOrBlank()) {
                                imagePath = serverImage
                            } else if (imagePath.isNullOrBlank()) {
                                // Jika gambar di kolom entity kosong, coba ambil dari tabel image
                                val images = imageDao.getImagesForEntity("alat", item.id)
                                if (images.isNotEmpty()) {
                                    imagePath = images.first().image_url ?: images.first().local_path
                                }
                            }
                            
                            // Buat objek entitas lokal dengan penanda status tersinkronisasi (synced)
                            AlatEntity(
                                id = item.id,
                                name = item.name,
                                category = item.category,
                                total_qty = item.total_qty,
                                available_qty = item.available_qty,
                                condition = item.condition,
                                sync_status = "synced", // Status synced karena diambil langsung dari server
                                image_path = imagePath,
                                pending_action = null // Tidak ada aksi pending karena sudah sama dengan server
                            )
                        }
                        
                        // 5. Simpan data baru dari server ke SQLite lokal
                        // Filter agar data yang sedang dimodifikasi secara offline (pending) tidak tertimpa
                        entities.filter { it.id !in pendingIds }.forEach { entity ->
                            dao.insertAlat(entity)
                        }
                    }
                }
            } catch (e: Exception) {
                // Tampilkan stack trace jika terjadi kesalahan koneksi/parsing API
                e.printStackTrace()
            }
        }

        // 6. BLOK PERBAIKAN OTOMATIS: Memeriksa dan membetulkan link image_path jika kosong
        try {
            val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
            val imageDao = db.imageDao()
            val localAlats = dao.getAllAlat()
            localAlats.forEach { alat ->
                // Jika kolom path gambar kosong tapi di tabel image ternyata ada gambarnya
                if (alat.image_path.isNullOrBlank()) {
                    val images = imageDao.getImagesForEntity("alat", alat.id)
                    if (images.isNotEmpty()) {
                        val path = images.first().image_url ?: images.first().local_path
                        if (!path.isNullOrBlank()) {
                            // Perbarui kolom gambar alat
                            val updated = alat.copy(image_path = path)
                            dao.insertAlat(updated)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 7. Selalu kembalikan data dari database SQLite lokal (Single Source of Truth)
        return dao.getAllAlat()
    }

    suspend fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
        // 1. Buat ID acak lokal menggunakan UUID (karena belum terdaftar di database server)
        val localId = UUID.randomUUID().toString()
        // 2. Buat objek data alat lokal dengan flag pending agar disinkronkan nanti
        val localEntity = AlatEntity(
            id = localId,
            name = name,
            category = category,
            total_qty = totalQty,
            available_qty = totalQty,
            condition = condition,
            sync_status = "pending", // Status pending karena belum dikirim ke server
            image_path = imagePath,
            pending_action = "create" // Aksi tertunda: buat baru
        )
        // 3. Masukkan data ke SQLite lokal
        dao.insertAlat(localEntity)

        // 4. Jika ada gambar, simpan referensi file gambar tersebut ke database lokal gambar
        if (!imagePath.isNullOrBlank()) {
            try {
                val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
                val mediaApi = com.example.perkapp.core.network.RetrofitClient.instance.create(com.example.perkapp.features.media.api.MediaApiService::class.java)
                val imageDao = db.imageDao()
                val mediaRepository = com.example.perkapp.features.media.data.MediaRepository(mediaApi, imageDao, context)
                // Panggil repository media untuk mengurus penyimpanan gambar lokal
                mediaRepository.saveImageLocally("alat", localId, imagePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 5. Coba langsung kirim data baru ini ke server jika saat ini HP sedang online
        if (NetworkUtils.isOnline(context)) {
            try {
                syncPendingData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        // 1. Bikin objek data alat terupdate dengan status pending
        val updated = alat.copy(
            name = request.name,
            category = request.category,
            total_qty = request.total_qty,
            condition = request.condition,
            sync_status = "pending", // Butuh sinkronisasi ulang
            image_path = request.image_path,
            // Jika data ini belum pernah dikirim ke server (status pending action = create),
            // biarkan tetap 'create'. Tapi jika sudah ada di server, tandai sebagai 'update'.
            pending_action = if (alat.pending_action == "create") "create" else "update"
        )
        // 2. Perbarui baris data alat di database lokal SQLite
        dao.updateAlat(updated)

        // 3. Jika gambar diubah, simpan berkas gambar baru tersebut ke database lokal gambar
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

        // 4. Jika sedang online, segera sinkronisasikan perubahan ke server Laravel
        if (NetworkUtils.isOnline(context)) {
            try {
                syncPendingData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteAlat(id: String) {
        // 1. Cari data alat yang ingin dihapus di database lokal
        val existing = dao.getAlatById(id)
        if (existing != null) {
            // 2. Cek apakah alat dibuat saat offline dan belum masuk ke server
            if (existing.pending_action == "create") {
                // Cukup hapus langsung dari SQLite lokal, server tidak perlu diberi tahu karena belum pernah tahu
                dao.deleteAlat(id)
            } else {
                // Jika alat sudah ada di server, tandai data lokal dengan pending_action 'delete'
                // Data lokal tidak langsung dihapus agar SyncWorker nanti tahu bahwa ia harus mengirim request DELETE ke server
                dao.updateAlat(
                    existing.copy(
                        sync_status = "pending",
                        pending_action = "delete"
                    )
                )
            }
        }

        // 3. Jalankan sinkronisasi instan jika online
        if (NetworkUtils.isOnline(context)) {
            try {
                syncPendingData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getAlatById(id: String): AlatEntity? {
        // Mengambil entitas data alat dari SQLite berdasarkan ID lewat DAO
        return dao.getAlatById(id)
    }

    suspend fun syncPendingData(): Boolean {
        // Gunakan mutex agar proses sinkronisasi berjalan bergantian (thread-safe)
        return syncMutex.withLock {
            // 1. Ambil semua data alat lokal yang statusnya masih 'pending'
            val pendingItems = dao.getPendingAlat()
            var allSuccess = true

            // 2. Lakukan perulangan untuk memproses tiap data tertunda
            for (item in pendingItems) {
                try {
                    when (item.pending_action) {
                        "create" -> {
                            // Merakit DTO request data alat baru
                            val request = CreateAlatRequest(
                                name = item.name,
                                category = item.category,
                                total_qty = item.total_qty,
                                condition = item.condition
                            )
                            // Kirim request POST ke server Laravel
                            val response = api.createAlat(request)
                            if (response.isSuccessful) {
                                response.body()?.data?.let { apiAlat ->
                                    // Hapus baris data dengan ID lokal acak
                                    dao.deleteAlat(item.id)
                                    // Masukkan data baru dengan ID resmi UUID bentukan server
                                    dao.insertAlat(
                                        AlatEntity(
                                            id = apiAlat.id,
                                            name = apiAlat.name,
                                            category = apiAlat.category,
                                            total_qty = apiAlat.total_qty,
                                            available_qty = apiAlat.available_qty,
                                            condition = apiAlat.condition,
                                            sync_status = "synced", // Status sukses synced
                                            image_path = item.image_path,
                                            pending_action = null
                                        )
                                    )

                                    // Perbarui ID relasi di tabel gambar: ubah dari ID lokal acak menjadi ID resmi server
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
                                // Tandai gagal jika server menolak/error
                                allSuccess = false
                            }
                        }
                        "update" -> {
                            // Merakit DTO request edit
                            val request = CreateAlatRequest(
                                name = item.name,
                                category = item.category,
                                total_qty = item.total_qty,
                                condition = item.condition,
                                image_path = item.image_path
                            )
                            // Kirim request PUT ke server Laravel
                            val response = api.updateAlat(item.id, request)
                            if (response.isSuccessful) {
                                // Hapus status pending jika sukses
                                dao.updateAlat(
                                    item.copy(sync_status = "synced", pending_action = null)
                                )
                            } else {
                                allSuccess = false
                            }
                        }
                        "delete" -> {
                            // Kirim request DELETE ke server Laravel
                            val response = api.deleteAlat(item.id)
                            if (response.isSuccessful) {
                                // Jika server sukses menghapus, hapus permanen data lokal dari SQLite
                                dao.deleteAlat(item.id)
                            } else {
                                allSuccess = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    allSuccess = false // Tandai gagal jika putus koneksi di tengah jalan
                }
            }
            
            // 3. Sinkronisasikan berkas gambar-gambar yang terhubung ke server setelah data alat beres
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
            
            // Mengembalikan status keberhasilan seluruh antrean sync
            allSuccess
        }
    }
}
