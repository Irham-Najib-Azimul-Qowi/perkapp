package com.example.perkapp.features.shared

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.contracts.contract

/**
 * ImagePickerButton — Komponen tombol pakai ulang untuk memilih gambar dari Galeri HP.
 *
 * Menggunakan ActivityResultContracts bawaan Jetpack Compose untuk meminta
 * akses file secara aman.
 *
 * @param onImagePicked Fungsi yang dipanggil saat pengguna selesai memilih gambar.
 *                      Akan mengembalikan alamat (URI) dari gambar yang dipilih.
 */
@Composable
fun ImagePickerButton(
    onImagePicked: (Uri) -> Unit
) {
    // Menyiapkan 'Peluncur' yang meminta Galeri Android untuk mengambil file
    val launcher = rememberLauncherForActivityResult(
        contract  = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Jika user tidak batal (uri tidak kosong), kembalikan hasilnya
        uri?.let { onImagePicked(it) }
    }

    // Tombol pemicu
    Button(onClick =  { 
        // Meluncurkan permintaan pencarian file dengan tipe gambar apa saja
        launcher.launch("image/*")
    }) {
        Text("Pilih Gambar")
    }
}