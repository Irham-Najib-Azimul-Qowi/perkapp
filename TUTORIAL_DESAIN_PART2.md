# Tutorial Mengubah Desain Perkapp — Part 2
## Modifikasi Komponen UI: InventarisScreen & AlatCard

> **Prasyarat**: Sudah menyelesaikan [TUTORIAL_DESAIN_PART1.md](./TUTORIAL_DESAIN_PART1.md)

---

## Tahap 1: Modifikasi `InventarisScreen.kt`

### File: `app/src/main/java/com/example/perkapp/features/alat/ui/screen/InventarisScreen.kt`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```kotlin
package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.features.alat.ui.component.AlatCard
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarisScreen(
    viewModel: AlatViewModel,
    onAddClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    val alatList by viewModel.alatList.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    LaunchedEffect(Unit) {
        viewModel.getAllAlat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inventaris Alat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah Alat",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        } else if (alatList.isEmpty()) {
            // Tampilan ketika daftar kosong
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Belum ada alat",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tekan tombol + untuk menambah alat baru",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(alatList) { alat ->
                    AlatCard(
                        alat = alat,
                        onClick = { onItemClick(alat.id) }
                    )
                }
            }
        }
    }
}
```

### Apa yang berubah?
1. **TopAppBar** sekarang berwarna hijau primary (`#22C55E`) dengan teks putih
2. **FAB** menggunakan shape `CircleShape` dengan shadow, ukuran lebih besar (64dp)
3. **CircularProgressIndicator** menggunakan warna primary
4. **Ditambahkan** tampilan "empty state" ketika daftar kosong
5. **LazyColumn** ditambahkan `contentPadding` dan `verticalArrangement` untuk spacing

---

## Tahap 2: Modifikasi `AlatCard.kt`

### File: `app/src/main/java/com/example/perkapp/features/alat/ui/component/AlatCard.kt`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```kotlin
package com.example.perkapp.features.alat.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.perkapp.core.utils.ImageUtils
import com.example.perkapp.features.alat.data.local.AlatEntity

@Composable
fun AlatCard(
    alat: AlatEntity,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val bitmap = remember(alat.image_path) {
        ImageUtils.loadBitmapFromUri(context, alat.image_path)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gambar Alat
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
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

            Spacer(modifier = Modifier.width(16.dp))

            // Info Alat
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Label Kategori (Chip style)
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = alat.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stok: ${alat.available_qty}/${alat.total_qty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Warna kondisi berdasarkan status
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
            }
        }
    }
}
```

### Apa yang berubah?
1. **Card shape** menjadi `RoundedCornerShape(16.dp)` — sudut lebih membulat
2. **Gambar** di-clip dengan rounded corner, dan jika tidak ada gambar menampilkan huruf pertama nama alat
3. **Kategori** ditampilkan dalam chip/label berwarna hijau muda (`primaryContainer`)
4. **Kondisi** ditampilkan dengan warna: hijau untuk "good", merah untuk "damaged"
5. **Layout** dirapikan dengan alignment dan spacing yang konsisten
6. **Teks overflow** dibatasi 1 baris dengan ellipsis

---

## ✅ Checklist Part 2

Setelah selesai Part 2, pastikan:

- [ ] `InventarisScreen.kt` sudah diperbarui — TopAppBar hijau, FAB bulat
- [ ] `AlatCard.kt` sudah diperbarui — card rounded, chip kategori, warna kondisi
- [ ] Build project dan pastikan tidak ada error

> ➡️ **Lanjut ke [TUTORIAL_DESAIN_PART3.md](./TUTORIAL_DESAIN_PART3.md)** untuk modifikasi DetailAlatScreen dan TambahAlatScreen
