package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class Supplier(
    @get:PropertyName("supplierId") @set:PropertyName("supplierId")
    var supplierId: String = "",
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("contact") @set:PropertyName("contact")
    var contact: String = "",
    
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String? = null,
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
)
