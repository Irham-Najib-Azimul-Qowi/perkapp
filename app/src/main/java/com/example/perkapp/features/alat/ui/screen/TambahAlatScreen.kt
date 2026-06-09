package com.example.perkapp.features.alat.ui.screen

// Mengimpor kelas Bitmap untuk memproses dan menampilkan data gambar di memori
import android.graphics.Bitmap
// Mengimpor API Jetpack Compose untuk mengelola launcher hasil activity (kamera/galeri)
import androidx.activity.compose.rememberLauncherForActivityResult
// Mengimpor tipe kontrak standar activity untuk mengambil konten dari galeri atau kamera
import androidx.activity.result.contract.ActivityResultContracts
// Mengimpor fungsi ekstensi launch untuk memicu pembukaan kamera
import androidx.activity.result.launch
// Mengimpor komponen Image Compose untuk merender preview gambar di layar
import androidx.compose.foundation.Image
// Mengimpor modifier background untuk memberikan warna latar belakang pada tata letak
import androidx.compose.foundation.background
// Mengimpor komponen tata letak untuk mengatur perataan elemen
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
// Mengimpor state scroll untuk menangani kondisi ketika form di-scroll oleh user
import androidx.compose.foundation.rememberScrollState
// Mengimpor bentuk sudut membulat untuk mempercantik kolom input dan tombol
import androidx.compose.foundation.shape.RoundedCornerShape
// Mengimpor modifier verticalScroll agar halaman responsif jika konten melebihi batas layar
import androidx.compose.foundation.verticalScroll
// Mengimpor kumpulan ikon bawaan Material Design
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
// Mengimpor komponen tombol utama, warna tombol default, dan dropdown menu dari Material3
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
// Mengimpor anotasi Material 3 API eksperimental (seperti dropdown box & topappbar)
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
// Mengimpor fungsi daur hidup Compose untuk menyimpan status variabel (State)
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// Mengimpor livedata agar bisa memantau state loading dari ViewModel secara langsung
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// Mengimpor helper untuk mengubah bitmap biasa menjadi ImageBitmap yang dikenali Compose
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
// Mengimpor Context lokal untuk mengakses filesystem dan resource sistem Android
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// Mengimpor berkas utilitas penanganan gambar
import com.example.perkapp.core.utils.ImageUtils
// Mengimpor model data controller (ViewModel) alat
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

