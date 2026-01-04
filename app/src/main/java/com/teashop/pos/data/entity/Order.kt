package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val orderId: String,
    val shopId: String,
    val tableId: String? = null,
    val serviceType: String, // TABLE, STANDING, PARCEL
    val totalAmount: Double = 0.0,
    val discount: Double = 0.0,
    val payableAmount: Double = 0.0,
    val paymentStatus: String = "PENDING", // PENDING, PAID
    val paymentMethod: String? = null, // CASH, ONLINE, CREDIT
    val status: String = "OPEN", // OPEN, CLOSED, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    
    // Sync Metadata
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
