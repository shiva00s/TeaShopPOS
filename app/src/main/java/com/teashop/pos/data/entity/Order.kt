package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import java.util.UUID

@Keep
data class Order(
    @get:PropertyName("orderId") @set:PropertyName("orderId")
    var orderId: String = UUID.randomUUID().toString(),
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("tableId") @set:PropertyName("tableId")
    var tableId: String? = null,
    
    @get:PropertyName("serviceType") @set:PropertyName("serviceType")
    var serviceType: String = "STANDING",
    
    @get:PropertyName("totalAmount") @set:PropertyName("totalAmount")
    var totalAmount: Double = 0.0,
    
    @get:PropertyName("discount") @set:PropertyName("discount")
    var discount: Double = 0.0,
    
    @get:PropertyName("payableAmount") @set:PropertyName("payableAmount")
    var payableAmount: Double = 0.0,
    
    @get:PropertyName("paymentStatus") @set:PropertyName("paymentStatus")
    var paymentStatus: String = "PENDING",
    
    @get:PropertyName("paymentMethod") @set:PropertyName("paymentMethod")
    var paymentMethod: String? = null,
    
    @get:PropertyName("cashAmount") @set:PropertyName("cashAmount")
    var cashAmount: Double = 0.0,
    
    @get:PropertyName("onlineAmount") @set:PropertyName("onlineAmount")
    var onlineAmount: Double = 0.0,
    
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "OPEN",
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    
    @get:PropertyName("closedAt") @set:PropertyName("closedAt")
    var closedAt: Long? = null,
    
    @get:PropertyName("lastModified") @set:PropertyName("lastModified")
    var lastModified: Long = System.currentTimeMillis(),
    
    @get:PropertyName("isSynced") @set:PropertyName("isSynced")
    var isSynced: Boolean = false
)
