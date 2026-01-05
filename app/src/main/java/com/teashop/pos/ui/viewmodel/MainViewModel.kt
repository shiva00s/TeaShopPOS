package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.Shop
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    private val repository: MainRepository
) : ViewModel() {

    private val startOfDay: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    val allShops: StateFlow<List<Shop>> = repository.getAllShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalTodaySales: StateFlow<Double?> = repository.reportDao.getGlobalTodaySales(startOfDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addShop(shop: Shop) {
        viewModelScope.launch {
            repository.insertShop(shop)
        }
    }
}
