package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey val purchaseId: String,
    val shopId: String,
    val supplierId: String,
    val supplierName: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val balanceAmount: Double, // The "Udhaar" to supplier
    val purchaseDate: Long = System.currentTimeMillis(),
    val description: String? = null,
    val isSettled: Boolean = false
)
