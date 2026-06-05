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

    fun getAllAlat() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                alatList.value = repository.getAllAlat()
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
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

    fun deleteAlat(id: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.deleteAlat(id)
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