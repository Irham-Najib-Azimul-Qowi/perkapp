package com.example.perkapp.dao

import androidx.room.*
import com.example.perkapp.model.KegiatanEntity
import com.example.perkapp.model.KegiatanAlatEntity

/**
 * KegiatanDao — Data Access Object untuk tabel 'kegiatan' dan 'kegiatan_alat'.
 *
 * Menyediakan fungsi-fungsi untuk membaca, menambah, mengubah, dan menghapus 
 * data riwayat peminjaman/kegiatan di database SQLite.
 */
@Dao
interface KegiatanDao {
    /**
     * FUNGSI: getAllKegiatan
     * TUJUAN: Mengambil seluruh riwayat peminjaman (kegiatan) dari database lokal, 
     * KECUALI kegiatan yang ditandai untuk dihapus (`pending_action = 'delete'`).
     * Ini memastikan UI tidak menampilkan data yang sebenarnya sudah dibuang saat offline.
     * @return List dari entitas kegiatan lokal.
     */
    @Query("SELECT * FROM kegiatan WHERE pending_action != 'delete' OR pending_action IS NULL")
    suspend fun getAllKegiatan(): List<KegiatanEntity>

    /**
     * FUNGSI: getKegiatanById
     * TUJUAN: Mencari rincian satu kegiatan berdasarkan UUID uniknya.
     * Berguna saat membuka halaman Detail Kegiatan.
     * @param id String UUID kegiatan yang dicari.
     * @return KegiatanEntity jika ditemukan, null jika tidak ada.
     */
    @Query("SELECT * FROM kegiatan WHERE id = :id")
    suspend fun getKegiatanById(id: String): KegiatanEntity?

    /**
     * FUNGSI: insertKegiatan
     * TUJUAN: Menyimpan data kegiatan baru ke dalam database SQLite. 
     * Jika ID kegiatan sudah ada, ia akan menimpa (REPLACE) data lama dengan yang baru.
     * @param kegiatan Objek entitas kegiatan yang ingin disimpan.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKegiatan(kegiatan: KegiatanEntity)

    /**
     * FUNGSI: updateKegiatan
     * TUJUAN: Memperbarui kolom-kolom pada kegiatan yang sudah ada di database 
     * (misal memperbarui status dari BERLANGSUNG menjadi SELESAI).
     * @param kegiatan Objek entitas kegiatan dengan data yang sudah diperbarui.
     */
    @Update
    suspend fun updateKegiatan(kegiatan: KegiatanEntity)

    /**
     * FUNGSI: deleteKegiatan
     * TUJUAN: Menghapus baris data kegiatan dari SQLite secara permanen berdasarkan ID.
     * @param id String UUID kegiatan yang akan dimusnahkan.
     */
    @Query("DELETE FROM kegiatan WHERE id = :id")
    suspend fun deleteKegiatan(id: String)

    /**
     * FUNGSI: getPendingKegiatan
     * TUJUAN: Mencari semua kegiatan yang dibuat/diubah saat HP tidak ada internet 
     * (`sync_status = 'pending'`). Data ini nantinya akan dikirim oleh SyncWorker.
     * @return List kegiatan yang tertunda.
     */
    @Query("SELECT * FROM kegiatan WHERE sync_status = 'pending'")
    suspend fun getPendingKegiatan(): List<KegiatanEntity>

    /**
     * FUNGSI: insertKegiatanAlat
     * TUJUAN: Menyimpan kaitan antara satu kegiatan dengan satu jenis alat (relasi Many-to-Many).
     * Mencatat jumlah barang (qty) yang dipinjam pada kegiatan tersebut.
     * @param alat Entitas Pivot relasi Kegiatan <-> Alat.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKegiatanAlat(alat: KegiatanAlatEntity)

    /**
     * FUNGSI: updateKegiatanAlat
     * TUJUAN: Memperbarui status peminjaman satu alat di satu kegiatan 
     * (contoh: user menandai alat A sudah dikembalikan, sehingga `isReturned = 1`).
     * @param alat Entitas Pivot relasi yang sudah diperbarui.
     */
    @Update
    suspend fun updateKegiatanAlat(alat: KegiatanAlatEntity)

    /**
     * FUNGSI: getAlatForKegiatan
     * TUJUAN: Menarik daftar seluruh alat (dan jumlahnya) yang disewa/dipinjam 
     * dalam satu ID kegiatan tertentu.
     * @param kegiatanId String UUID kegiatan peminjaman.
     * @return List dari relasi peminjaman alat.
     */
    @Query("SELECT * FROM kegiatan_alat WHERE kegiatanId = :kegiatanId")
    suspend fun getAlatForKegiatan(kegiatanId: String): List<KegiatanAlatEntity>

    /**
     * FUNGSI: getPendingKegiatanAlat
     * TUJUAN: Mencari semua detail penyewaan alat yang belum tersinkronisasi 
     * ke server Laravel.
     * @return List entitas relasi alat tertunda.
     */
    @Query("SELECT * FROM kegiatan_alat WHERE sync_status = 'pending'")
    suspend fun getPendingKegiatanAlat(): List<KegiatanAlatEntity>

    /**
     * FUNGSI: deleteKegiatanAlatForKegiatan
     * TUJUAN: Menghapus semua riwayat peminjaman alat jika kegiatan induknya dihapus.
     * (Meniru efek CASCADE DELETE di SQL).
     * @param kegiatanId String UUID kegiatan yang memayungi alat-alat ini.
     */
    @Query("DELETE FROM kegiatan_alat WHERE kegiatanId = :kegiatanId")
    suspend fun deleteKegiatanAlatForKegiatan(kegiatanId: String)

    /**
     * FUNGSI: getActiveBorrowingsForAlat
     * TUJUAN: Mengecek apakah barang/alat tertentu saat ini sedang dibawa/dipinjam 
     * orang dan belum dikembalikan (`isReturned = 0`). 
     * Logika ini dipakai agar Admin tidak bisa menghapus barang yang sedang dipakai dari Inventaris.
     * @param alatId String UUID barang fisik.
     * @return List relasi yang membuktikan barang tersebut masih dipinjam.
     */
    @Query("SELECT * FROM kegiatan_alat WHERE alatId = :alatId AND isReturned = 0")
    suspend fun getActiveBorrowingsForAlat(alatId: String): List<KegiatanAlatEntity>

    /**
     * FUNGSI: approveAlatForKegiatan
     * TUJUAN: Menandai sebuah form peminjaman telah di-ACC (disetujui) oleh Admin.
     * @param kegiatanId String UUID kegiatan peminjaman.
     */
    @Query("UPDATE kegiatan SET alat_approved = 1 WHERE id = :kegiatanId")
    suspend fun approveAlatForKegiatan(kegiatanId: String)
}
