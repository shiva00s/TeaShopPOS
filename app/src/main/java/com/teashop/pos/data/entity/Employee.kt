package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import java.io.Serializable

@Keep
data class Employee(
    @get:PropertyName("employeeId") @set:PropertyName("employeeId")
    var employeeId: String = "",
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    
    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String = "",
    
    @get:PropertyName("salaryType") @set:PropertyName("salaryType")
    var salaryType: String = "MONTHLY_FIXED",
    
    @get:PropertyName("salaryRate") @set:PropertyName("salaryRate")
    var salaryRate: Double = 0.0,
    
    @get:PropertyName("shiftStart") @set:PropertyName("shiftStart")
    var shiftStart: String = "10:00",
    
    @get:PropertyName("shiftEnd") @set:PropertyName("shiftEnd")
    var shiftEnd: String = "22:00",
    
    @get:PropertyName("breakHours") @set:PropertyName("breakHours")
    var breakHours: Double = 0.0,
    
    @get:PropertyName("graceMinutes") @set:PropertyName("graceMinutes")
    var graceMinutes: Int = 5,
    
    @get:PropertyName("otRateMultiplier") @set:PropertyName("otRateMultiplier")
    var otRateMultiplier: Double = 1.0,
    
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    
    @get:PropertyName("hireDate") @set:PropertyName("hireDate")
    var hireDate: Long = System.currentTimeMillis(),
    
    @get:PropertyName("terminateDate") @set:PropertyName("terminateDate")
    var terminateDate: Long? = null,
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),

    @get:Exclude @set:Exclude
    var lastActive: Long? = null
) : Serializable
