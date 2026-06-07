# Tutorial Mengatasi Kendala Running Aplikasi Perkapp ke HP — Part 4 (Force Close Fix)

Selamat! Proses kompilasi (*Build*) project Anda kini telah berhasil sepenuhnya tanpa ada error, dan file APK sukses terinstal ke dalam HP Anda.

Namun, kendala baru yang Anda temui adalah **Force Close** (aplikasi langsung *crash* atau tertutup sendiri seketika saat baru dibuka). 

Dokumen **Part 4** ini akan menjelaskan hasil analisa penyebab *crash* tersebut dan tutorial cara memperbaikinya.

---

## 💥 Analisa Akar Masalah (Root Cause)

Akar masalah Force Close ini disebabkan oleh aturan ketat dari pustaka jaringan **Retrofit** terkait penulisan alamat **Base URL**.

Jika kita melihat file `app/src/main/java/com/example/perkapp/core/network/RetrofitClient.kt`, konstanta `BASE_URL` saat ini dituliskan sebagai berikut:
```kotlin
private const val BASE_URL = "https://cakramanggalapnm.com/api/v1"
```

**Mengapa ini memicu *crash*?**
Secara desain internal, Retrofit mewajibkan setiap `baseUrl` **harus selalu diakhiri dengan tanda garis miring (trailing slash `/`)**. 

Ketika aplikasi baru dibuka, fungsi `onCreate` di `MainActivity` langsung berjalan dan melakukan inisialisasi objek `RetrofitClient`. Karena alamat URL tidak berakhiran `/`, Retrofit secara otomatis melemparkan eksepsi fatal:
```
java.lang.IllegalArgumentException: baseUrl must end in /
```
Eksepsi yang tidak ditangkap (*uncaught exception*) inilah yang membuat sistem Android langsung mematikan paksa aplikasi Anda (*Force Close*).

---

## 🛠️ Tutorial Cara Mengatasinya

Untuk memperbaiki kendala ini, Anda hanya perlu menambahkan satu karakter garis miring (`/`) di akhir string Base URL.

### Langkah 1: Buka File RetrofitClient.kt
Navigasikan ke panel **Project**, lalu buka file:
`app/src/main/java/com/example/perkapp/core/network/RetrofitClient.kt`

### Langkah 2: Tambahkan Trailing Slash
Cari baris ke-12 yang mendefinisikan `BASE_URL`, lalu tambahkan tanda `/` tepat di bagian paling akhir string URL tersebut (di dalam tanda kutip).

**Sebelum Perbaikan:**
```kotlin
private const val BASE_URL = "https://cakramanggalapnm.com/api/v1"
```

**Setelah Perbaikan:**
```diff
-private const val BASE_URL = "https://cakramanggalapnm.com/api/v1"
+private const val BASE_URL = "https://cakramanggalapnm.com/api/v1/"
```

### Langkah 3: Jalankan Ulang Aplikasi
Setelah menambahkan garis miring tersebut, simpan file dan klik kembali tombol **Run (Play hijau)** di Android Studio Anda. 

Kini Retrofit akan berhasil diinisialisasi dengan sempurna, dan halaman utama aplikasi Perkapp Anda akan terbuka lancar di HP tanpa *Force Close*!
