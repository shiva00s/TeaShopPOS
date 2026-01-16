package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class Purchase(
    @get:PropertyName("purchaseId") @set:PropertyName("purchaseId")
    var purchaseId: String = "",
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("supplierId") @set:PropertyName("supplierId")
    var supplierId: String = "",
    
    @get:PropertyName("supplierName") @set:PropertyName("supplierName")
    var supplierName: String = "",
    
    @get:PropertyName("totalAmount") @set:PropertyName("totalAmount")
    var totalAmount: Double = 0.0,
    
    @get:PropertyName("paidAmount") @set:PropertyName("paidAmount")
    var paidAmount: Double = 0.0,
    
    @get:PropertyName("balanceAmount") @set:PropertyName("balanceAmount")
    var balanceAmount: Double = 0.0,
    
    @get:PropertyName("purchaseDate") @set:PropertyName("purchaseDate")
    var purchaseDate: Long = System.currentTimeMillis(),
    
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String? = null,
    
    @get:PropertyName("isSettled") @set:PropertyName("isSettled")
    var isSettled: Boolean = false
)
