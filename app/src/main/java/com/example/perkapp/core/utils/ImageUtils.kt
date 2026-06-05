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

object ImageUtils {

    fun loadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        android.util.Log.d("ImageUtils", "loadBitmapFromUri: uriString = $uriString")
        return try {
            val uri = Uri.parse(uriString)
            android.util.Log.d("ImageUtils", "loadBitmapFromUri: parsed uri = $uri, scheme = ${uri.scheme}")
            val bitmap = if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path)
            } else {
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

    private fun loadBitmapFromNetwork(context: Context, urlString: String): Bitmap? {
        android.util.Log.d("ImageUtils", "loadBitmapFromNetwork: urlString = $urlString")
        return try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(urlString).build()
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
                    loadFallbackLocalImage(context, urlString)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "loadBitmapFromNetwork: error loading $urlString, trying local fallback...", e)
            loadFallbackLocalImage(context, urlString)
        }
    }

    private fun loadFallbackLocalImage(context: Context, urlString: String): Bitmap? {
        return try {
            val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
            val imageDao = db.imageDao()
            var localPath: String? = null
            kotlinx.coroutines.runBlocking {
                val entity = imageDao.getImageByUrl(urlString)
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
     * Memuat bitmap baik dari URL online (network) maupun URI lokal.
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
     * Simpan bitmap ke file internal app dan kembalikan URI string-nya.
     * File disimpan di filesDir (bukan cacheDir) agar tidak terhapus otomatis.
     */
    fun saveBitmapToFile(context: Context, bitmap: Bitmap): String? {
        return try {
            val filename = "img_${UUID.randomUUID()}.jpg"
            // Gunakan filesDir agar gambar persisten (tidak dihapus oleh sistem)
            val imageDir = File(context.filesDir, "images")
            if (!imageDir.exists()) imageDir.mkdirs()
            val file = File(imageDir, filename)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Dapatkan File object dari URI string.
     * Berguna untuk upload gambar ke server.
     */
    fun getFileFromUri(context: Context, uriString: String): File? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                File(uri.path!!)
            } else {
                // Content URI - copy ke file lokal
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
 * Helper composable untuk memuat gambar secara asinkron dari URL online maupun file lokal.
 */
@Composable
fun rememberAsyncImage(path: String?): Bitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path) {
        if (!path.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                bitmap = ImageUtils.loadBitmap(context, path)
            }
        } else {
            bitmap = null
        }
    }
    return bitmap
}