/**
 * File: AppModule.kt
 *
 * FUNGSI UTAMA:
 * File ini adalah modul konfigurasi Hilt (Dependency Injection) yang mendefinisikan
 * CARA MEMBUAT setiap dependensi (komponen) yang dibutuhkan oleh aplikasi.
 *
 * PENJELASAN MENDALAM:
 * Dalam pola Dependency Injection, kelas-kelas tidak membuat sendiri dependensinya.
 * Sebaliknya, mereka "meminta" (melalui @Inject) dan Hilt yang akan menyediakannya.
 * AppModule memberi tahu Hilt: "Jika ada yang butuh KegiatanDao, buatkan dengan cara ini."
 *
 * Anotasi penting:
 * - @Module: Menandai kelas ini sebagai penyedia dependensi untuk Hilt
 * - @InstallIn(SingletonComponent): Dependensi di sini hidup selama aplikasi berjalan (singleton)
 * - @Provides: Menandai fungsi sebagai "resep" untuk membuat suatu objek
 * - @Singleton: Menjamin hanya ada SATU instance yang dibuat (hemat memori)
 *
 * PERAN DALAM ARSITEKTUR:
 * AppModule → menyediakan AppDatabase, DAO, ApiService, Repository
 * → ViewModel menerima Repository via @Inject → Screen menggunakan ViewModel
 */
package com.example.perkapp.di

import android.content.Context
import com.example.perkapp.database.AppDatabase
import com.example.perkapp.dao.KegiatanDao
import com.example.perkapp.dao.AlatDao
import com.example.perkapp.repository.KegiatanRepository
import com.example.perkapp.repository.KegiatanRepositoryImpl
import com.example.perkapp.network.KegiatanApiService
import com.example.perkapp.network.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule — Modul Hilt yang menyediakan seluruh dependensi tingkat aplikasi.
 *
 * Setiap fungsi @Provides di dalam modul ini adalah "pabrik" yang menghasilkan
 * satu instance komponen. Hilt secara otomatis memanggil fungsi-fungsi ini
 * ketika ada kelas yang membutuhkan dependensi tersebut.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * FUNGSI: provideAppDatabase
     * TUJUAN: Menyediakan instance database Room (SQLite) untuk seluruh aplikasi.
     * Singleton agar hanya ada satu koneksi database yang terbuka, sehingga menghemat 
     * memori dan mencegah korupsi data akibat multiple-access.
     *
     * @param context Context aplikasi yang disediakan oleh Hilt secara otomatis.
     * @return Instance AppDatabase yang sudah siap digunakan.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * FUNGSI: provideKegiatanDao
     * TUJUAN: Menyiapkan `KegiatanDao` — pintu masuk untuk mengeksekusi *query* SQLite 
     * khusus tabel `kegiatan`. Hilt akan memanggil `provideAppDatabase` lebih dulu untuk 
     * memenuhi parameter `database` secara otomatis (Injeksi Bertingkat).
     * 
     * @param database Basis data utama aplikasi.
     * @return Objek DAO Kegiatan.
     */
    @Provides
    @Singleton
    fun provideKegiatanDao(database: AppDatabase): KegiatanDao {
        return database.kegiatanDao()
    }

    /**
     * FUNGSI: provideAlatDao
     * TUJUAN: Menyediakan akses `AlatDao` untuk modul/fitur yang ingin membaca 
     * atau menulis inventaris barang secara lokal tanpa perlu tahu cara membuat objeknya.
     * 
     * @param database Basis data utama aplikasi.
     * @return Objek DAO Alat.
     */
    @Provides
    @Singleton
    fun provideAlatDao(database: AppDatabase): AlatDao {
        return database.alatDao()
    }

    /**
     * FUNGSI: provideKegiatanApiService
     * TUJUAN: Menyiapkan antarmuka API (Retrofit) agar repositori bisa 
     * mengunduh data log peminjaman dari server Laravel. `RetrofitClient.instance` 
     * sudah terpasang interceptor token di balik layar.
     * 
     * @return Antarmuka layanan web (KegiatanApiService).
     */
    @Provides
    @Singleton
    fun provideKegiatanApiService(): KegiatanApiService {
        return RetrofitClient.instance.create(KegiatanApiService::class.java)
    }

    /**
     * FUNGSI: provideKegiatanRepository
     * TUJUAN: Merakit `KegiatanRepository`, yang merupakan lapisan bisnis utama 
     * untuk memutuskan kapan harus membaca dari SQLite (offline) dan kapan 
     * menarik data dari API (online).
     * 
     * Hilt akan memilah dan mengumpulkan bahan-bahan (`apiService`, `kegiatanDao`, `context`)
     * dan memasukkannya ke dalam konstruktor `KegiatanRepositoryImpl`.
     * 
     * @param apiService Layanan jaringan internet.
     * @param kegiatanDao Layanan memori database lokal.
     * @param context Konteks utama Android.
     * @return Repositori Kegiatan yang siap pakai.
     */
    @Provides
    @Singleton
    fun provideKegiatanRepository(
        apiService: KegiatanApiService,
        kegiatanDao: KegiatanDao,
        @ApplicationContext context: Context
    ): KegiatanRepository {
        return KegiatanRepositoryImpl(apiService, kegiatanDao, context)
    }
}
