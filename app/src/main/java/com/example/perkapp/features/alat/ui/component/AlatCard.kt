/**
 * FUNGSI: AlatCard
 * TUJUAN: Komponen UI (Jetpack Compose) yang dapat digunakan ulang (Reusable) 
 * untuk menampilkan satu kotak kartu barang/alat.
 *
 * ALUR LOGIKA PENGERJAAN:
 * 1. Menerima objek `AlatEntity` dan mencetak rinciannya (Nama, Kategori, Stok, Kondisi).
 * 2. Memuat gambar alat secara asinkron. Jika gagal atau kosong, kartu akan 
 *    mencetak huruf inisial dari nama alat.
 * 3. Memberikan umpan balik visual terkait status sinkronisasi alat:
 *    - Hijau ("Terupload ke API Server") jika `sync_status == "synced"`.
 *    - Kuning ("Tersimpan di Room Database") jika `sync_status == "pending"`.
 * 4. Saat diklik, kartu ini memicu fungsi (Lambda) `onClick` agar layarnya 
 *    tahu kartu mana yang dipilih pengguna (misal untuk navigasi ke Detail Alat).
 */
package com.example.perkapp.features.alat.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.perkapp.core.utils.rememberAsyncImage
import com.example.perkapp.features.alat.data.local.AlatEntity

@Composable
fun AlatCard(
    alat: AlatEntity,
    onClick: () -> Unit = {}
) {

    val bitmap = rememberAsyncImage(alat.image_path)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal =  16.dp, vertical =  4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation =  2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier =  Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(bitmap = it.asImageBitmap(),
                        contentDescription = "Gambar Alat",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } ?: Text(
                    text = alat.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp) )
// INFO ALAT
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Indikator sync status
                    if (alat.sync_status == "pending") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Belum disinkronkan",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF856404)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // label kategori dengan chip style

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape =  RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text =alat.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(8.dp))

                Row{
                    Text(
                        text = "Stok: ${alat.available_qty}/${alat.total_qty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // warna kondisi berdasarkan status
                    val kondisiColor = if (alat.condition == "good") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = alat.condition.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = kondisiColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Detail upload/sync status text
                val isSynced = alat.sync_status == "synced"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (isSynced) Color(0xFFE8F5E9) else Color(0xFFFFF3CD),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSynced) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isSynced) Color(0xFF2E7D32) else Color(0xFF856404)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSynced) "Terupload ke API Server (Online)" else "Tersimpan di Room Database (Offline)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isSynced) Color(0xFF2E7D32) else Color(0xFF856404)
                    )
                }
            }
        }
    }
}