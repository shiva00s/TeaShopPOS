package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import java.util.UUID

@Keep
data class StockMovement(
    @get:PropertyName("movementId") @set:PropertyName("movementId")
    var movementId: String = UUID.randomUUID().toString(),
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("itemId") @set:PropertyName("itemId")
    var itemId: String = "",
    
    @get:PropertyName("quantity") @set:PropertyName("quantity")
    var quantity: Double = 0.0,
    
    @get:PropertyName("movementType") @set:PropertyName("movementType")
    var movementType: String = "ADJUSTMENT",
    
    @get:PropertyName("referenceId") @set:PropertyName("referenceId")
    var referenceId: String? = null,
    
    @get:PropertyName("reason") @set:PropertyName("reason")
    var reason: String? = null,
    
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)
