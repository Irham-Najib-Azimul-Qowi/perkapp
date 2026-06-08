# 📱 Perkapp — Aplikasi Mobile Manajemen Perlengkapan UKM PA Cakra Manggala

**Perkapp** adalah aplikasi Android berbasis *offline-first* yang dirancang untuk mendata, memantau, dan mengelola inventaris alat serta perlengkapan operasional pada **UKM PA Cakra Manggala**. Aplikasi ini mempermudah pencatatan peminjaman alat untuk kegiatan operasional, pemantauan kondisi alat, serta pelaporan log kegiatan secara terstruktur.

---

## 🚀 Fitur Utama

1. **Autentikasi & Manajemen Pengguna**
   * Pendaftaran akun baru (*Register*) dan masuk (*Login*) menggunakan JWT (JSON Web Token).
   * Halaman profil untuk melihat informasi pengguna yang sedang masuk.

2. **Manajemen Inventaris Alat (Offline-First)**
   * Menampilkan daftar inventaris alat lengkap dengan status stok, kategori, dan kondisi (`good` / `damaged`).
   * Menambah, memperbarui, dan menghapus data alat secara lokal maupun remote.
   * Mendukung penyimpanan lokal (Room DB) sehingga aplikasi tetap berfungsi penuh meskipun tanpa koneksi internet.

3. **Manajemen Kegiatan**
   * Mencatat kegiatan baru, mengedit detail kegiatan, dan memantau status kegiatan (`draft`, `ongoing`, `completed`).
   * Mengaitkan kebutuhan alat ke dalam kegiatan operasional tertentu beserta kuantitasnya.
   * Pengurangan stok alat secara otomatis saat dipinjam dan pengembalian stok saat status kegiatan berubah menjadi `completed`.

4. **Sinkronisasi Otomatis (Background Sync)**
   * Menggunakan **WorkManager** untuk menyelaraskan perubahan data lokal (Room DB) ke API Server backend ketika koneksi internet terdeteksi kembali.
   * Deteksi status koneksi internet secara *real-time* dengan `NetworkMonitor`.

5. **Manajemen Gambar (Media)**
   * Mendukung pengambilan foto menggunakan kamera atau galeri untuk diunggah sebagai visualisasi alat atau dokumentasi kegiatan.

---

## 🛠️ Tech Stack & Arsitektur

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/compose) (Modern Android declarative UI)
* **Local Database:** [Room Database](https://developer.android.com/training/data-storage/room) (SQLite abstraction layer)
* **Networking:** [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/) (HTTP Client)
* **Asynchronous Programming:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
* **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) / Custom DI
* **Background Processing:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
* **Design Pattern:** MVVM (Model-View-ViewModel) dengan arsitektur bersih terbagi atas modul `core` (shared) dan `features` (domain-specific).

---

## 📂 Struktur Direktori Proyek

```text
app/src/main/java/com/example/perkapp/
├── core/                         # Komponen global/shared
│   ├── database/                 # Room Database, DAO, & Entitas Lokal
│   ├── datastore/                # UserPreferences (Token & session storage)
│   ├── di/                       # Dependency Injection Module
│   ├── features/kegiatan/        # Modul Core Kegiatan (UI, ViewModel, Repo)
│   ├── navigation/               # NavGraph, BottomBar, & Routes
│   ├── network/                  # Retrofit Client, Interceptor, & ApiResponse
│   ├── sync/                     # SyncManager & SyncWorker (WorkManager)
│   ├── ui/theme/                 # Konfigurasi Tema (Color, Theme, Type)
│   └── utils/                    # Utility (ImageUtils & NetworkUtils)
│
└── features/                     # Fitur spesifik aplikasi
    ├── alat/                     # Modul Inventaris Alat (API, Local, UI, ViewModel)
    ├── auth/                     # Modul Autentikasi (Splash, Login, Register, Profile)
    └── media/                    # Modul Media / Upload Gambar
```

---

## ⚙️ Persyaratan Sistem & Instalasi

### Persyaratan Minimum
* **JDK:** Version 17 atau lebih tinggi.
* **Android SDK:** Compile SDK 35, Min SDK 26 (Android 8.0).
* **Android Studio:** Android Studio Koala / Ladybug atau versi terbaru.

### Langkah Menjalankan Aplikasi

1. **Clone Repositori:**
   ```bash
   git clone git@github.com:Irham-Najib-Azimul-Qowi/perkapp.git
   cd perkapp
   ```

2. **Konfigurasi API URL:**
   Sesuaikan `BASE_URL` pada file `RetrofitClient.kt` di direktori:
   `app/src/main/java/com/example/perkapp/core/network/RetrofitClient.kt`
   ```kotlin
   private const val BASE_URL = "https://api.perkapp.com/v1/"
   ```

3. **Build & Run Aplikasi:**
   Buka proyek di Android Studio, tunggu proses sinkronisasi Gradle selesai, lalu hubungkan perangkat fisik (HP) via USB Debugging / Emulator dan tekan tombol **Run**. Alternatif menggunakan CLI:
   ```bash
   ./gradlew installDebug
   ```

---

## 📄 Dokumentasi Tambahan

Detail spesifikasi endpoint REST API yang digunakan oleh aplikasi dapat dibaca pada berkas [API_DOCUMENTATION.md](API_DOCUMENTATION.md).
