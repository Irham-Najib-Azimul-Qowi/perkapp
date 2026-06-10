package com.example.perkapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.perkapp.model.ImageEntity

/**
 * FUNGSI: ImageDao
 * TUJUAN: Data Access Object untuk tabel 'images'.
 * 
 * ALUR LOGIKA PENGERJAAN:
 * Bertugas mengurus data gambar-gambar secara lokal, baik gambar yang sudah 
 * tersimpan rapi dari server (URL) maupun gambar foto langsung dari HP 
 * yang masih tertahan belum bisa di-upload.
 */
@Dao
interface ImageDao {
    /**
     * FUNGSI: getImagesForEntity
     * TUJUAN: Mengambil seluruh kumpulan gambar yang berafiliasi dengan satu item tertentu.
     * Sistem tabel ini dibuat generik (`entity_type` dan `entity_id`) agar satu tabel gambar 
     * bisa dipakai lintas fitur (contoh: gambar profil user, gambar alat, gambar tempat acara).
     * @param type String penanda tabel pemilik (contoh: "alat", "user").
     * @param entityId String UUID dari barang/user yang bersangkutan.
     * @return List entitas gambar yang terkait dengan item tersebut.
     */
    @Query("SELECT * FROM images WHERE entity_type = :type AND entity_id = :entityId")
    suspend fun getImagesForEntity(type: String, entityId: String): List<ImageEntity>

    /**
     * FUNGSI: insertImage
     * TUJUAN: Mendaftarkan satu buah gambar baru ke dalam direktori lokal database.
     * File gambar fisik sebenarnya berada di folder galeri/penyimpanan HP, 
     * Room Database hanya menyimpan 'Path' (alamat) tempat gambar itu berada.
     * @param image Entitas gambar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    /**
     * FUNGSI: insertAllImages
     * TUJUAN: Mendaftarkan banyak kumpulan referensi gambar sekaligus secara bersamaan (Bulk Insert).
     * Terutama dipanggil pasca-sinkronisasi saat server mengirimkan tautan-tautan foto barang.
     * @param images List entitas gambar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllImages(images: List<ImageEntity>)

    /**
     * FUNGSI: deleteImage
     * TUJUAN: Mencabut (menghapus) referensi satu gambar dari memori database aplikasi berdasarkan ID uniknya.
     * @param id String UUID gambar.
     */
    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImage(id: String)

    /**
     * FUNGSI: getPendingImages
     * TUJUAN: Melakukan inspeksi untuk menemukan gambar-gambar hasil foto kamera HP user
     * yang sedang mengantre (tertahan) karena tidak ada internet (status = 'pending').
     * Fungsi ini adalah pondasi sistem unggah-gambar latar belakang (Background Image Uploader).
     * @return List antrean gambar yang belum di-upload.
     */
    @Query("SELECT * FROM images WHERE sync_status = 'pending'")
    suspend fun getPendingImages(): List<ImageEntity>

    /**
     * FUNGSI: updateImage
     * TUJUAN: Menyimpan hasil modifikasi rincian suatu gambar.
     * Paling sering dipakai oleh `SyncWorker` ketika foto yang antre berhasil terunggah (upload),
     * statusnya akan diubah dari 'pending' menjadi 'synced', dan `image_url`-nya diisi dengan 
     * tautan asli dari server (AWS/Cloudinary/Hosting).
     * @param image Entitas gambar yang sudah termodifikasi datanya.
     */
    @Update
    suspend fun updateImage(image: ImageEntity)

    /**
     * FUNGSI: getImageByUrl
     * TUJUAN: Mengecek apakah gambar dari tautan (URL) web server sudah diregistrasi sebelumnya 
     * ke dalam database lokal, agar aplikasi tidak perlu mengunduh data berulang kali.
     * @param imageUrl String tautan alamat internet dari gambar.
     * @return Entitas gambar bila URL-nya sudah tercatat, atau null jika belum.
     */
    @Query("SELECT * FROM images WHERE image_url = :imageUrl LIMIT 1")
    suspend fun getImageByUrl(imageUrl: String): ImageEntity?
}
