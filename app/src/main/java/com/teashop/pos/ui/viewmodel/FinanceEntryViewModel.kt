package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinanceEntryViewModel @Inject constructor(
    private val repository: MainRepository,
    private val sharedViewModel: SharedViewModel
) : ViewModel() {

    val allItems: StateFlow<List<Item>> = repository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _shopId = MutableStateFlow("")

    val allSuppliers: StateFlow<List<Supplier>> = _shopId.flatMapLatest { shopId ->
        repository.getShopSuppliers(shopId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPurchaseDescriptions: StateFlow<List<String>> = _shopId.flatMapLatest { shopId ->
        repository.getCashbookEntriesForPeriod(shopId, 0, Long.MAX_VALUE)
            .map { entries -> entries.filter { it.category == "PURCHASE" }.mapNotNull { it.description }.distinct() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(Date())

    val recentEntries: StateFlow<List<Cashbook>> = combine(_shopId, _selectedDate) { shopId, date ->
        Pair(shopId, date)
    }.flatMapLatest { (shopId, date) ->
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        repository.getCashbookEntriesForPeriod(shopId, startOfDay, endOfDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedEntry = MutableStateFlow<Cashbook?>(null)
    val selectedEntry = _selectedEntry.asStateFlow()

    fun setShopId(shopId: String) {
        _shopId.value = shopId
    }

    fun setDate(date: Date) {
        _selectedDate.value = date
    }

    fun loadEntry(shopId: String, id: String) {
        viewModelScope.launch {
            repository.getCashbookEntriesForPeriod(shopId, 0, Long.MAX_VALUE).collect { entries ->
                _selectedEntry.value = entries.find { it.entryId == id }
            }
        }
    }

    fun addSupplier(shopId: String, name: String, contact: String) {
        viewModelScope.launch {
            repository.insertSupplier(
                Supplier(UUID.randomUUID().toString(), shopId, name, contact)
            )
        }
    }

    fun addFinancialEntry(
        shopId: String,
        amount: Double,
        type: String,
        category: String,
        description: String,
        transactionDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val entry = Cashbook(
                entryId = UUID.randomUUID().toString(),
                shopId = shopId,
                transactionType = type,
                category = category,
                amount = amount,
                description = description,
                transactionDate = transactionDate
            )
            repository.insertCashbookEntry(entry)
            recalculateShopProfit(shopId)
        }
    }

    fun updateFinancialEntry(entry: Cashbook, amount: Double, type: String, category: String, description: String, date: Long) {
        viewModelScope.launch {
            repository.insertCashbookEntry(entry.copy(
                amount = amount,
                transactionType = type,
                category = category,
                description = description,
                transactionDate = date,
                lastModified = System.currentTimeMillis()
            ))
            recalculateShopProfit(entry.shopId)
        }
    }

    fun deleteFinancialEntry(entry: Cashbook) {
        viewModelScope.launch {
            repository.deleteCashbookEntry(entry)
            recalculateShopProfit(entry.shopId)
        }
    }

    fun updateStock() {
        viewModelScope.launch {
            // Cloud-only stock logic
        }
    }

    fun recordPurchaseWithBalance(
        shopId: String,
        supplierId: String,
        supplierName: String,
        total: Double,
        paid: Double,
        date: Long
    ) {
        viewModelScope.launch {
            val purchaseId = UUID.randomUUID().toString()
            val balance = total - paid
            
            val purchase = Purchase(
                purchaseId = purchaseId,
                shopId = shopId,
                supplierId = supplierId,
                supplierName = supplierName,
                totalAmount = total,
                paidAmount = paid,
                balanceAmount = balance,
                purchaseDate = date,
                isSettled = balance <= 0
            )

            val cashbookEntry = if (paid > 0) {
                Cashbook(
                    entryId = UUID.randomUUID().toString(),
                    shopId = shopId,
                    transactionType = "OUT",
                    category = "PURCHASE",
                    amount = paid,
                    description = "Paid to $supplierName (Total: ₹$total)",
                    referenceId = purchaseId,
                    transactionDate = date
                )
            } else null

            repository.recordPurchase(purchase, cashbookEntry)
            recalculateShopProfit(shopId)
        }
    }

    private fun recalculateShopProfit(shopId: String) {
        viewModelScope.launch {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            repository.getCashFlowSummary(shopId, start, end).collect { summary ->
                if (summary != null) {
                    val profit = summary.totalIn - (summary.totalOut + summary.fixedExpenses)
                    sharedViewModel.updateShopProfit(shopId, profit)
                }
            }
        }
    }
}