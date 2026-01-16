package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class Item(
    @get:PropertyName("itemId") @set:PropertyName("itemId")
    var itemId: String = "",
    
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "",
    
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    
    @get:PropertyName("hasParcelCharge") @set:PropertyName("hasParcelCharge")
    var hasParcelCharge: Boolean = false,
    
    @get:PropertyName("defaultParcelCharge") @set:PropertyName("defaultParcelCharge")
    var defaultParcelCharge: Double = 5.0,
    
    @get:PropertyName("globalPrice") @set:PropertyName("globalPrice")
    var globalPrice: Double = 0.0,
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
)
