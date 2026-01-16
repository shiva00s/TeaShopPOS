package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class AdvancePayment(
    @get:PropertyName("advanceId") @set:PropertyName("advanceId")
    var advanceId: String = "",
    
    @get:PropertyName("employeeId") @set:PropertyName("employeeId")
    var employeeId: String = "",
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Double = 0.0,
    
    @get:PropertyName("date") @set:PropertyName("date")
    var date: Long = System.currentTimeMillis(),
    
    @get:PropertyName("isRecovered") @set:PropertyName("isRecovered")
    var isRecovered: Boolean = false,
    
    @get:PropertyName("recoveryPaymentId") @set:PropertyName("recoveryPaymentId")
    var recoveryPaymentId: String? = null
)
