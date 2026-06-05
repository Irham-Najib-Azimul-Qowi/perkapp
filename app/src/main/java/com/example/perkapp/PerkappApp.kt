package com.example.perkapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.perkapp.core.navigation.BottomBar
import com.example.perkapp.core.navigation.SetupNavGraph

@Composable
fun PerkappApp() {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        // innerPadding adalah ruang sisa setelah dikurangi tinggi BottomBar (jika BottomBar tampil)
        SetupNavGraph(navController = navController, paddingValues = innerPadding)
    }
}
