package com.example.perkapp.features.alat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.perkapp.core.sync.SyncManager
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import com.example.perkapp.features.alat.data.repository.AlatRepository
import kotlinx.coroutines.launch

class AlatViewModel(
    private val repository: AlatRepository,
    private val application: Application
) : ViewModel(){
    val alatList = MutableLiveData<List<AlatEntity>>()
    val selectedAlat = MutableLiveData<AlatEntity?>()
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>()

    init {
        getAllAlat()
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            com.example.perkapp.core.utils.NetworkUtils.observeNetworkStatus(application)
                .collect { isOnline ->
                    if (isOnline) {
                        try {
                            repository.syncPendingData()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            getAllAlat()
                        }
                    }
                }
        }
    }

    fun getAllAlat() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                if (com.example.perkapp.core.utils.NetworkUtils.isOnline(application)) {
                    try {
                        repository.syncPendingData()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                alatList.value = repository.getAllAlat()
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
        if (isLoading.value == true) return
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.createAlat(name, category, totalQty, condition, imagePath)
                // Jadwalkan sync jika ada data pending
                SyncManager.scheduleSyncWhenOnline(application)
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                getAllAlat()
                isLoading.value = false
            }
        }
    }

    fun getAlatById(id: String) {
        viewModelScope.launch {
            selectedAlat.value = repository.getAlatById(id)
        }
    }

    fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.updateAlat(alat, request)
                // Jadwalkan sync jika ada data pending
                SyncManager.scheduleSyncWhenOnline(application)
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                getAllAlat()
                isLoading.value = false
            }
        }
    }

    fun deleteAlat(id: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Cek apakah alat sedang dipinjam oleh kegiatan
                val db = com.example.perkapp.core.database.AppDatabase.getDatabase(application)
                val borrowings = db.kegiatanDao().getActiveBorrowingsForAlat(id)
                if (borrowings.isNotEmpty()) {
                    throw Exception("Alat sedang dipinjam oleh kegiatan dan tidak bisa dihapus!")
                }

                repository.deleteAlat(id)
                // Jadwalkan sync jika ada data pending
                SyncManager.scheduleSyncWhenOnline(application)
                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = e.message
                onError(e.message ?: "Gagal menghapus alat")
            } finally {
                getAllAlat()
                isLoading.value = false
            }
        }
    }
}

class AlatViewModelFactory(
    private val repository: AlatRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
         if (modelClass.isAssignableFrom(AlatViewModel::class.java)) {
             @Suppress("UNCHECKED_CAST")
             return AlatViewModel(repository, application) as T
         }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}