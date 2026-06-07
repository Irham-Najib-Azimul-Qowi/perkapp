# Panduan Pengujian & Simulasi Fitur Inventaris Alat — Bagian Najib

Selamat! Bagian **Najib (Fitur Inventaris Alat)** telah selesai 100% dan terpasang sebagai halaman utama saat aplikasi dibuka (`startDestination = "inventaris"`). 

Dokumen ini adalah **Panduan Pengujian (Testing Guide)** langkah demi langkah untuk mencoba seluruh fungsionalitas UI, Database Room, serta sinkronisasi API Retrofit ke server Cakra Manggala.

---

## 🗺️ Peta Alur Navigasi Saat Ini

Saat Anda menjalankan aplikasi di HP, navigasi diatur secara otomatis sebagai berikut:

```
                  ┌──────────────────────┐
                  │   Start Destination  │
                  │   InventarisScreen   │ ◄─────────────────────────┐
                  │ (Daftar inventaris)  │                           │
                  └──────────┬───────────┘                           │
                             │                                       │
            Klik Tombol (+)  │  Klik Salah Satu Item                 │ Tombol Kembali
                  FAB        │        Alat                           │ (Back)
                             ▼                                       │
                  ┌──────────────────────┐                           │
                  │  TambahAlatScreen    │                           │
                  │   (Form Tambah)      │                           │
                  └──────────┬───────────┘                           │
                             │ Klik                                  │
                             │ Simpan                                │
                             ▼                                       │
                  ┌──────────────────────┐                           │
                  │   DetailAlatScreen   │                           │
                  │ (Lihat Detail & Sync)├───────────────────────────┤
                  └──────────┬───────────┘                           │
                             │ Klik                                  │
                             │ Edit Alat                             │
                             ▼                                       │
                  ┌──────────────────────┐                           │
                  │    EditAlatScreen    │                           │
                  │ (Form Edit Inventaris)├───────────────────────────┘
                  └──────────────────────┘
```

---

## 🧪 Skenario Pengujian Sistem (Test Cases)

Silakan ikuti skenario pengujian di bawah ini untuk membuktikan bahwa fitur buatan Najib berfungsi dengan sempurna.

### Skenario 1: Pengujian Form Tambah Alat (Offline-First)
Skenario ini membuktikan bahwa aplikasi dapat menyimpan data secara lokal meskipun HP tidak memiliki koneksi internet.

1. **Matikan Internet HP**: Aktifkan *Flight Mode* (Mode Pesawat) atau matikan Wi-Fi dan Data Seluler pada HP Anda.
2. **Buka Aplikasi Perkapp**: Halaman utama **Inventaris Alat** akan terbuka (dalam kondisi kosong jika belum ada data).
3. **Buka Form Tambah**: Ketuk tombol melayang **(+)** di pojok kanan bawah. Anda akan diarahkan ke **TambahAlatScreen**.
4. **Isi Form**:
   *   Nama Alat: `Tenda Dome Singgalang`
   *   Kategori: `Outdoor`
   *   Jumlah: `4`
   *   Kondisi: `good`
5. **Simpan**: Ketuk tombol **Simpan**. Anda akan otomatis kembali ke halaman utama.
6. **Verifikasi Hasil**:
   *   Item `Tenda Dome Singgalang` kini muncul di daftar halaman utama.
   *   Ketuk item tersebut untuk masuk ke **DetailAlatScreen**.
   *   Perhatikan baris **Status Sync**. Statusnya harus bertuliskan **`pending`** (karena internet mati, data hanya disimpan di database lokal Room HP Anda).

---

### Skenario 2: Simulasi Sinkronisasi Otomatis ke Backend (Online Sync)
Skenario ini membuktikan bahwa sistem sinkronisasi otomatis bekerja saat internet kembali aktif.

1. **Aktifkan Internet HP**: Nyalakan kembali Wi-Fi atau Data Seluler di HP Anda.
2. **Buka Detail/Edit Alat**: 
   *   Masuk ke item `Tenda Dome Singgalang` yang berstatus `pending` tadi.
   *   Ketuk tombol **Edit Alat**, lalu ubah jumlahnya menjadi `5` atau biarkan saja, kemudian ketuk **Simpan Perubahan**.
3. **Verifikasi Sinkronisasi**:
   *   Aplikasi secara otomatis mengirimkan data tersebut ke API server Cakra Manggala di latar belakang menggunakan Retrofit.
   *   Jika rute API di server hosting Anda sudah dibersihkan cache-nya, status pada **DetailAlatScreen** akan otomatis berubah dari **`pending`** menjadi **`synced`**!

---

### Skenario 3: Membuka Fitur Edit
1. Pada halaman daftar alat, ketuk salah satu item.
2. Di halaman **Detail Alat**, ketuk tombol **Edit Alat** di bagian bawah.
3. Anda akan diarahkan ke **EditAlatScreen**, di mana form secara otomatis terisi dengan data alat yang lama.
4. Ubah Kondisi dari `good` menjadi `damaged`, lalu simpan.
5. Halaman detail akan langsung memperbarui info kondisi alat tersebut.

---

## 🛠️ Cara Mengintip Database Lokal (Room) via Android Studio

Untuk membuktikan secara nyata bahwa data Anda benar-benar tersimpan di dalam memori penyimpanan internal HP (SQLite Room), Anda dapat menggunakan fitur **Database Inspector** bawaan Android Studio:

1. Sambungkan HP ke laptop menggunakan kabel data.
2. Di Android Studio, lihat menu bagian bawah lalu klik tab **App Inspection**.
3. Pastikan proses aplikasi `com.example.perkapp` sedang terpilih di pojok kiri atas jendela App Inspection.
4. Pada tab **Database Inspector**, pilih database bernama **`perkapp_database`**.
5. Klik dua kali pada tabel **`alat`**.
6. Anda akan melihat baris data tabel SQLite secara langsung beserta nilai kolom `sync_status` (`synced` / `pending`) secara real-time!

---

> [!TIP]
> **Catatan Uji Coba**:  
> Jika saat pengujian *Online Sync* (Skenario 2) aplikasi sempat memakan waktu lama sebelum data berubah menjadi `synced`, jangan panik. Hal tersebut wajar karena Retrofit sedang menunggu respons timeout dari server hosting Anda. Pastikan server hosting Anda bebas dari masalah koneksi.
