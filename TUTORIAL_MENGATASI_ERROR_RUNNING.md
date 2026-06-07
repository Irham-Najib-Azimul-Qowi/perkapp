# Tutorial Mengatasi Kendala Running Aplikasi Perkapp ke HP

Berdasarkan hasil analisa menyeluruh terhadap seluruh kode sumber dari Part 1 hingga Part 4, terdapat beberapa **kesalahan penulisan sintaks (typo)**, **konfigurasi build yang tidak valid**, serta **kesalahan definisi rute navigasi** yang menyebabkan aplikasi gagal di-compile (*Build Failed*) dan crash saat dijalankan.

Dokumen ini berisi tutorial langkah demi langkah untuk memperbaiki semua masalah tersebut agar aplikasi Perkapp dapat berjalan lancar di HP Anda.

---

## 📊 Ringkasan Analisa Masalah

Berikut adalah akar masalah (*root cause*) mengapa aplikasi saat ini tidak bisa di-running ke HP:

1. **Konfigurasi `compileSdk` Tidak Valid**: Pada file `app/build.gradle.kts`, definisi versi SDK ditulis menggunakan blok lambda yang salah sehingga Gradle gagal mengevaluasi skrip.
2. **Typo Tag Permission di Manifest**: Tag penambahan izin internet di `AndroidManifest.xml` salah ketik dari `<uses-permission>` menjadi `<user-permission>`. Ini memblokir akses jaringan Retrofit.
3. **Parameter Rute Navigasi Terpotong**: Di `MainActivity.kt`, parameter rute Jetpack Navigation Compose kekurangan tanda kurung kurawal penutup `}` pada halaman Detail dan Edit. Ini memicu *runtime crash*.
4. **File Sampah / Typo**: Terdapat file kosong yang salah ketik seperti `RetrovitClient.kt` dan `AlatReponse.kt` yang tertinggal dan dapat membingungkan IDE.

---

## 🛠️ Langkah-Langkah Perbaikan (Tutorial)

Silakan ikuti instruksi perbaikan berikut secara berurutan pada project Android Studio Anda:

### 1. Perbaikan Konfigurasi Build (`app/build.gradle.kts`)
Buka file `app/build.gradle.kts` (modul app). Pada bagian blok `android { ... }`, cari baris kode `compileSdk` yang salah dan ubah menjadi properti integer langsung.

**Sebelum Perbaikan:**
```kotlin
android {
    namespace = "com.example.perkapp"
    compileSdk {
        version = release(36)
    }
    // ...
```

**Setelah Perbaikan:**
```diff
 android {
     namespace = "com.example.perkapp"
-    compileSdk {
-        version = release(36)
-    }
+    compileSdk = 36
     // ...
```

> [!IMPORTANT]  
> Setelah mengubah file `build.gradle.kts`, selalu klik tombol **Sync Now** (berlogo gajah) di pojok kanan atas editor Android Studio agar Gradle memperbarui konfigurasi project.

---

### 2. Perbaikan Izin Internet di Manifest (`AndroidManifest.xml`)
Buka file `app/src/main/AndroidManifest.xml`. Perbaiki nama tag penulisan izin akses internet.

**Sebelum Perbaikan:**
```xml
    <user-permission android:name="android.permission.INTERNET" />
```

**Setelah Perbaikan:**
```diff
-    <user-permission android:name="android.permission.INTERNET" />
+    <uses-permission android:name="android.permission.INTERNET" />
```

---

### 3. Perbaikan Rute Navigasi Compose (`MainActivity.kt`)
Buka file `app/src/main/java/com/example/perkapp/MainActivity.kt`. Cari bagian definisi `NavHost` pada baris ke-72 dan ke-90, lalu lengkapi tanda penutup kurung kurawal `}` pada argumen `alatId`.

**Sebelum Perbaikan:**
```kotlin
// Pada blok composable detail_alat:
route = "detail_alat/{alatId",

// Pada blok composable edit_alat:
route = "edit_alat/{alatId",
```

**Setelah Perbaikan:**
```diff
// Pada blok composable detail_alat:
-route = "detail_alat/{alatId",
+route = "detail_alat/{alatId}",

// Pada blok composable edit_alat:
-route = "edit_alat/{alatId",
+route = "edit_alat/{alatId}",
```

---

### 4. Pembersihan File Salah Ketik (Cleanup)
Untuk menjaga kebersihan arsitektur kode dan menghindari ambiguitas saat kompilasi, hapus file-file kosong berikut yang salah nama:
- Hapus file: `app/src/main/java/com/example/perkapp/core/RetrovitClient.kt` *(karena yang benar adalah `core/network/RetrofitClient.kt`)*.
- Hapus file: `app/src/main/java/com/example/perkapp/features/alat/api/AlatReponse.kt` *(karena model respons yang dipakai berada di `data/remote/AlatResponse.kt`)*.

---

### 5. Hapus Import Otomatis yang Tidak Perlu di `TambahAlatScreen.kt`
Buka file `app/src/main/java/com/example/perkapp/features/alat/ui/screen/TambahAlatScreen.kt`, lalu hapus dua baris import paling atas berikut jika ada:
```kotlin
import android.R.attr.icon
import android.R.id.icon
```

---

## 📱 Panduan Konfigurasi HP untuk Running

Setelah semua kode di atas diperbaiki, ikuti langkah berikut agar HP Android Anda terdeteksi oleh Android Studio dan aplikasi bisa diinstal:

1. **Aktifkan Opsi Pengembang (Developer Options)**:
   - Buka **Settings (Pengaturan)** di HP Anda.
   - Masuk ke **About Phone (Tentang Ponsel)**.
   - Ketuk tulisan **Build Number (Nomor Bentukan)** sebanyak **7 kali** berturut-turut hingga muncul notifikasi *"Anda sekarang adalah seorang pengembang!"*.
2. **Aktifkan USB Debugging**:
   - Kembali ke menu utama Settings, cari menu **System** atau **Additional Settings (Pengaturan Tambahan)**.
   - Pilih **Developer Options (Opsi Pengembang)**.
   - Gulir ke bawah dan aktifkan sakelar **USB Debugging (Debug USB)**.
3. **Sambungkan dan Izinkan Perangkat**:
   - Hubungkan HP ke laptop/komputer menggunakan kabel USB data yang berkualitas.
   - Pada layar HP, akan muncul *pop-up* konfirmasi: **"Allow USB debugging?"** (Izinkan debugging USB?).
   - Centang opsi *"Always allow from this computer"* lalu ketuk **Allow (Izinkan)**.
4. **Jalankan Aplikasi di Android Studio**:
   - Pastikan nama HP Anda sudah muncul di menu *dropdown* perangkat (sebelah tombol Run hijau di toolbar atas Android Studio).
   - Klik tombol **Run (Play hijau)** atau tekan shortcut `Shift + F10`.
   - Android Studio akan melakukan proses *Build*. Setelah tulisan *"Launch succeeded"* muncul di bawah, aplikasi Perkapp akan otomatis terbuka di layar HP Anda.

---

> [!TIP]
> Jika proses build masih mengalami kendala cache sistem lama, lakukan pembersihan total dengan memilih menu **Build > Clean Project**, dilanjutkan dengan **Build > Rebuild Project** di Android Studio.