/**
 * FUNGSI: TambahAlatScreen
 * TUJUAN: Menyediakan antarmuka pendaftaran barang baru ke dalam sistem.
 *
 * ALUR LOGIKA PENGERJAAN:
 * 1. Mengelola banyak *State* untuk menyimpan tulisan di setiap kolom (Nama, Kategori, dst).
 * 2. Menyediakan 2 *Launcher* (Kamera & Galeri) bawaan sistem Android.
 *    - Kamera akan memotret, mengubahnya jadi `Bitmap`, dan menyimpannya ke memori internal.
 *    - Galeri akan mengambil foto (`URI`) dan menyalinnya ke memori internal.
 * 3. Ketika tombol "Simpan" diklik, sistem mengecek kelengkapan (*Validasi*) 
 *    menggunakan instruksi *If-Else*. Jika kosong, muncul `Toast` teguran.
 * 4. Jika valid, form akan diracik dan diserahkan pada `createAlat()` di ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class) // Mengaktifkan penggunaan komponen Material3 eksperimental
@Composable // Menandakan bahwa ini adalah fungsi UI deklaratif Jetpack Compose
fun TambahAlatScreen(
    viewModel: AlatViewModel, // Menerima parameter ViewModel pengelola data alat
    onBack: () -> Unit = {} // Parameter fungsi navigasi untuk kembali ke halaman sebelumnya
) {
    // Mengambil Context Android saat ini yang berlaku di dalam Compose Tree
    val context = LocalContext.current
    
    // Berlangganan status loading dari LiveData di ViewModel (Spinner akan berputar jika bernilai true)
    val isLoading by viewModel.isLoading.observeAsState(false)

    // Menyimpan state dari setiap inputan form (menggunakan remember agar nilainya tidak hilang saat recompose)
    var nama by remember { mutableStateOf("") } // String kosong untuk nama alat
    var kategori by remember { mutableStateOf("") } // String kosong untuk kategori alat
    var jumlah by remember { mutableStateOf("") } // String kosong untuk jumlah alat (input berupa teks terlebih dahulu)
    var kondisi by remember { mutableStateOf("good") } // Default kondisi diatur ke "good" (baik)
    var expandedKondisi by remember { mutableStateOf(false) } // State boolean untuk status buka/tutup menu dropdown kondisi
    
    // Menyimpan URI (alamat berkas) foto hasil tangkapan dalam bentuk teks URL lokal
    var imageUriString by remember { mutableStateOf<String?>(null) }
    // Menyimpan objek Bitmap hasil pengolahan gambar untuk ditampilkan sebagai preview
    var bitmapPreview by remember { mutableStateOf<Bitmap?>(null) }

    // Mendefinisikan launcher untuk membuka galeri foto bawaan perangkat
    val gallerylauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() // Menggunakan kontrak sistem untuk memilih file/konten
    ) { uri -> // Callback setelah user selesai memilih file dari galeri
        uri?.let { // Jika URI yang dikembalikan tidak bernilai null (user memilih gambar)
            // Copy gambar dari galeri ke penyimpanan internal aplikasi agar aman saat offline
            val file = ImageUtils.getFileFromUri(context, it.toString())
            if (file != null) {
                // Simpan alamat file lokal yang baru ke state URI gambar
                imageUriString = android.net.Uri.fromFile(file).toString()
            } else {
                // Gunakan URI asli jika salinan lokal gagal dibuat
                imageUriString = it.toString()
            }
            // Mengubah URI menjadi objek Bitmap agar bisa dirender sebagai preview di layar HP
            bitmapPreview = ImageUtils.loadBitmapFromUri(context, imageUriString)
        }
    }

    // Mendefinisikan launcher untuk membuka aplikasi kamera bawaan perangkat
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview() // Kontrak sistem untuk mengambil foto cepat (thumbnail bitmap)
    ) { bitmap -> // Callback setelah pengambilan foto dari kamera selesai
        bitmap?.let { // Jika hasil tangkapan kamera menghasilkan bitmap valid
            // Menyimpan objek bitmap hasil potretan tersebut ke file fisik JPG di folder internal app
            val savedUriString = ImageUtils.saveBitmapToFile(context, it)
            if (savedUriString != null) {
                // Memperbarui state alamat URI dengan alamat file gambar yang sukses tersimpan
                imageUriString = savedUriString
                // Menyimpan objek bitmap ke state preview agar langsung muncul di layar
                bitmapPreview = it
            }
        }
    }

    // Daftar opsi pilihan kondisi alat yang disediakan di dalam dropdown menu
    val kondisiOptions = listOf("good", "damaged")

    // Pengaturan warna kustom terpadu untuk semua komponen OutlinedTextField (input teks berbingkai)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary, // Warna bingkai saat kolom aktif difokuskan
        unfocusedBorderColor = MaterialTheme.colorScheme.outline, // Warna bingkai saat kolom tidak aktif
        focusedLabelColor = MaterialTheme.colorScheme.primary, // Warna label teks penunjuk saat kolom aktif
        cursorColor = MaterialTheme.colorScheme.primary // Warna garis kedip kursor teks
    )

    // Struktur layout dasar Material Design yang menyediakan kerangka bar navigasi atas (topbar)
    Scaffold(
        topBar = {
            // Bar navigasi atas halaman
            TopAppBar(
                title = {
                    // Judul halaman di sebelah kiri bar atas
                    Text(
                        "Tambah Alat",
                        style = MaterialTheme.typography.titleLarge, // Menggunakan ukuran teks besar bawaan tema
                        fontWeight = FontWeight.Bold // Menebalkan huruf judul
                    )
                },
                navigationIcon = {
                    // Tombol ikon di sebelah kiri judul (tombol kembali)
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, // Menggunakan ikon panah kiri default
                            contentDescription = "Kembali", // Deskripsi ikon untuk fitur pembaca layar aksesibilitas
                            tint = Color.White // Mewarnai ikon dengan warna putih
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // Latar belakang topbar mengikuti warna utama tema
                    titleContentColor = Color.White // Teks judul diwarnai putih
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Latar belakang dasar mengikuti warna default tema aplikasi
    ) { innerPadding -> // Memberikan padding dalam agar elemen tidak tertutup oleh TopAppBar di atas
        // Menyusun elemen masukan formulir secara vertikal
        Column(
            modifier = Modifier
                .padding(innerPadding) // Menerapkan padding penyeimbang area TopAppBar
                .padding(16.dp) // Memberikan jarak rongga 16dp di sekeliling sisi luar form
                // Menerapkan state scroll vertikal agar form dapat digulir jika layar HP pendek
                .verticalScroll(rememberScrollState())
        ) {
            // --- Input Nama Alat ---
            OutlinedTextField(
                value = nama, // Menampilkan teks nama yang sedang diketik user
                onValueChange = { nama = it }, // Memperbarui state 'nama' setiap kali user mengetik karakter baru
                modifier = Modifier.fillMaxWidth(), // Lebar kolom memenuhi ukuran maksimal layar
                label = { Text("Nama Alat") }, // Label teks penunjuk di dalam bingkai input
                shape = RoundedCornerShape(12.dp), // Membuat sudut kotak input melengkung 12dp
                colors = textFieldColors // Menerapkan skema warna terpadu yang telah didefinisikan sebelumnya
            )

            // Memberikan jarak vertikal pemisah antar kolom input sebesar 12dp
            Spacer(modifier = Modifier.height(12.dp))

            // --- Input Kategori ---
            OutlinedTextField(
                value = kategori, // Menampilkan teks kategori yang diinput user
                onValueChange = { kategori = it }, // Memperbarui state 'kategori'
                modifier = Modifier.fillMaxWidth(), // Lebar kolom memenuhi ukuran maksimal layar
                label = { Text("Kategori") }, // Teks label penunjuk kolom kategori
                shape = RoundedCornerShape(12.dp), // Sudut kotak input melengkung 12dp
                colors = textFieldColors // Skema warna terpadu
            )

            // Jarak vertikal pemisah antar kolom
            Spacer(modifier = Modifier.height(12.dp))

            // --- Input Jumlah ---
            OutlinedTextField(
                value = jumlah, // Menampilkan teks jumlah barang
                onValueChange = { jumlah = it }, // Memperbarui state 'jumlah'
                modifier = Modifier.fillMaxWidth(), // Lebar kolom memenuhi ukuran maksimal layar
                label = { Text("Jumlah") }, // Teks label penunjuk kolom jumlah
                shape = RoundedCornerShape(12.dp), // Sudut kotak input melengkung 12dp
                colors = textFieldColors // Skema warna terpadu
            )

            // Jarak vertikal pemisah
            Spacer(modifier = Modifier.height(12.dp))

            // --- Dropdown Pilihan Kondisi ---
            // Wadah khusus untuk mengaitkan dropdown menu dengan kotak teks inputnya
            ExposedDropdownMenuBox(
                expanded = expandedKondisi, // Status menu sedang terbuka atau tertutup
                onExpandedChange = { expandedKondisi = !expandedKondisi } // Membalikkan status buka/tutup saat diklik
            ) {
                OutlinedTextField(
                    // Menampilkan opsi terpilih dengan huruf pertama dikonversi menjadi huruf kapital (misal: "Good")
                    value = kondisi.replaceFirstChar { it.uppercase() },
                    onValueChange = {}, // Kosong karena input bersifat read-only (user harus memilih opsi)
                    readOnly = true, // Mengunci kolom teks agar user tidak bisa mengetik manual
                    label = { Text("Kondisi") }, // Label petunjuk kondisi
                    trailingIcon = {
                        // Menampilkan ikon panah kecil di sebelah kanan kolom untuk indikator dropdown
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKondisi)
                    },
                    // Mengaitkan anchor menu ke kolom input ini agar dropdown muncul tepat di bawah kotak teks
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), // Sudut kotak input melengkung 12dp
                    colors = textFieldColors // Skema warna terpadu
                )
                // Daftar menu pilihan yang melayang tepat di bawah kolom anchor
                ExposedDropdownMenu(
                    expanded = expandedKondisi, // Hanya dirender jika status bernilai true
                    onDismissRequest = { expandedKondisi = false } // Menutup dropdown saat user mengklik area luar menu
                ) {
                    // Melakukan perulangan untuk merender setiap opsi kondisi alat yang tersedia
                    kondisiOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.replaceFirstChar { it.uppercase() }) }, // Mengkapitalisasi huruf pertama opsi
                            onClick = {
                                kondisi = option // Menyimpan opsi terpilih ke state 'kondisi'
                                expandedKondisi = false // Menutup menu dropdown secara otomatis setelah dipilih
                            }
                        )
                    }
                }
            }

            // Memberikan jarak vertikal yang lebih besar sebelum bagian kelola gambar (20dp)
            Spacer(modifier = Modifier.height(20.dp))

            // --- Bagian Label Gambar Alat ---
            Text(
                text = "Gambar Alat", // Judul bagian upload gambar
                style = MaterialTheme.typography.titleSmall, // Menggunakan ukuran teks judul kecil
                fontWeight = FontWeight.SemiBold, // Ketebalan huruf semi-tebal
                color = MaterialTheme.colorScheme.onSurface // Warna teks menyesuaikan skema tema gelap/terang perangkat
            )

            // Jarak kecil ke barisan tombol gambar
            Spacer(modifier = Modifier.height(8.dp))

            // Menyusun tombol Kamera dan Galeri secara horizontal berdampingan
            Row(
                modifier = Modifier.fillMaxWidth(), // Lebar baris memenuhi ukuran maksimal layar
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Memberikan celah renggang 12dp di antara kedua tombol
            ) {
                // Tombol Buka Kamera
                OutlinedButton(
                    onClick = { cameraLauncher.launch() }, // Memicu pembukaan antarmuka kamera Android saat diklik
                    modifier = Modifier.weight(1f).height(48.dp), // Tombol berbagi lebar secara adil (bobot 1f) dan tinggi 48dp
                    shape = RoundedCornerShape(12.dp), // Sudut tombol melengkung 12dp
                    border = ButtonDefaults.outlinedButtonBorder // Menerapkan bingkai garis terluar default tombol
                ) {
                    Icon(
                        Icons.Default.CameraAlt, // Ikon Kamera
                        contentDescription = null, // Ikon dekoratif saja tanpa penjelasan suara aksesibilitas
                        modifier = Modifier.size(18.dp) // Ukuran ikon kamera 18dp
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // Jarak horizontal kecil antara ikon dengan teks tombol
                    Text("Kamera") // Label teks tombol kamera
                }

                // Tombol Buka Galeri
                OutlinedButton(
                    onClick = { gallerylauncher.launch("image/*") }, // Membuka galeri sistem Android dengan filter berkas gambar saja
                    modifier = Modifier.weight(1f).height(48.dp), // Tombol berbagi lebar secara adil dan tinggi 48dp
                    shape = RoundedCornerShape(12.dp), // Sudut tombol melengkung 12dp
                    border = ButtonDefaults.outlinedButtonBorder // Bingkai garis luar tombol
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary, // Ikon Galeri Gambar
                        contentDescription = null, // Ikon dekoratif tanpa deskripsi suara
                        modifier = Modifier.size(18.dp) // Ukuran ikon galeri 18dp
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // Jarak horizontal kecil antara ikon dengan teks
                    Text("Galeri") // Label teks tombol galeri
                }
            }

            // Jarak vertikal sebelum tampilan preview gambar
            Spacer(modifier = Modifier.height(12.dp))

            // Tampilkan foto hasil jepretan kamera/galeri jika variabel 'bitmapPreview' tidak bernilai null
            bitmapPreview?.let {
                // Box sebagai wadah pembungkus gambar agar memiliki efek latar belakang dan sudut melengkung
                Box(
                    modifier = Modifier
                        .fillMaxWidth() // Lebar wadah memenuhi ukuran layar maksimal
                        .height(180.dp) // Tinggi wadah diatur 180dp
                        .clip(RoundedCornerShape(12.dp)) // Memotong sudut luar agar membulat 12dp sesuai wadah
                        .background(MaterialTheme.colorScheme.surfaceVariant) // Latar belakang wadah varian permukaan abu-abu/terang
                ) {
                    Image(
                        bitmap = it.asImageBitmap(), // Mengubah bitmap reguler menjadi bitmap siap render di Compose
                        contentDescription = "Preview Gambar", // Deskripsi aksesibilitas gambar
                        contentScale = ContentScale.Crop, // Memotong sisi gambar secara merata agar pas memenuhi ruang 180dp
                        modifier = Modifier
                            .fillMaxWidth() // Lebar gambar memenuhi wadah
                            .height(180.dp) // Tinggi gambar memenuhi wadah
                            .clip(RoundedCornerShape(12.dp)) // Sudut gambar membulat 12dp
                    )
                }
            }

            // Jarak vertikal sebelum tombol simpan utama
            Spacer(modifier = Modifier.height(24.dp))

            // --- Tombol Simpan Formulir ---
            Button(
                onClick = {
                    // Validasi Dasar agar data masukan formulir tidak kosong
                    if (nama.isBlank()) {
                        // Memberi teguran pop-up jika nama alat kosong
                        android.widget.Toast.makeText(context, "Nama Alat tidak boleh kosong", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button // Menghentikan eksekusi lambda tombol
                    }
                    if (kategori.isBlank()) {
                        // Memberi teguran pop-up jika kategori kosong
                        android.widget.Toast.makeText(context, "Kategori tidak boleh kosong", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button // Menghentikan eksekusi
                    }
                    // Mengonversi teks jumlah ke tipe data Integer, jika bukan angka valid default ke 0
                    val qty = jumlah.toIntOrNull() ?: 0
                    if (qty <= 0) {
                        // Memberi teguran jika jumlah alat tidak diinput angka valid di atas 0
                        android.widget.Toast.makeText(context, "Jumlah harus lebih dari 0", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button // Menghentikan eksekusi
                    }
                    
                    // Mengirimkan seluruh data masukan valid ke fungsi createAlat di ViewModel untuk diolah lebih lanjut
                    viewModel.createAlat(nama, kategori, qty, kondisi, imageUriString ?: "")
                    onBack() // Melakukan navigasi kembali ke halaman daftar inventaris (halaman sebelumnya)
                },
                enabled = !isLoading, // Menonaktifkan tombol ketika aplikasi sedang melakukan proses simpan/sinkronisasi
                modifier = Modifier
                    .fillMaxWidth() // Lebar tombol memenuhi batas maksimal horizontal
                    .height(52.dp), // Tinggi tombol diatur kokoh setinggi 52dp
                shape = RoundedCornerShape(12.dp), // Sudut tombol membulat melengkung 12dp
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary // Latar belakang tombol utama menggunakan warna primer aplikasi
                )
            ) {
                Text(
                    // Mengubah label tombol secara dinamis mengikuti status loading data
                    if (isLoading) "Menyimpan..." else "Simpan",
                    style = MaterialTheme.typography.labelLarge, // Menggunakan ukuran font teks label bawaan tema
                    fontWeight = FontWeight.SemiBold // Menebalkan tulisan label
                )
            }
        }
    }
}