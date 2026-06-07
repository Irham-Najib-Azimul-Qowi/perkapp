# Tutorial Mengatasi Kendala Running Aplikasi Perkapp ke HP — Part 2

Hebat sekali! Anda telah berhasil menyelesaikan dan menerapkan seluruh perbaikan kode dari tutorial sebelumnya dengan sempurna. File-file *typo* sudah terhapus, rute navigasi di `MainActivity.kt` sudah valid, dan *permission* di `AndroidManifest.xml` sudah benar.

Namun, saat ini proses *build* dan *running* ke HP masih gagal dengan pesan error yang sangat singkat:
```
* What went wrong:
26.0.1
```

Dokumen **Part 2** ini akan menganalisa secara mendalam mengapa error tersebut muncul dan memberikan tutorial cara mengatasinya.

---

## 🔍 Analisa Akar Masalah (Root Cause)

Berdasarkan pengecekan lingkungan (*environment*) Gradle pada sistem Linux Anda, ditemukan konfigurasi berikut:
- **Launcher JVM**: `26.0.1 (Arch Linux 26.0.1)`
- **Versi Android Gradle Plugin (AGP)**: `8.13.2`

**Mengapa ini menjadi masalah?**
Project Android saat ini dijalankan menggunakan **Java Development Kit (JDK) versi 26**. JDK 26 adalah versi yang sangat baru (*bleeding-edge*). Sistem *compiler* Android (AGP 8.x) dan pustaka pemroses anotasi Kotlin Symbol Processing (KSP) **belum mendukung JDK 26**. 

Akibatnya, saat Gradle mencoba memverifikasi versi Java yang digunakan, pemindai internalnya gagal mengenali format versi `26.0.1` dan langsung *crash* dengan melemparkan teks eksepsi `26.0.1`.

---

## 🛠️ Tutorial Cara Mengatasinya

Untuk mengatasi masalah kompatibilitas JDK ini agar aplikasi Perkapp sukses di-compile dan diinstal ke HP Anda, ikuti dua tahapan utama berikut:

### Tahap 1: Mengubah Versi Gradle JDK di Android Studio

Solusi utamanya adalah mengarahkan Android Studio untuk menggunakan **JDK 17** atau **JDK 21** (versi stabil standar yang didukung penuh oleh AGP 8.x), bukan menggunakan default JDK bawaan sistem Linux Anda.

1. Buka project Perkapp di **Android Studio**.
2. Masuk ke menu pengaturan (Settings):
   - Klik menu **File > Settings** (atau gunakan shortcut `Ctrl + Alt + S`).
3. Arahkan ke menu konfigurasi Gradle:
   - Pada panel sebelah kiri, buka **Build, Execution, Deployment > Build Tools > Gradle**.
4. Ubah opsi **Gradle JDK**:
   - Cari bagian **Gradle JDK** (di bawah daftar project/modul).
   - Klik *dropdown* versi JDK tersebut.
   - Pilih **jbr-17** (JetBrains Runtime 17) atau **jbr-21** (JetBrains Runtime 21) jika sudah tersedia di dalam daftar.
   - **Jika belum ada JDK 17/21**: Klik opsi **Download JDK...** di menu *dropdown* tersebut, pilih versi **17** atau **21** (vendor Corretto atau JetBrains), lalu klik Download.
5. Simpan pengaturan:
   - Klik **Apply** lalu **OK**.
6. Sinkronisasi ulang:
   - Klik tombol **Sync Project with Gradle Files** (ikon gajah di pojok kanan atas) dan tunggu hingga selesai.

---

### Tahap 2: Verifikasi & Troubleshooting Koneksi HP (Physical Device)

Jika setelah mengganti JDK proses *build* berhasil namun aplikasi masih tidak mau terbuka di HP, periksa hal-hal berikut:

1. **Persetujuan (Authorization) USB Debugging**:
   - Terkadang koneksi kabel longgar membuat *authorisasi* ulang terlewat. Cabut dan pasang kembali kabel USB HP Anda.
   - Pastikan Anda mengetuk **"Allow" (Izinkan)** pada *prompt* **Allow USB Debugging** yang muncul di layar HP saat kabel dicolokkan.
2. **Pemilihan Perangkat Target di Toolbar**:
   - Perhatikan *dropdown* perangkat target di bagian atas Android Studio (di sebelah kiri tombol Play/Run hijau).
   - Pastikan yang terpilih adalah **nama HP Anda** (misal: *Xiaomi*, *Samsung*, atau *Infinix*), **bukan** *Loading Devices...* atau *No Devices*.
3. **Pembersihan Cache Terakhir (Clean & Rebuild)**:
   - Untuk memastikan sisa-sisa *cache* dari kegagalan kompilasi JDK 26 benar-benar hilang, klik menu **Build > Clean Project** di panel atas Android Studio.
   - Setelah selesai, klik **Build > Rebuild Project**.
4. **Jalankan Aplikasi**:
   - Klik tombol **Run** (Play hijau). Android Studio akan memaketkan APK dan menginstalnya langsung ke HP Anda.
