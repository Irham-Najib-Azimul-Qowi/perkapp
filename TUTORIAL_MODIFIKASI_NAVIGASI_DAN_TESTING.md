# Tutorial Modifikasi Kode untuk Menguji Semua Fitur Perkapp — Bagian Najib

Menarik sekali! Anda menyampaikan bahwa Anda saat ini **hanya bisa mengakses halaman daftar inventaris (kosong) dan form tambah alat**, tetapi tidak bisa mengetes halaman **Detail** dan **Edit** karena tidak ada item yang bisa diketuk. 

Setelah dianalisa, ada **dua penyebab utama** mengapa hal ini terjadi pada kode lokal Anda:
1. **Tidak Ada Refresh List**: Di file `AlatViewModel.kt`, setelah fungsi `createAlat` berhasil menyimpan data ke database, fungsi tersebut **lupa melakukan pemanggilan `getAllAlat()`** untuk memperbarui daftar di layar utama. Akibatnya, setelah menekan Simpan, halaman utama tetap terlihat kosong (kecuali Anda menutup dan membuka ulang aplikasi).
2. **Ketergantungan Data Kosong**: Karena layar utama kosong, tidak ada kartu alat (*AlatCard*) yang bisa diklik untuk memicu navigasi ke halaman **Detail** dan **Edit**.

Berikut adalah tutorial langkah demi langkah untuk memodifikasi kode Anda agar semua fitur langsung aktif, otomatis terisi data uji coba (*Mock/Dummy Data*), dan siap Anda tes secara menyeluruh!

---

## 🛠️ Langkah 1: Perbaiki Auto-Refresh di `AlatViewModel.kt`

Buka file `app/src/main/java/com/example/perkapp/features/alat/ui/viewmodel/AlatViewModel.kt`.

Cari fungsi `createAlat` pada baris ke-33. Tambahkan baris `getAllAlat()` tepat di bawah baris pemanggilan repositori agar daftar alat langsung diperbarui otomatis begitu Anda kembali dari form tambah.

**Sebelum Perbaikan:**
```kotlin
    fun createAlat(name: String, category: String,  totalQty: Int, condition: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.createAlat(name, category,  totalQty, condition)
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally { isLoading.value = false}
        }
    }
```

**Setelah Perbaikan:**
```diff
     fun createAlat(name: String, category: String,  totalQty: Int, condition: String) {
         viewModelScope.launch {
             isLoading.value = true
             try {
                 repository.createAlat(name, category,  totalQty, condition)
+                getAllAlat() // Tambahkan baris ini untuk memperbarui daftar
             } catch (e: Exception) {
                 errorMessage.value = e.message
-            } finally { isLoading.value = false}
+            } finally { 
+                isLoading.value = false
+            }
         }
     }
```

---

## 🧪 Langkah 2: Tambahkan Dummy Data Otomatis untuk Pengujian

Agar Anda tidak perlu repot mengisi form setiap saat untuk mencoba fitur detail dan edit, kita bisa memodifikasi `AlatRepository.kt` agar otomatis menyuntikkan data buatan (*Mock Data*) ke dalam Room Database lokal Anda jika database terdeteksi kosong.

Buka file `app/src/main/java/com/example/perkapp/features/alat/data/repository/AlatRepository.kt`. Ubah fungsi `getAllAlat()` menjadi seperti ini:

**Ganti fungsi `getAllAlat()` (Baris 14 - 37) dengan kode ini:**
```kotlin
    suspend fun getAllAlat(): List<AlatEntity> {
        try {
            // Cek data di API
            val response = api.getAllAlat()
            if (response.isSuccessful) {
                response.body()?.data?.let { alatList ->
                    val entities = alatList.map { item ->
                        AlatEntity(
                            id = item.id,
                            name = item.name,
                            category = item.category,
                            total_qty = item.total_qty,
                            available_qty = item.available_qty,
                            condition = item.condition,
                            sync_status = "synced"
                        )
                    }
                    dao.insertAllAlat(entities)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace();
        }

        // AMBIL DATA DARI ROOM LOCAL
        val localData = dao.getAllAlat()
        
        // PENTING UNTUK TESTING: Jika database lokal masih kosong,
        // kita masukkan data dummy secara otomatis agar Anda bisa mengetes klik detail & edit!
        if (localData.isEmpty()) {
            val dummyList = listOf(
                AlatEntity("dummy-1", "Carrier Eiger 60L", "Tas", 5, 5, "good", "synced"),
                AlatEntity("dummy-2", "Tenda Consina Magnum 4", "Tenda", 3, 2, "good", "pending"),
                AlatEntity("dummy-3", "Kompor Portable Kovar", "Alat Masak", 10, 8, "damaged", "synced")
            )
            dao.insertAllAlat(dummyList)
            return dummyList
        }

        return localData
    }
```

---

## 🏃‍♂️ Langkah 3: Rebuild dan Lakukan Pengujian!

1. Klik tombol **Build > Clean Project** di Android Studio.
2. Klik **Run (Play hijau)** untuk menginstal ulang aplikasi ke HP Anda.
3. **Hasil yang akan Anda lihat**:
   *   Saat pertama kali aplikasi dibuka, Anda akan langsung melihat **3 item alat dummy** di layar utama (`Carrier Eiger`, `Tenda Consina`, dan `Kompor Portable`).
   *   Ketuk salah satu item, misalnya **Tenda Consina**. Anda akan langsung dibawa masuk ke **DetailAlatScreen** dengan informasi lengkap.
   *   Di halaman detail, ketuk tombol **Edit Alat** di bagian bawah. Form **EditAlatScreen** akan terbuka lengkap dengan data awal tenda yang siap diubah.
   *   Ketuk tombol **(+)** di layar utama, tambahkan alat baru, dan begitu menekan **Simpan**, daftar alat di layar utama akan langsung bertambah secara instan!
