package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.dao.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportsViewModel(private val repository: MainRepository) : ViewModel() {

    private val _smartStock = MutableStateFlow<List<SmartStockReminder>>(emptyList())
    val smartStock: StateFlow<List<SmartStockReminder>> = _smartStock.asStateFlow()

    private val _cashFlow = MutableStateFlow<CashFlowSummary?>(null)
    val cashFlow: StateFlow<CashFlowSummary?> = _cashFlow.asStateFlow()

    private val _financeBreakdown = MutableStateFlow<List<FinancialCategorySummary>>(emptyList())
    val financeBreakdown: StateFlow<List<FinancialCategorySummary>> = _financeBreakdown.asStateFlow()

    fun loadReportsForPeriod(shopId: String, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            repository.reportDao.getSmartStockReminders(shopId).collect {
                _smartStock.value = it
            }
        }
        viewModelScope.launch {
            repository.reportDao.getCashFlowSummary(shopId, startTime, endTime).collect {
                _cashFlow.value = it
            }
        }
        viewModelScope.launch {
            repository.reportDao.getShopFinancialBreakdown(shopId, startTime, endTime).collect {
                _financeBreakdown.value = it
            }
        }
    }
}
