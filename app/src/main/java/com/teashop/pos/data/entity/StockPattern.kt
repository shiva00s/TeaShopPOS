package com.teashop.pos.data.entity

import androidx.room.Entity

@Entity(
    tableName = "stock_patterns",
    primaryKeys = ["itemId", "shopId"] // Corrected: Composite Primary Key
)
data class StockPattern(
    val itemId: String,
    val shopId: String,
    val weekdayStandard: Double, // e.g. 20.0
    val weekendStandard: Double, // e.g. 25.0
    val supplierId: String? = null // Preferred supplier for this item
)
