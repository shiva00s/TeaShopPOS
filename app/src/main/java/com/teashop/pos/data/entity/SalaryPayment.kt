package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class SalaryPayment(
    @get:PropertyName("paymentId") @set:PropertyName("paymentId")
    var paymentId: String = "",
    
    @get:PropertyName("employeeId") @set:PropertyName("employeeId")
    var employeeId: String = "",
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("totalEarnings") @set:PropertyName("totalEarnings")
    var totalEarnings: Double = 0.0,
    
    @get:PropertyName("deductions") @set:PropertyName("deductions")
    var deductions: Double = 0.0,
    
    @get:PropertyName("netPaid") @set:PropertyName("netPaid")
    var netPaid: Double = 0.0,
    
    @get:PropertyName("periodStart") @set:PropertyName("periodStart")
    var periodStart: Long = 0,
    
    @get:PropertyName("periodEnd") @set:PropertyName("periodEnd")
    var periodEnd: Long = 0,
    
    @get:PropertyName("paymentDate") @set:PropertyName("paymentDate")
    var paymentDate: Long = System.currentTimeMillis()
)
