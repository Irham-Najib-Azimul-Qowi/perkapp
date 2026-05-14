package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailAlatScreen(
    alatId: String,
    viewModel: AlatViewModel,
    onBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {}
) {
    val alat by viewModel.selectedAlat.observeAsState()

    LaunchedEffect(alatId) {
        viewModel.getAlatById(alatId)
    }

    Scaffold(
        topBar =  {
            TopAppBar(
                title = { Text("Detail Alat")},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            alat?.let { data ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation =  2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = data.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kategori: ${data.category}")
                        Text("Total Stok: ${data.total_qty}")
                        Text("Stok Tersedia: ${data.available_qty}")
                        Text("Kondisi: ${data.condition}")
                        Text("Status Sync: ${data.sync_status}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick =  { onEditClick(data.id)},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Alat")
                }
            } ?: run {
                Text("Memuat data...")
            }
        }
    }
}