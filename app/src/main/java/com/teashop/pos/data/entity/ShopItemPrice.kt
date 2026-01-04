package com.teashop.pos.data.entity

import androidx.room.Entity

@Entity(
    tableName = "shop_item_prices",
    primaryKeys = ["shopId", "itemId"]
)
data class ShopItemPrice(
    val shopId: String,
    val itemId: String,
    val sellingPrice: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
