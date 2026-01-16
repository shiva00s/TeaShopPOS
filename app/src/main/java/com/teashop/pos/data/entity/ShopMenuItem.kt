package com.teashop.pos.data.entity

import androidx.annotation.Keep

@Keep
data class ShopMenuItem(
    val item: Item = Item(),
    val finalPrice: Double = 0.0,
    val salesCount: Int = 0
)
