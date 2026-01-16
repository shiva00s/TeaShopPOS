package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import java.io.Serializable
import java.util.UUID

@Keep
data class EmployeeHistory(
    @get:PropertyName("historyId") @set:PropertyName("historyId")
    var historyId: String = UUID.randomUUID().toString(),
    
    @get:PropertyName("employeeId") @set:PropertyName("employeeId")
    var employeeId: String = "",
    
    @get:PropertyName("version") @set:PropertyName("version")
    var version: Int = 0,
    
    @get:PropertyName("salaryType") @set:PropertyName("salaryType")
    var salaryType: String = "",
    
    @get:PropertyName("salaryRate") @set:PropertyName("salaryRate")
    var salaryRate: Double = 0.0,
    
    @get:PropertyName("shiftStart") @set:PropertyName("shiftStart")
    var shiftStart: String = "",
    
    @get:PropertyName("shiftEnd") @set:PropertyName("shiftEnd")
    var shiftEnd: String = "",
    
    @get:PropertyName("breakHours") @set:PropertyName("breakHours")
    var breakHours: Double = 0.0,
    
    @get:PropertyName("changeDate") @set:PropertyName("changeDate")
    var changeDate: Long = System.currentTimeMillis(),
    
    @get:PropertyName("changeReason") @set:PropertyName("changeReason")
    var changeReason: String? = null
) : Serializable
