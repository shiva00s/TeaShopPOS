package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.teashop.pos.data.MainRepository

/**
 * A centralized factory for creating all ViewModels that have a dependency on MainRepository.
 * This avoids boilerplate code in each Activity.
 */
class ViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(repository) as T
            }
            modelClass.isAssignableFrom(POSViewModel::class.java) -> {
                POSViewModel(repository) as T
            }
            modelClass.isAssignableFrom(StaffViewModel::class.java) -> {
                StaffViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ItemMasterViewModel::class.java) -> {
                ItemMasterViewModel(repository) as T
            }
            modelClass.isAssignableFrom(FinanceEntryViewModel::class.java) -> {
                FinanceEntryViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> {
                ReportsViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
