package com.example.perkapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.core.sync.SyncManager
import com.example.perkapp.core.ui.theme.PerkappTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Silent Auto-login solusi sementara
        lifecycleScope.launch {
            if (com.example.perkapp.core.utils.NetworkUtils.isOnline(applicationContext)) {
                RetrofitClient.performSilentLogin(applicationContext)
            }
        }

        // Jadwalkan sync untuk data pending yang mungkin ada dari sesi sebelumnya
        SyncManager.scheduleSyncWhenOnline(applicationContext)

        setContent {
            PerkappTheme {
                PerkappApp()
            }
        }
    }
}
