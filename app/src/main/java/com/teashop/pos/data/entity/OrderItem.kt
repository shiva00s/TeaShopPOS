package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey val orderItemId: String,
    val orderId: String,
    val itemId: String,
    val itemName: String, // Snapshotted name at time of sale
    val quantity: Double,
    val unitPrice: Double,
    val subTotal: Double,
    val createdAt: Long = System.currentTimeMillis()
)
