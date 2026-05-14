package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel
import java.util.NavigableMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahAlatScreen(
    viewModel: AlatViewModel,
    onBack: () -> Unit = {}
){
    var nama by remember {
        mutableStateOf("")
    }

    var kategori by remember {
        mutableStateOf("")
    }

    var jumlah by remember {
        mutableStateOf("")
    }

    var kondisi by remember { mutableStateOf("good")}
    var expandedKondisi by remember { mutableStateOf(false) }

    val kondisiOptions = listOf("good", "damaged")

    Scaffold(
        topBar =  {
            TopAppBar(
                title = { Text("Tambah Alat")},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value =  nama,
                onValueChange = { nama = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Alat")}
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = kategori,
                onValueChange = { kategori = it },
                modifier = Modifier.fillMaxWidth(),
                label = {Text("Kategori")}
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = jumlah,
                onValueChange =  { jumlah = it},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Jumlah")}
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded =  expandedKondisi,
                onExpandedChange = { expandedKondisi = !expandedKondisi}
            ) {
                OutlinedTextField(
                    value = kondisi,
                    onValueChange =  {},
                    readOnly = true,
                    label = { Text("Kondisi")},
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKondisi)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedKondisi,
                    onDismissRequest =  { expandedKondisi = false}
                ) { kondisiOptions.forEach { option ->
                    DropdownMenuItem(
                        text =  { Text(option)},
                        onClick =  {
                            kondisi = option
                            expandedKondisi = false
                        }
                    )
                }

                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick =  {
                    val qty = jumlah.toIntOrNull() ?: 0
                    if (nama.isNotBlank() && kategori.isNotBlank() && qty > 0) {
                        viewModel.createAlat(nama, kategori, qty, kondisi)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan")
            }
        }
    }
}