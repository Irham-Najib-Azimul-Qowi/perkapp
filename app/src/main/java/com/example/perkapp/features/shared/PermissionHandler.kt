package com.example.perkapp.features.shared

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import android.Manifest

/**
 * RequestStoragePermission — Komponen (efek samping) untuk meminta izin akses ke penyimpanan HP.
 *
 * Menyesuaikan izin yang diminta berdasarkan versi Android (Android 13+ minta izin foto spesifik,
 * di bawahnya minta izin penyimpanan eksternal biasa).
 *
 * @param onGranted Fungsi yang dijalankan jika pengguna memberikan izin (Allow).
 * @param onDenied Fungsi yang dijalankan jika pengguna menolak izin (Deny).
 */
@Composable
fun RequestStoragePermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted() else onDenied()
    }

    LaunchedEffect(Unit) {
        launcher.launch(permission)
    }
}