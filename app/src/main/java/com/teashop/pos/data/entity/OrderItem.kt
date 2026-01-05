package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey val orderItemId: String,
    val orderId: String,
    val itemId: String,
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val parcelCharge: Double = 0.0, // Added for your requirement
    val subTotal: Double,
    val createdAt: Long = System.currentTimeMillis()
)
