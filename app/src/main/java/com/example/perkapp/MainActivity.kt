package com.example.perkapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.core.sync.SyncManager
import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.repository.AlatRepository
import com.example.perkapp.features.alat.ui.screen.DetailAlatScreen
import com.example.perkapp.features.alat.ui.screen.EditAlatScreen
import com.example.perkapp.features.alat.ui.screen.InventarisScreen
import com.example.perkapp.features.alat.ui.screen.TambahAlatScreen
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModelFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.perkapp.ui.theme.PerkappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val alatDao = database.alatDao()
        val alatApi = RetrofitClient.instance.create(AlatApiService::class.java)
        val alatRepository = AlatRepository(alatApi, alatDao, applicationContext)
        val alatViewModelFactory = AlatViewModelFactory(alatRepository, application)

        // Silent Auto-login solusi sementara karena halaman login belum dibuat
        lifecycleScope.launch {
            if (com.example.perkapp.core.utils.NetworkUtils.isOnline(applicationContext)) {
                RetrofitClient.performSilentLogin(applicationContext)
            }
        }

        // Jadwalkan sync untuk data pending yang mungkin ada dari sesi sebelumnya
        SyncManager.scheduleSyncWhenOnline(applicationContext)

        setContent {
            PerkappTheme {
                val navController = rememberNavController()
                val alatViewModel: AlatViewModel = viewModel(factory = alatViewModelFactory)

                NavHost(
                    navController = navController,
                    startDestination =  "inventaris"
                ) {
                    composable("inventaris"){
                        InventarisScreen(
                            viewModel = alatViewModel,
                            onAddClick =  {
                                navController.navigate("tambah_alat")
                            },
                            onItemClick =  { id ->
                                navController.navigate("detail_alat/$id")
                            }
                        )
                    }

                    composable("tambah_alat") {
                        TambahAlatScreen(
                            viewModel = alatViewModel,
                            onBack = { navController.popBackStack()}
                        )
                    }

                    composable(
                        route = "detail_alat/{alatId}",
                        arguments =  listOf(
                            navArgument("alatId") { type = NavType.StringType}
                        )
                    ) {
                        backStackEntry ->
                        val alatId = backStackEntry.arguments?.getString("alatId") ?: ""
                        DetailAlatScreen(
                            alatId = alatId,
                            viewModel = alatViewModel,
                            onBack = { navController.popBackStack()},
                            onEditClick = { id ->
                                navController.navigate("edit_alat/$id")
                            },
                            onDeleteClick = { id ->
                                alatViewModel.deleteAlat(id)
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = "edit_alat/{alatId}",
                        arguments =  listOf(
                            navArgument("alatId") { type = NavType.StringType}
                        )
                    ) { backStackEntry ->
                        val alatId = backStackEntry.arguments?.getString("alatId") ?: ""
                        EditAlatScreen(
                            alatId = alatId,
                            viewModel = alatViewModel,
                            onBack =  { navController.popBackStack()}
                        )
                    }
                }
            }
        }
    }
}
