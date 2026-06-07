# Panduan Setup Project Perkapp

Dokumen ini berisi langkah-langkah untuk menyiapkan dan menjalankan project Android **Perkapp** di lingkungan lokal Anda.

## Prasyarat (Prerequisites)

Sebelum memulai, pastikan Anda telah menginstal perangkat lunak berikut:

1.  **Java Development Kit (JDK) 11**: Project ini menggunakan Java 11. Anda bisa menggunakan OpenJDK atau JDK dari Oracle.
2.  **Android Studio**: Disarankan menggunakan versi terbaru (Ladybug atau lebih baru) untuk dukungan penuh terhadap Jetpack Compose dan Gradle terbaru.
3.  **Android SDK**: Pastikan SDK Platform untuk Android 36 (atau versi yang sesuai di `build.gradle.kts`) sudah terinstal melalui SDK Manager di Android Studio.

## Langkah-langkah Setup

### 1. Persiapan Project
Jika Anda baru saja mengunduh project ini:
- Ekstrak file zip atau clone repository ini ke folder lokal Anda.
- Buka **Android Studio**.

### 2. Membuka Project
- Pilih **File > Open** atau **Open an Existing Project**.
- Arahkan ke direktori root project ini (`perkapp`).
- Klik **OK**.

### 3. Sinkronisasi Gradle (Gradle Sync)
- Setelah project terbuka, Android Studio akan otomatis memulai proses *Gradle Sync*.
- Pastikan Anda terhubung ke internet karena Android Studio akan mengunduh dependensi yang diperlukan.
- Tunggu hingga proses selesai. Jika ada pesan "Build Successful" di tab *Build*, berarti project siap digunakan.

### 4. Konfigurasi Emulator atau Device
- **Emulator**: Buka **Device Manager** di Android Studio dan buat/jalankan *Android Virtual Device* (AVD). Disarankan menggunakan API level 24 ke atas (sesuai `minSdk`).
- **Physical Device**: Sambungkan HP Android Anda via kabel USB dan pastikan **USB Debugging** sudah aktif di opsi pengembang.

### 5. Menjalankan Project
- Pastikan modul `app` terpilih di toolbar atas.
- Pilih device/emulator yang ingin digunakan.
- Klik tombol **Run** (ikon play hijau) atau tekan `Shift + F10`.
- Aplikasi akan di-*build* dan diinstal ke perangkat Anda.

## Struktur Project Singkat

- `app/src/main/java/com/example/perkapp`: Berisi kode sumber Kotlin.
- `app/src/main/res`: Berisi resource (layout, gambar, nilai string, dll).
- `gradle/libs.versions.toml`: Berisi daftar versi library (Version Catalog).
- `build.gradle.kts`: Konfigurasi build level project dan modul.

## Troubleshooting

- **Gradle Build Error**: Jika terjadi error saat build, coba lakukan **File > Invalidate Caches / Restart**.
- **JDK Version**: Pastikan di **Settings > Build, Execution, Deployment > Build Tools > Gradle**, bagian *Gradle JDK* diarahkan ke JDK 11 atau yang kompatibel.
- **Missing SDK**: Jika muncul pesan SDK missing, klik link yang disediakan di error message untuk menginstal SDK yang diperlukan otomatis.

---
*Dibuat oleh Antigravity AI*
