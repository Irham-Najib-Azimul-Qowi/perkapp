package com.example.perkapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.core.sync.SyncManager
import com.example.perkapp.core.ui.theme.PerkappTheme
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

import com.example.perkapp.core.datastore.UserPreferences
import com.example.perkapp.core.datastore.dataStore
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sync pending data if already logged in and online
        lifecycleScope.launch {
            val userPrefs = UserPreferences(applicationContext.dataStore)
            val token = userPrefs.getAuthToken.first()
            if (!token.isNullOrBlank() && com.example.perkapp.core.utils.NetworkUtils.isOnline(applicationContext)) {
                SyncManager.syncNow(applicationContext)
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
