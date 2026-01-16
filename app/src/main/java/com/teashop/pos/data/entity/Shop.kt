package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import java.util.UUID

@Keep
data class Shop(
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = UUID.randomUUID().toString(),
    
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("location") @set:PropertyName("location")
    var location: String = "",
    
    @get:PropertyName("openingDate") @set:PropertyName("openingDate")
    var openingDate: Long = System.currentTimeMillis(),
    
    @get:PropertyName("openingCashBalance") @set:PropertyName("openingCashBalance")
    var openingCashBalance: Double = 0.0,
    
    @get:PropertyName("tableCount") @set:PropertyName("tableCount")
    var tableCount: Int = 0,
    
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
)
