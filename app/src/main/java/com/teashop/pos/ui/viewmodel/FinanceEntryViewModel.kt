package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class FinanceEntryViewModel(private val repository: MainRepository) : ViewModel() {

    val allItems: StateFlow<List<Item>> = repository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<Supplier>> = repository.getShopSuppliers("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedEntry = MutableStateFlow<Cashbook?>(null)
    val selectedEntry: StateFlow<Cashbook?> = _selectedEntry.asStateFlow()

    fun loadEntry(entryId: String) {
        viewModelScope.launch {
            _selectedEntry.value = repository.getCashbookEntry(entryId)
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
        }
    }

    fun updateFinancialEntry(
        entry: Cashbook,
        amount: Double,
        type: String,
        category: String,
        description: String,
        transactionDate: Long
    ) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(
                amount = amount,
                transactionType = type,
                category = category,
                description = description,
                transactionDate = transactionDate
            )
            repository.updateCashbookEntry(updatedEntry)
        }
    }

    fun updateStock(
        shopId: String,
        itemId: String,
        quantity: Double,
        type: String,
        transactionDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val movement = StockMovement(
                movementId = UUID.randomUUID().toString(),
                shopId = shopId,
                itemId = itemId,
                quantity = quantity,
                movementType = type,
                timestamp = transactionDate,
                reason = "Manual Update: $type"
            )
            repository.insertStockMovement(movement)
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
        }
    }
}
