package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class ShopTable(
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("tableId") @set:PropertyName("tableId")
    var tableId: String = "",
    
    @get:PropertyName("tableName") @set:PropertyName("tableName")
    var tableName: String = "",
    
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "FREE",
    
    @get:PropertyName("currentOrderId") @set:PropertyName("currentOrderId")
    var currentOrderId: String? = null
)
