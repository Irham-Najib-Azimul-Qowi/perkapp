package com.example.perkapp.core.features.kegiatan.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Image

data class DetailToolState(
    val id: String,
    val name: String,
    val category: String,
    val qty: Int,
    val isExternal: Boolean,
    val isReturned: Boolean,
    val isPending: Boolean = false,
    val imagePath: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailKegiatanScreen(
    kegiatanId: String,
    onBack: () -> Unit,
    onEditClick: (String) -> Unit = {},
    onDeleteSuccess: () -> Unit = {},
    viewModel: AktivitasViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val currentUserInfo by viewModel.currentUserInfo.collectAsState()

    // Normalize IDs (e.g. 101 -> 1, 102 -> 2, 103 -> 3)
    val normalizedId = remember(kegiatanId) {
        if (kegiatanId.startsWith("10") && kegiatanId.length > 2) {
            kegiatanId.substring(2)
        } else {
            kegiatanId
        }
    }

    LaunchedEffect(kegiatanId) {
        viewModel.loadActivities()
        viewModel.loadAlatForKegiatan(kegiatanId)
        viewModel.loadCurrentUserInfo()
    }

    val aktivitas = remember(uiState.aktivitasList, normalizedId) {
        uiState.aktivitasList.find { it.id == normalizedId } ?: uiState.aktivitasList.find { it.id == kegiatanId }
    }

    val toolList = uiState.currentDetailAlatList

    // Access permissions logic
    val currentUserName = currentUserInfo?.nama ?: ""
    val currentUserRole = currentUserInfo?.role ?: "member"
    val isAdmin = currentUserRole.lowercase() == "admin"
    val currentUserId = currentUserInfo?.id ?: ""
    val peminjamList = remember(aktivitas?.peminjam) {
        aktivitas?.peminjam?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }
    val hasAccess = remember(isAdmin, peminjamList, currentUserName, aktivitas, currentUserId) {
        val isCreator = aktivitas?.createdBy != null && aktivitas.createdBy == currentUserId
        isAdmin || isCreator || peminjamList.any { it.equals(currentUserName, ignoreCase = true) }
    }
    
    // Approval logic: admin can approve alat for member-created kegiatan
    val needsApproval = remember(aktivitas, isAdmin) {
        aktivitas != null && isAdmin && aktivitas.alatApproved == false && !aktivitas.isPending
    }
    val isApproved = aktivitas?.alatApproved ?: false

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Kegiatan") },
            text = { Text("Apakah Anda yakin ingin menghapus kegiatan ini? Stok alat yang sedang dipinjam akan dikembalikan ke inventaris.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteKegiatan(kegiatanId) {
                            Toast.makeText(context, "Kegiatan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            onDeleteSuccess()
                        }
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detail Kegiatan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        aktivitas?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                // Card Info Detail Kegiatan
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = data.judul,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Badge
                        val statusLabel = when (data.status) {
                            StatusAktivitas.BERLANGSUNG -> "In Progress"
                            StatusAktivitas.SELESAI -> "Completed"
                            StatusAktivitas.DRAFT -> "Draft"
                        }
                        val statusColor = when (data.status) {
                            StatusAktivitas.BERLANGSUNG -> MaterialTheme.colorScheme.primary
                            StatusAktivitas.SELESAI -> MaterialTheme.colorScheme.primary
                            StatusAktivitas.DRAFT -> MaterialTheme.colorScheme.secondary
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        DetailInfoRow(label = "Tanggal", value = data.tanggal)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailInfoRow(label = "Peminjam", value = data.peminjam.ifBlank { "-" })
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Deskripsi Kegiatan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = data.realDeskripsi.ifBlank { "Tidak ada deskripsi." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Approval Status Banner
                        Spacer(modifier = Modifier.height(16.dp))
                        if (!isApproved) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFFFFF3CD),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF856404)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alat menunggu persetujuan admin",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF856404)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alat telah disetujui ✓",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (hasAccess) {
                    // Section header for tools
                    Text(
                        text = "Daftar & Absensi Pengembalian Alat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )

                    // List of tool cards matching AlatCard style
                    toolList.forEach { toolEntity ->
                        val tool = DetailToolState(
                            id = toolEntity.id,
                            name = toolEntity.name,
                            category = toolEntity.category,
                            qty = toolEntity.qty,
                            isExternal = toolEntity.isExternal,
                            isReturned = toolEntity.isReturned,
                            isPending = toolEntity.sync_status == "pending",
                            imagePath = toolEntity.image_path
                        )
                        DetailKegiatanToolCard(
                            tool = tool,
                            onClick = {
                                val nextState = !toolEntity.isReturned
                                viewModel.updateKegiatanAlatStatus(toolEntity.id, nextState, kegiatanId)
                                
                                val toastMsg = if (nextState) {
                                    if (toolEntity.isExternal) {
                                        "Alat Luar '${toolEntity.name}' berhasil diabsen kembali ke pemilik luar!"
                                    } else {
                                        "Alat '${toolEntity.name}' berhasil diabsen kembali ke inventaris!"
                                    }
                                } else {
                                    "Absensi pengembalian '${toolEntity.name}' dibatalkan."
                                }
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    // Text info that tools are hidden
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Anda tidak memiliki akses ke daftar alat untuk kegiatan ini. Hanya pembuat atau peminjam terdaftar yang bisa melihat dan mengelola alat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons only visible if user has access (Creator/Borrower/Admin)
                if (hasAccess) {
                    // Approve button for admin (only shown for unapproved member-created kegiatan)
                    if (needsApproval) {
                        Button(
                            onClick = {
                                viewModel.approveAlat(kegiatanId) {
                                    Toast.makeText(context, "Alat berhasil disetujui!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            )
                        ) {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Setujui Alat",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Edit Button
                    Button(
                        onClick = { onEditClick(data.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Edit Kegiatan",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delete Button
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Hapus Kegiatan",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Back Button
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Text(
                        "Kembali",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DetailKegiatanToolCard(
    tool: DetailToolState,
    onClick: () -> Unit
) {
    val bitmap = com.example.perkapp.core.utils.rememberAsyncImage(tool.imagePath)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
            // Left Box matching AlatCard
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (tool.isExternal) MaterialTheme.colorScheme.secondaryContainer 
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
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
                    text = tool.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (tool.isExternal) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Category or External Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (tool.isExternal) MaterialTheme.colorScheme.secondaryContainer 
                                    else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (tool.isExternal) "Pinjaman Luar" else tool.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tool.isExternal) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity Text
                Text(
                    text = "Dipinjam: ${tool.qty} unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Return status banner at the bottom (matching AlatCard Room/API status banner)
                val bannerColor = if (tool.isPending) Color(0xFFFFF3CD) else if (tool.isReturned) Color(0xFFE8F5E9) else Color(0xFFFFF3CD)
                val bannerIcon = if (tool.isPending) Icons.Default.Info else if (tool.isReturned) Icons.Default.CheckCircle else Icons.Default.Info
                val bannerIconTint = if (tool.isPending) Color(0xFF856404) else if (tool.isReturned) Color(0xFF2E7D32) else Color(0xFF856404)
                val bannerText = if (tool.isPending) {
                    "Pending (Menunggu Sinkronisasi)"
                } else if (tool.isReturned) {
                    if (tool.isExternal) "Sudah Dikembalikan (Ke Pemilik Luar)" 
                    else "Sudah Dikembalikan (Masuk Inventaris)"
                } else {
                    if (tool.isExternal) "Belum Dikembalikan (Ketuk untuk Absen)"
                    else "Belum Dikembalikan (Ketuk untuk Absen)"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = bannerColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = bannerIcon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = bannerIconTint
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = bannerIconTint
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
