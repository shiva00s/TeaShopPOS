package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stock Philosophy: Stock is derived from movements.
 */
@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey val movementId: String,
    val shopId: String,
    val itemId: String,
    val quantity: Double, // Positive for IN (Purchase/Adjustment), Negative for OUT (Sale/Wastage)
    val movementType: String, // SALE, PURCHASE, WASTAGE, ADJUSTMENT
    val referenceId: String? = null, // OrderId or PurchaseId
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
