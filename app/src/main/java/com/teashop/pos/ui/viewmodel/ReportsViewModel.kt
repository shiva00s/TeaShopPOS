package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.AppDatabase
import com.teashop.pos.data.dao.CashFlowSummary
import com.teashop.pos.data.dao.FinancialCategorySummary
import com.teashop.pos.data.dao.ReportDao
import com.teashop.pos.data.dao.StockStatus
import com.teashop.pos.data.entity.Purchase
import com.teashop.pos.data.entity.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportsViewModel(private val db: AppDatabase) : ViewModel() {

    private val reportDao = db.reportDao()
    private val purchaseDao = db.purchaseDao()

    private val _cashFlow = MutableStateFlow<CashFlowSummary?>(null)
    val cashFlow: StateFlow<CashFlowSummary?> = _cashFlow.asStateFlow()

    private val _financeBreakdown = MutableStateFlow<List<FinancialCategorySummary>>(emptyList())
    val financeBreakdown: StateFlow<List<FinancialCategorySummary>> = _financeBreakdown.asStateFlow()

    private val _stockStatus = MutableStateFlow<List<StockStatus>>(emptyList())
    val stockStatus: StateFlow<List<StockStatus>> = _stockStatus.asStateFlow()

    private val _lowStockAlerts = MutableStateFlow<List<StockStatus>>(emptyList())
    val lowStockAlerts: StateFlow<List<StockStatus>> = _lowStockAlerts.asStateFlow()

    private val _pendingPayments = MutableStateFlow<List<Purchase>>(emptyList())
    val pendingPayments: StateFlow<List<Purchase>> = _pendingPayments.asStateFlow()

    private val _activeReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val activeReminders: StateFlow<List<Reminder>> = _activeReminders.asStateFlow()

    fun loadReportsForPeriod(shopId: String, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            reportDao.getCashFlowSummary(shopId, startTime, endTime).collect {
                _cashFlow.value = it
            }
        }

        viewModelScope.launch {
            reportDao.getShopFinancialBreakdown(shopId, startTime, endTime).collect {
                _financeBreakdown.value = it
            }
        }

        // Keep alerts and pending global for the shop
        viewModelScope.launch {
            reportDao.getLowStockAlerts(shopId).collect { _lowStockAlerts.value = it }
        }
        viewModelScope.launch {
            purchaseDao.getPendingPayments(shopId).collect { _pendingPayments.value = it }
        }
    }
}
