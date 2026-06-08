package com.example.perkapp.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * ImageUtils — Alat bantu serba bisa untuk urusan Gambar (Foto).
 *
 * Berisi fungsi-fungsi kompleks untuk:
 * 1. Mengubah file gambar lokal menjadi Bitmap (bisa ditampilkan di UI)
 * 2. Mengunduh gambar dari server secara manual jika library gagal
 * 3. Menyimpan gambar kamera secara permanen agar tidak hilang
 */
object ImageUtils {

    /**
     * Memuat gambar yang tersimpan secara lokal di HP (via alamat URI lokal).
     */
    fun loadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        android.util.Log.d("ImageUtils", "loadBitmapFromUri: uriString = $uriString")
        return try {
            val uri = Uri.parse(uriString)
            android.util.Log.d("ImageUtils", "loadBitmapFromUri: parsed uri = $uri, scheme = ${uri.scheme}")
            // Jika formatnya 'file://', baca langsung filenya
            val bitmap = if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path)
            } else {
                // Jika formatnya 'content://' (standar Android terbaru), gunakan MediaStore / ImageDecoder
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }
            }
            android.util.Log.d("ImageUtils", "loadBitmapFromUri: success loaded bitmap = $bitmap")
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "loadBitmapFromUri: error loading $uriString", e)
            null
        }
    }

    /**
     * Memuat gambar langsung dari internet (URL) menggunakan OkHttp.
     * Jika gagal (misal karena offline), otomatis mencari salinan lokalnya di database.
     */
    private fun loadBitmapFromNetwork(context: Context, urlString: String): Bitmap? {
        // Otomatis memperbaiki URL jika server lokal (localhost) namun diakses via HP fisik
        val correctedUrl = if (urlString.contains("localhost") || urlString.contains("127.0.0.1") || urlString.contains("10.0.2.2")) {
            urlString.replace(Regex("^https?://[^/]+"), "https://cakramanggalapnm.com")
        } else {
            urlString
        }
        android.util.Log.d("ImageUtils", "loadBitmapFromNetwork: urlString = $urlString, correctedUrl = $correctedUrl")
        return try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(correctedUrl).build()
            client.newCall(request).execute().use { response ->
                android.util.Log.d("ImageUtils", "loadBitmapFromNetwork: HTTP code = ${response.code}, isSuccessful = ${response.isSuccessful}")
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        android.util.Log.d("ImageUtils", "loadBitmapFromNetwork: decoded bitmap = $bitmap")
                        bitmap
                    }
                } else {
                    android.util.Log.w("ImageUtils", "loadBitmapFromNetwork: failed, trying local fallback...")
                    // Gagal ambil dari internet (error 404/500 dll), cari fotonya di HP
                    loadFallbackLocalImage(context, correctedUrl, urlString)
                }
            }
        } catch (e: Exception) {
            // Internet mati, langsung cari file offline-nya
            android.util.Log.e("ImageUtils", "loadBitmapFromNetwork: error loading $correctedUrl, trying local fallback...", e)
            loadFallbackLocalImage(context, correctedUrl, urlString)
        }
    }

    /**
     * Mencari apakah gambar yang gagal diunduh dari internet punya salinan fisik 
     * di folder lokal (berdasarkan catatan tabel ImageEntity di Room).
     */
    private fun loadFallbackLocalImage(context: Context, urlString: String, originalUrl: String? = null): Bitmap? {
        return try {
            val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
            val imageDao = db.imageDao()
            var localPath: String? = null
            kotlinx.coroutines.runBlocking {
                var entity = imageDao.getImageByUrl(urlString)
                if (entity == null && originalUrl != null) {
                    entity = imageDao.getImageByUrl(originalUrl)
                }
                localPath = entity?.local_path
            }
            if (!localPath.isNullOrBlank()) {
                android.util.Log.d("ImageUtils", "loadFallbackLocalImage: found local fallback path = $localPath")
                loadBitmapFromUri(context, localPath)
            } else {
                android.util.Log.w("ImageUtils", "loadFallbackLocalImage: no local fallback path found for $urlString")
                null
            }
        } catch (ex: Exception) {
            android.util.Log.e("ImageUtils", "loadFallbackLocalImage: error in lookup", ex)
            null
        }
    }

    /**
     * Fungsi utama untuk menampilkan gambar.
     * Otomatis mendeteksi apakah path-nya berupa alamat Web atau alamat Lokal.
     */
    fun loadBitmap(context: Context, path: String?): Bitmap? {
        android.util.Log.d("ImageUtils", "loadBitmap: path = $path")
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            val res = loadBitmapFromNetwork(context, path)
            android.util.Log.d("ImageUtils", "loadBitmap: network result = $res")
            res
        } else {
            val res = loadBitmapFromUri(context, path)
            android.util.Log.d("ImageUtils", "loadBitmap: local result = $res")
            res
        }
    }

    /**
     * Simpan objek gambar (Bitmap) menjadi file fisik (.jpg) di memori internal aplikasi.
     * Gambar ini dikunci dan aman (tidak mudah terhapus cache-cleaner).
     * 
     * Berguna saat memfoto via aplikasi, lalu menyimpannya untuk di-upload belakangan.
     */
    fun saveBitmapToFile(context: Context, bitmap: Bitmap): String? {
        return try {
            val filename = "img_${UUID.randomUUID()}.jpg"
            // Gunakan filesDir agar gambar persisten (tidak dihapus oleh sistem secara otomatis)
            val imageDir = File(context.filesDir, "images")
            if (!imageDir.exists()) imageDir.mkdirs() // Buat foldernya jika belum ada
            val file = File(imageDir, filename)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            Uri.fromFile(file).toString() // Kembalikan alamat 'file://...'
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Menyiapkan File fisik dari sebuah alamat URI (misal foto dari galeri).
     * Jika asalnya dari galeri (content://), kita salin/copy dulu ke folder 
     * aplikasi agar server Retrofit bisa mengunggahnya tanpa masalah izin akses.
     */
    fun getFileFromUri(context: Context, uriString: String): File? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                File(uri.path!!)
            } else {
                // Content URI (dari Galeri Android) -> Copy ke direktori lokal aplikasi
                val filename = "upload_${UUID.randomUUID()}.jpg"
                val imageDir = File(context.filesDir, "images")
                if (!imageDir.exists()) imageDir.mkdirs()
                val file = File(imageDir, filename)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Ingatan Visual (Remember Async Image) — Komponen UI bantu untuk Compose.
 *
 * Mengambil gambar secara asinkron di latar belakang (tanpa bikin layar macet),
 * lalu menampilkannya begitu fotonya siap.
 */
@Composable
fun rememberAsyncImage(path: String?): Bitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Simpan status gambar, awalnya null (kosong)
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    
    // Efek Samping: Tiap kali 'path' foto berubah, lakukan pengunduhan ulang
    LaunchedEffect(path) {
        if (!path.isNullOrBlank()) {
            withContext(Dispatchers.IO) { // Pindahkan beban kerja ke thread pekerja (IO)
                bitmap = ImageUtils.loadBitmap(context, path)
            }
        } else {
            bitmap = null
        }
    }
    return bitmap
}