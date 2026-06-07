# Tutorial Mengatasi Kendala Running Aplikasi Perkapp ke HP — Part 3

Analisa baru telah dilakukan! Setelah kita berhasil mengarahkan kompilator untuk menggunakan **JDK 17** yang didukung sistem, proses *build* akhirnya bisa berjalan lebih jauh dan memunculkan pesan error kompilasi yang sesungguhnya (bukan lagi error `26.0.1` yang terpotong).

Pesan error yang muncul saat ini pada tab **Build Output** Android Studio Anda adalah:
```
> Configure project :app
ksp-2.0.0-1.0.21 is too old for kotlin-2.0.21. Please upgrade ksp or downgrade kotlin-gradle-plugin to 2.0.0.

> Task :app:kspDebugKotlin FAILED
e: java.lang.IncompatibleClassChangeError: class com.google.devtools.ksp.common.PersistentMap cannot inherit from final class org.jetbrains.kotlin.com.intellij.util.io.PersistentHashMap
```

Dokumen **Part 3** ini akan membedah tuntas mengapa error ini terjadi dan tutorial super singkat untuk menyelesaikannya.

---

## 🔬 Analisa Akar Masalah (Root Cause)

Akar masalahnya adalah **ketidakcocokan (mismatch) versi antara Kotlin Compiler dan plugin KSP (Kotlin Symbol Processing)** yang digunakan oleh database Room.

1. Pada file `gradle/libs.versions.toml`, versi **Kotlin** Anda diatur ke `2.0.21`.
2. Namun, versi **KSP** Anda tertulis `2.0.0-1.0.21` (yang ditujukan untuk Kotlin versi lama `2.0.0`).
3. Karena KSP bekerja dengan cara menyisipkan dirinya langsung ke dalam inti internal kompilator Kotlin, perbedaan versi ini menyebabkan bentrokan struktur kelas di memori (*IncompatibleClassChangeError*), di mana sebuah kelas internal Kotlin (`PersistentHashMap`) telah berubah sifatnya menjadi kelas final pada versi `2.0.21`.

---

## 🛠️ Tutorial Cara Mengatasinya

Solusinya sangatlah mudah dan hanya memerlukan satu perubahan kecil pada satu baris file konfigurasi. Silakan ikuti langkah berikut:

### Langkah 1: Buka File Version Catalog
Buka panel **Project** di Android Studio Anda, lalu navigasikan ke folder `gradle/` dan buka file **`libs.versions.toml`**.

### Langkah 2: Ubah Versi KSP
Cari blok `[versions]` di bagian paling atas file tersebut. Perhatikan baris ke-17 yang mendefinisikan versi `ksp`. Ubah nilainya agar sejajar dengan versi Kotlin `2.0.21`.

**Sebelum Perbaikan:**
```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
// ...
ksp = "2.0.0-1.0.21"
```

**Setelah Perbaikan:**
```diff
 [versions]
 agp = "8.13.2"
 kotlin = "2.0.21"
 // ...
-ksp = "2.0.0-1.0.21"
+ksp = "2.0.21-1.0.28"
```

### Langkah 3: Sinkronisasi & Jalankan Ulang
1. Setelah angka versi diubah, akan muncul *banner* atau tombol **Sync Now** (berlogo gajah) di pojok kanan atas editor. Klik tombol tersebut untuk mengunduh pustaka KSP yang baru.
2. Tunggu hingga proses *Sync* selesai dengan status *successful*.
3. Klik kembali tombol **Run (Play hijau)** untuk menjalankan aplikasi ke HP Anda. 

Dengan perbaikan ini, kompilator KSP dan Kotlin akan berjalan harmonis, memproses anotasi tabel Room Database tanpa *crash*, dan aplikasi Perkapp siap tampil di layar HP Anda!
