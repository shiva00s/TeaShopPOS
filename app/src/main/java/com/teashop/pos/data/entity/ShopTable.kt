package com.teashop.pos.data.entity

import androidx.room.Entity

@Entity(
    tableName = "shop_tables",
    primaryKeys = ["shopId", "tableId"]
)
data class ShopTable(
    val shopId: String,
    val tableId: String,
    val tableName: String, // T1, T2, etc.
    val status: String = "FREE", // FREE, RUNNING, BILL_READY
    val currentOrderId: String? = null
)
