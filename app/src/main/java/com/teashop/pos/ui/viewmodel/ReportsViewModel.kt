package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.*
import com.teashop.pos.data.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(private val repository: MainRepository) : ViewModel() {

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _smartStock = MutableStateFlow<List<SmartStockReminder>>(emptyList())
    val smartStock: StateFlow<List<SmartStockReminder>> = _smartStock.asStateFlow()

    private val _cashFlow = MutableStateFlow<CashFlowSummary?>(null)
    val cashFlow: StateFlow<CashFlowSummary?> = _cashFlow.asStateFlow()

    private val _totalSalary = MutableStateFlow(0.0)
    val totalSalary: StateFlow<Double> = _totalSalary.asStateFlow()

    private val _totalFixedExpenses = MutableStateFlow(0.0)
    val totalFixedExpenses: StateFlow<Double> = _totalFixedExpenses.asStateFlow()

    private val _financeBreakdown = MutableStateFlow<List<FinancialCategorySummary>>(emptyList())
    val financeBreakdown: StateFlow<List<FinancialCategorySummary>> = _financeBreakdown.asStateFlow()

    private val _cashbookEntries = MutableStateFlow<List<Cashbook>>(emptyList())
    val cashbookEntries: StateFlow<List<Cashbook>> = _cashbookEntries.asStateFlow()

    private val _monthlyDailySummary = MutableStateFlow<List<DailyPosSummary>>(emptyList())
    val monthlyDailySummary: StateFlow<List<DailyPosSummary>> = _monthlyDailySummary.asStateFlow()

    private val _posBillReports = MutableStateFlow<List<OrderWithItems>>(emptyList())
    val posBillReports: StateFlow<List<OrderWithItems>> = _posBillReports.asStateFlow()

    private val _posReportSummary = MutableStateFlow<PosReportSummary?>(null)
    val posReportSummary: StateFlow<PosReportSummary?> = _posReportSummary.asStateFlow()

    private val _topSellingItems = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val topSellingItems: StateFlow<List<Pair<String, Int>>> = _topSellingItems.asStateFlow()

    private val _lowSellingItems = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val lowSellingItems: StateFlow<List<Pair<String, Int>>> = _lowSellingItems.asStateFlow()

    private var tickerJob: Job? = null
    private var reportJob: Job? = null

    fun loadEmployees(shopId: String) {
        viewModelScope.launch {
            repository.getShopEmployees(shopId).collect { 
                _employees.value = it
            }
        }
    }

    fun loadReportsForPeriod(
        shopId: String?,
        startTime: Long,
        endTime: Long,
        salary: Double
    ) {
        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            _totalSalary.value = salary

            launch {
                repository.getCashFlowSummary(shopId, startTime, endTime, salary).collect { summary ->
                    _cashFlow.value = summary
                    summary?.let { _totalFixedExpenses.value = it.fixedExpenses }
                }
            }

            launch {
                repository.getCashbookEntriesForPeriod(shopId, startTime, endTime).collect { entries ->
                    _cashbookEntries.value = entries
                    _financeBreakdown.value = entries.groupBy { Triple(it.category, it.description ?: "", it.transactionType) }
                        .map { (key, list) -> FinancialCategorySummary(key.first, key.second, list.sumOf { it.amount }, key.third) }
                }
            }
        }
    }

    fun loadPosReports(shopId: String, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            repository.getBilledOrdersWithItems(shopId, startTime, endTime).collect { bills ->
                _posBillReports.value = bills

                val totalSales = bills.sumOf { it.order.totalAmount }
                val cashSales = bills.filter { it.order.paymentMethod == "CASH" }.sumOf { it.order.totalAmount } + bills.filter { it.order.paymentMethod == "SPLIT" }.sumOf { it.order.cashAmount }
                val qrSales = bills.filter { it.order.paymentMethod == "ONLINE" }.sumOf { it.order.totalAmount } + bills.filter { it.order.paymentMethod == "SPLIT" }.sumOf { it.order.onlineAmount }
                _posReportSummary.value = PosReportSummary(totalSales, cashSales, qrSales)

                val itemCounts = mutableMapOf<String, Int>()
                bills.forEach { orderWithItems ->
                    orderWithItems.items.forEach { orderItem ->
                        itemCounts[orderItem.itemName] = (itemCounts[orderItem.itemName] ?: 0) + orderItem.quantity.toInt()
                    }
                }

                val sortedItems = itemCounts.entries.sortedByDescending { it.value }
                _topSellingItems.value = sortedItems.take(5).map { Pair(it.key, it.value) }
                _lowSellingItems.value = sortedItems.takeLast(5).map { Pair(it.key, it.value) }
            }
        }
    }

    fun deleteCashbookEntry(entry: Cashbook) {
        viewModelScope.launch {
            repository.deleteCashbookEntry(entry)
        }
    }

    fun deleteOrder(order: Order) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }

    fun updateOrderAndItems(order: Order, items: List<OrderItem>) {
        viewModelScope.launch {
            repository.updateOrder(order)
            items.forEach { repository.insertOrderItem(it) }
        }
    }
}

data class DailyPosSummary(val date: String, val cashTotal: Double, val onlineTotal: Double, val billCount: Int)
data class SmartStockReminder(val itemName: String, val currentStock: Double, val weekdayStandard: Double, val weekendStandard: Double, val supplierName: String?, val supplierContact: String?)
data class PosReportSummary(val totalSales: Double, val cashSales: Double, val qrSales: Double)
