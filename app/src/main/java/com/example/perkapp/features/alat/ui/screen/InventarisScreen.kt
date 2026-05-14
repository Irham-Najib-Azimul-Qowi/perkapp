package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.alat.ui.component.AlatCard
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel
import kotlin.collections.emptyList

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
        topBar =  {
            TopAppBar(title = { Text("Inventaris Alat")})
        },
        floatingActionButton = {
            FloatingActionButton(onClick =  onAddClick) {
                Icon(Icons.Default.Add, contentDescription =  "Tambah Alat")
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                items(alatList) { alat ->
                    AlatCard(
                        alat = alat,
                        onClick = { onItemClick(alat.id)}
                    )
                }
            }
        }
    }
}