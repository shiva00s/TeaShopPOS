package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cashbook")
data class Cashbook(
    @PrimaryKey val entryId: String,
    val shopId: String,
    val transactionType: String, // IN, OUT
    val category: String, // SALES, EXPENSE, PURCHASE, SALARY, ADVANCE, CREDIT_SETTLEMENT, OTHER
    val amount: Double,
    val description: String? = null,
    val referenceId: String? = null, // OrderId, ExpenseId, etc.
    val transactionDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),

    // Sync Metadata
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
