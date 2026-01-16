package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import java.util.UUID

@Keep
data class OrderItem(
    @get:PropertyName("orderItemId") @set:PropertyName("orderItemId")
    var orderItemId: String = UUID.randomUUID().toString(),
    
    @get:PropertyName("orderId") @set:PropertyName("orderId")
    var orderId: String = "",
    
    @get:PropertyName("itemId") @set:PropertyName("itemId")
    var itemId: String = "",
    
    @get:PropertyName("itemName") @set:PropertyName("itemName")
    var itemName: String = "",
    
    @get:PropertyName("quantity") @set:PropertyName("quantity")
    var quantity: Double = 0.0,
    
    @get:PropertyName("unitPrice") @set:PropertyName("unitPrice")
    var unitPrice: Double = 0.0,
    
    @get:PropertyName("parcelCharge") @set:PropertyName("parcelCharge")
    var parcelCharge: Double = 0.0,
    
    @get:PropertyName("subTotal") @set:PropertyName("subTotal")
    var subTotal: Double = 0.0,
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
)
