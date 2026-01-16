package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class ShopItemPrice(
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("itemId") @set:PropertyName("itemId")
    var itemId: String = "",
    
    @get:PropertyName("sellingPrice") @set:PropertyName("sellingPrice")
    var sellingPrice: Double = 0.0,
    
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
)
