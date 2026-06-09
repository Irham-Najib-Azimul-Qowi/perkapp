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

/**
 * FUNGSI: MediaRepository
 * TUJUAN: Sumber data khusus untuk pengelolaan media (terutama gambar).
 *
 * ALUR LOGIKA PENGERJAAN:
 * Repository ini menangani penyimpanan gambar ke database lokal (mode offline)
 * serta proses upload gambar fisik (Multipart) ke server (mode online).
 */
class MediaRepository(
    // Layanan API untuk upload gambar ke server
    private val api: MediaApiService,
    // Akses ke tabel 'images' di database lokal
    private val dao: ImageDao,
    private val context: Context
) {
    /**
     * FUNGSI: getImagesForAlat
     * TUJUAN: Mengambil daftar semua gambar yang terkait dengan satu alat spesifik.
     */
    suspend fun getImagesForAlat(alatId: String): List<ImageEntity> {
        return dao.getImagesForEntity("alat", alatId)
    }

    /**
     * FUNGSI: saveImageLocally
     * TUJUAN: Menyimpan referensi gambar secara lokal (offline) ke dalam tabel SQLite.
     * ALUR LOGIKA PENGERJAAN:
     * Gambar ini akan ditandai dengan status "pending", yang artinya 
     * aplikasinya harus mengunggah (upload) gambar ini ke server nanti saat ada internet.
     *
     * @param entityType Jenis entitas (misal: "alat", "kegiatan", "user")
     * @param entityId ID dari entitas pemilik gambar tersebut
     * @param localPath Path/lokasi file fisik gambar di penyimpanan HP
     */
    suspend fun saveImageLocally(entityType: String, entityId: String, localPath: String) {
        val entity = ImageEntity(
            id = UUID.randomUUID().toString(),
            entity_type = entityType,
            entity_id = entityId,
            image_url = "", // Belum ada URL karena belum di-upload
            local_path = localPath,
            sync_status = "pending" // Menunggu untuk di-upload
        )
        dao.insertImage(entity)
    }

    /**
     * FUNGSI: uploadImage
     * TUJUAN: Mencoba mengunggah gambar fisik ke server.
     * ALUR LOGIKA PENGERJAAN:
     * Jika gagal (misal tidak ada internet atau server error), gambar tidak akan hilang,
     * melainkan tetap tersimpan di database lokal dengan status "pending" agar
     * bisa dicoba upload kembali di lain waktu.
     *
     * @return true jika berhasil terupload ke server, false jika gagal/offline
     */
    suspend fun uploadImage(entityType: String, entityId: String, file: File): Boolean {
        return try {
            // Membungkus file fisik ke dalam format RequestBody untuk upload multipart
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            
            // Data tambahan yang dikirim bersama gambar (tipe dan ID pemilik gambar)
            val typeBody = entityType.toRequestBody("text/plain".toMediaTypeOrNull())
            val idBody = entityId.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = api.uploadImage(body, typeBody, idBody)
            if (response.isSuccessful) {
                // Server membalas dengan link URL gambar yang baru saja di-upload
                response.body()?.data?.let { result ->
                    val entity = ImageEntity(
                        id = UUID.randomUUID().toString(),
                        entity_type = entityType,
                        entity_id = entityId,
                        image_url = result.image_url, // Simpan URL dari server
                        local_path = file.absolutePath,
                        sync_status = "synced" // Tandai bahwa gambar ini sudah aman di server
                    )
                    dao.insertImage(entity)
                }
                true
            } else {
                // Gagal upload dari sisi server (misal: format tidak didukung), simpan lokal dulu (pending)
                saveImageLocally(entityType, entityId, file.absolutePath)
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Error teknis (misal: koneksi terputus tiba-tiba), simpan lokal
            saveImageLocally(entityType, entityId, file.absolutePath)
            false
        }
    }

    /**
     * FUNGSI: tryUploadImage
     * TUJUAN: Fungsi internal untuk mencoba mengirim ulang satu gambar yang masih berstatus pending.
     * ALUR LOGIKA PENGERJAAN:
     * Digunakan secara massal oleh proses SyncWorker.
     */
    private suspend fun tryUploadImage(image: ImageEntity): Boolean {
        if (image.local_path.isBlank()) return false
        
        // Membaca file dari path lokal (URI/File path)
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
                    // Jika sukses, perbarui status gambar di database lokal menjadi "synced"
                    // dan simpan URL aslinya
                    dao.updateImage(
                        image.copy(
                            image_url = result.image_url,
                            sync_status = "synced"
                        )
                    )

                    // Jika gambar ini milik sebuah alat, perbarui juga path gambar di tabel utama 'alat'
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
     * FUNGSI: syncPendingImages
     * TUJUAN: Mensinkronisasi semua gambar yang belum terkirim ("pending") ke server.
     * ALUR LOGIKA PENGERJAAN:
     * Biasanya fungsi ini dipanggil di belakang layar oleh SyncWorker setiap kali
     * aplikasi mendeteksi ada koneksi internet.
     *
     * @return true jika SEMUA gambar berhasil di-upload, false jika ada satu/lebih yang gagal
     */
    suspend fun syncPendingImages(): Boolean {
        // Ambil semua entri gambar di database yang berstatus "pending"
        val pendingImages = dao.getPendingImages()
        var allSuccess = true

        // Coba upload satu per satu
        for (image in pendingImages) {
            if (!tryUploadImage(image)) {
                allSuccess = false
            }
        }
        return allSuccess
    }
}